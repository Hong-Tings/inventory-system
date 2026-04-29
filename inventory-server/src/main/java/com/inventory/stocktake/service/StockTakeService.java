package com.inventory.stocktake.service;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inventory.common.constant.OrderStatus;
import com.inventory.common.exception.BusinessException;
import com.inventory.inventory.entity.Inventory;
import com.inventory.inventory.entity.InventoryLog;
import com.inventory.inventory.mapper.InventoryLogMapper;
import com.inventory.inventory.mapper.InventoryMapper;
import com.inventory.stocktake.entity.StockTake;
import com.inventory.stocktake.entity.StockTakeItem;
import com.inventory.stocktake.mapper.StockTakeItemMapper;
import com.inventory.stocktake.mapper.StockTakeMapper;
import com.inventory.system.entity.SysUser;
import com.inventory.system.mapper.SysUserMapper;
import com.inventory.product.entity.Product;
import com.inventory.product.mapper.ProductMapper;
import com.inventory.warehouse.entity.Warehouse;
import com.inventory.warehouse.mapper.WarehouseMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StockTakeService {

    private final StockTakeMapper stockTakeMapper;
    private final StockTakeItemMapper stockTakeItemMapper;
    private final InventoryMapper inventoryMapper;
    private final InventoryLogMapper inventoryLogMapper;
    private final WarehouseMapper warehouseMapper;
    private final SysUserMapper sysUserMapper;
    private final ProductMapper productMapper;

    public StockTakeService(StockTakeMapper stockTakeMapper,
                            StockTakeItemMapper stockTakeItemMapper,
                            InventoryMapper inventoryMapper,
                            InventoryLogMapper inventoryLogMapper,
                            WarehouseMapper warehouseMapper,
                            SysUserMapper sysUserMapper,
                            ProductMapper productMapper) {
        this.stockTakeMapper = stockTakeMapper;
        this.stockTakeItemMapper = stockTakeItemMapper;
        this.inventoryMapper = inventoryMapper;
        this.inventoryLogMapper = inventoryLogMapper;
        this.warehouseMapper = warehouseMapper;
        this.sysUserMapper = sysUserMapper;
        this.productMapper = productMapper;
    }

    public Page<StockTake> page(Page<StockTake> page, String orderNo, Long warehouseId, Integer takeType,
                                Integer minTotalItems, Integer maxTotalItems, Integer minDiffItems, Integer maxDiffItems,
                                String operatorName, String approverName, Integer status) {
        LambdaQueryWrapper<StockTake> wrapper = new LambdaQueryWrapper<StockTake>()
                .like(orderNo != null, StockTake::getOrderNo, orderNo)
                .eq(warehouseId != null, StockTake::getWarehouseId, warehouseId)
                .eq(takeType != null, StockTake::getTakeType, takeType)
                .ge(minTotalItems != null, StockTake::getTotalItems, minTotalItems)
                .le(maxTotalItems != null, StockTake::getTotalItems, maxTotalItems)
                .ge(minDiffItems != null, StockTake::getDiffItems, minDiffItems)
                .le(maxDiffItems != null, StockTake::getDiffItems, maxDiffItems)
                .eq(status != null, StockTake::getStatus, status)
                .ne(StockTake::getStatus, OrderStatus.VOIDED)
                .orderByDesc(StockTake::getId);
        Page<StockTake> result = stockTakeMapper.selectPage(page, wrapper);

        // Enrich warehouse name, operator name, approver name
        for (StockTake t : result.getRecords()) {
            enrichStockTake(t);
        }

        // Filter by operator name in memory
        if (operatorName != null && !operatorName.isEmpty()) {
            List<StockTake> filtered = result.getRecords().stream()
                    .filter(t -> t.getOperatorName() != null && t.getOperatorName().contains(operatorName))
                    .collect(Collectors.toList());
            result.setRecords(filtered);
            result.setTotal(filtered.size());
        }

        // Filter by approver name in memory
        if (approverName != null && !approverName.isEmpty()) {
            List<StockTake> filtered = result.getRecords().stream()
                    .filter(t -> t.getApproverName() != null && t.getApproverName().contains(approverName))
                    .collect(Collectors.toList());
            result.setRecords(filtered);
            result.setTotal(filtered.size());
        }

        return result;
    }

    private void enrichStockTake(StockTake t) {
        if (t.getWarehouseId() != null) {
            Warehouse w = warehouseMapper.selectById(t.getWarehouseId());
            if (w != null) t.setWarehouseName(w.getName());
        }
        if (t.getOperatorId() != null) {
            SysUser u = sysUserMapper.selectById(t.getOperatorId());
            if (u != null) t.setOperatorName(u.getRealName());
        }
        if (t.getApproverId() != null) {
            SysUser u = sysUserMapper.selectById(t.getApproverId());
            if (u != null) t.setApproverName(u.getRealName());
        }
    }

    public List<StockTake> listAll() {
        List<StockTake> list = stockTakeMapper.selectList(new LambdaQueryWrapper<StockTake>().orderByDesc(StockTake::getId));
        for (StockTake t : list) enrichStockTake(t);
        return list;
    }

    public StockTake getDetail(Long id) {
        StockTake stockTake = stockTakeMapper.selectById(id);
        if (stockTake != null) {
            List<StockTakeItem> items = stockTakeItemMapper.selectList(
                    new LambdaQueryWrapper<StockTakeItem>()
                            .eq(StockTakeItem::getStockTakeId, id));
            // 填充商品名称和编码
            for (StockTakeItem item : items) {
                if (item.getProductId() != null) {
                    Product p = productMapper.selectById(item.getProductId());
                    if (p != null) {
                        item.setProductName(p.getName());
                        item.setProductCode(p.getCode());
                    }
                }
            }
            stockTake.setItems(items);
            enrichStockTake(stockTake);
        }
        return stockTake;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(StockTake stockTake) {
        stockTake.setOrderNo(generateOrderNo());
        stockTake.setStatus(OrderStatus.STOCKTAKE_IN_PROGRESS);
        if (stockTake.getOrderDate() == null) {
            stockTake.setOrderDate(LocalDate.now());
        }
        stockTake.setTotalItems(0);
        stockTake.setDiffItems(0);
        stockTakeMapper.insert(stockTake);

        // 全盘：自动加载该仓库所有商品作为盘点项
        if (stockTake.getTakeType() != null && stockTake.getTakeType() == 0) {
            List<Inventory> invList = inventoryMapper.selectList(
                    new LambdaQueryWrapper<Inventory>()
                            .eq(Inventory::getWarehouseId, stockTake.getWarehouseId())
                            .gt(Inventory::getQuantity, 0));
            for (Inventory inv : invList) {
                StockTakeItem item = new StockTakeItem();
                item.setStockTakeId(stockTake.getId());
                item.setProductId(inv.getProductId());
                item.setBatchNo(inv.getBatchNo());
                item.setBookQty(inv.getQuantity());
                item.setActualQty(inv.getQuantity());
                item.setDiffQty(0);
                stockTakeItemMapper.insert(item);
            }
            stockTake.setTotalItems(invList.size());
            stockTake.setDiffItems(0);
            stockTakeMapper.updateById(stockTake);
        }

        return stockTake.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void addItem(StockTakeItem item) {
        // Verify stock take exists and is in progress
        StockTake stockTake = stockTakeMapper.selectById(item.getStockTakeId());
        if (stockTake == null) {
            throw new BusinessException("盘点单不存在");
        }
        if (stockTake.getStatus() != OrderStatus.STOCKTAKE_IN_PROGRESS) {
            throw new BusinessException("当前状态不可添加盘点项");
        }

        // Get current book quantity from inventory in the same warehouse
        List<Inventory> invList = inventoryMapper.selectList(
                new LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getProductId, item.getProductId())
                        .eq(Inventory::getWarehouseId, stockTake.getWarehouseId()));
        int bookQty = invList.stream().mapToInt(Inventory::getQuantity).sum();
        item.setBookQty(bookQty);
        int actualQty = item.getActualQty() != null ? item.getActualQty() : bookQty;
        item.setActualQty(actualQty);
        item.setDiffQty(actualQty - bookQty);

        stockTakeItemMapper.insert(item);

        // Update stock take totals
        stockTake.setTotalItems(stockTake.getTotalItems() + 1);
        if (item.getDiffQty() != 0) {
            stockTake.setDiffItems(stockTake.getDiffItems() + 1);
        }
        stockTakeMapper.updateById(stockTake);
    }

    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id, Long approverId) {
        StockTake stockTake = stockTakeMapper.selectById(id);
        if (stockTake == null) {
            throw new BusinessException("盘点单不存在");
        }
        if (stockTake.getStatus() != OrderStatus.STOCKTAKE_IN_PROGRESS) {
            throw new BusinessException("当前状态不可审核");
        }
        stockTake.setStatus(OrderStatus.STOCKTAKE_APPROVED);
        stockTake.setApproverId(approverId);
        stockTakeMapper.updateById(stockTake);
    }

    @Transactional(rollbackFor = Exception.class)
    public void adjust(Long id) {
        StockTake stockTake = stockTakeMapper.selectById(id);
        if (stockTake == null) {
            throw new BusinessException("盘点单不存在");
        }
        if (stockTake.getStatus() != OrderStatus.STOCKTAKE_APPROVED) {
            throw new BusinessException("当前状态不可调整，请先审核");
        }

        List<StockTakeItem> items = stockTakeItemMapper.selectList(
                new LambdaQueryWrapper<StockTakeItem>()
                        .eq(StockTakeItem::getStockTakeId, id));

        for (StockTakeItem item : items) {
            if (item.getDiffQty() == 0) {
                continue;
            }

            // 查询该商品在对应仓库中的所有库存记录，按批次汇总
            List<Inventory> invList = inventoryMapper.selectList(
                    new LambdaQueryWrapper<Inventory>()
                            .eq(Inventory::getProductId, item.getProductId())
                            .eq(Inventory::getWarehouseId, stockTake.getWarehouseId()));

            int totalQty = invList.stream().mapToInt(Inventory::getQuantity).sum();
            int diff = item.getActualQty() - totalQty;

            if (diff != 0) {
                InventoryLog log = new InventoryLog();
                log.setProductId(item.getProductId());
                log.setWarehouseId(stockTake.getWarehouseId());
                log.setChangeType(OrderStatus.STOCKTAKE_ADJUST);
                log.setChangeQty(diff);
                log.setBeforeQty(totalQty);
                log.setAfterQty(item.getActualQty());
                log.setRefOrderNo(stockTake.getOrderNo());
                log.setOperatorId(stockTake.getOperatorId());
                log.setRemark(item.getDiffReason() != null ? item.getDiffReason() : "盘点调整");
                inventoryLogMapper.insert(log);
            }

            // 更新库存：直接设第一个库存记录的数量为目标值
            if (!invList.isEmpty()) {
                invList.get(0).setQuantity(item.getActualQty());
                inventoryMapper.updateById(invList.get(0));
                // 多余批次清零
                for (int i = 1; i < invList.size(); i++) {
                    invList.get(i).setQuantity(0);
                    inventoryMapper.updateById(invList.get(i));
                }
            } else {
                Inventory inv = new Inventory();
                inv.setProductId(item.getProductId());
                inv.setWarehouseId(stockTake.getWarehouseId());
                inv.setQuantity(item.getActualQty());
                inv.setLockedQty(0);
                inventoryMapper.insert(inv);
            }
        }

        stockTake.setStatus(OrderStatus.STOCKTAKE_ADJUSTED);
        stockTakeMapper.updateById(stockTake);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateItem(StockTakeItem item) {
        StockTakeItem existing = stockTakeItemMapper.selectById(item.getId());
        if (existing == null) throw new BusinessException("盘点明细不存在");

        // 只更新实盘数和差异原因
        if (item.getActualQty() != null) {
            existing.setActualQty(item.getActualQty());
            existing.setDiffQty(item.getActualQty() - existing.getBookQty());
        }
        if (item.getDiffReason() != null) {
            existing.setDiffReason(item.getDiffReason());
        }
        stockTakeItemMapper.updateById(existing);

        // 重新统计差异数
        StockTake stockTake = stockTakeMapper.selectById(existing.getStockTakeId());
        if (stockTake != null) {
            List<StockTakeItem> allItems = stockTakeItemMapper.selectList(
                    new LambdaQueryWrapper<StockTakeItem>().eq(StockTakeItem::getStockTakeId, stockTake.getId()));
            long diffCount = allItems.stream().filter(i -> i.getDiffQty() != 0).count();
            stockTake.setDiffItems((int) diffCount);
            stockTakeMapper.updateById(stockTake);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteItem(Long itemId) {
        StockTakeItem item = stockTakeItemMapper.selectById(itemId);
        if (item == null) throw new BusinessException("盘点明细不存在");
        StockTake stockTake = stockTakeMapper.selectById(item.getStockTakeId());
        stockTakeItemMapper.deleteById(itemId);
        if (stockTake != null) {
            stockTake.setTotalItems(Math.max(0, stockTake.getTotalItems() - 1));
            if (item.getDiffQty() != 0) {
                stockTake.setDiffItems(Math.max(0, stockTake.getDiffItems() - 1));
            }
            stockTakeMapper.updateById(stockTake);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        StockTake order = stockTakeMapper.selectById(id);
        if (order == null) throw new BusinessException("盘点单不存在");
        order.setStatus(OrderStatus.VOIDED);
        stockTakeMapper.updateById(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Long> ids) {
        for (Long id : ids) {
            StockTake order = stockTakeMapper.selectById(id);
            if (order != null) {
                order.setStatus(OrderStatus.VOIDED);
                stockTakeMapper.updateById(order);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void voidOrder(Long id, String reason) {
        StockTake order = stockTakeMapper.selectById(id);
        if (order == null) throw new BusinessException("盘点单不存在");
        if (order.getStatus() == OrderStatus.STOCKTAKE_ADJUSTED) throw new BusinessException("已调整的盘点单不可作废");
        order.setStatus(OrderStatus.VOIDED);
        order.setRemark((order.getRemark() != null ? order.getRemark() + " | " : "") + "作废原因: " + (reason != null ? reason : ""));
        order.setUpdateTime(LocalDateTime.now());
        stockTakeMapper.updateById(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public void batchVoid(List<Long> ids, String reason) {
        for (Long id : ids) {
            voidOrder(id, reason);
        }
    }

    private synchronized String generateOrderNo() {
        String prefix = "ST";
        String dateStr = DateUtil.format(new Date(), "yyyyMMdd");
        String likePrefix = prefix + dateStr;

        LambdaQueryWrapper<StockTake> wrapper = new LambdaQueryWrapper<StockTake>()
                .likeRight(StockTake::getOrderNo, likePrefix)
                .orderByDesc(StockTake::getOrderNo);
        Page<StockTake> page = stockTakeMapper.selectPage(new Page<>(1, 1), wrapper);

        int seq = 1;
        if (!page.getRecords().isEmpty()) {
            String lastNo = page.getRecords().get(0).getOrderNo();
            seq = Integer.parseInt(lastNo.substring(lastNo.length() - 4)) + 1;
        }
        // 防止重复：如果生成的单号已存在则自增
        String orderNo = likePrefix + String.format("%04d", seq);
        while (stockTakeMapper.selectCount(new LambdaQueryWrapper<StockTake>().eq(StockTake::getOrderNo, orderNo)) > 0) {
            seq++;
            orderNo = likePrefix + String.format("%04d", seq);
        }
        return orderNo;
    }
}
