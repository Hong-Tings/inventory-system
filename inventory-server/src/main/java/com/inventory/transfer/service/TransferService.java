package com.inventory.transfer.service;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inventory.common.constant.OrderStatus;
import com.inventory.common.exception.BusinessException;
import com.inventory.inventory.entity.Inventory;
import com.inventory.inventory.entity.InventoryLog;
import com.inventory.inventory.mapper.InventoryLogMapper;
import com.inventory.inventory.mapper.InventoryMapper;
import com.inventory.product.entity.Product;
import com.inventory.product.mapper.ProductMapper;
import com.inventory.system.entity.SysUser;
import com.inventory.system.mapper.SysUserMapper;
import com.inventory.transfer.entity.InventoryTransfer;
import com.inventory.transfer.entity.InventoryTransferItem;
import com.inventory.transfer.mapper.InventoryTransferItemMapper;
import com.inventory.transfer.mapper.InventoryTransferMapper;
import com.inventory.warehouse.entity.Warehouse;
import com.inventory.warehouse.mapper.WarehouseMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransferService {

    private final InventoryTransferMapper transferMapper;
    private final InventoryTransferItemMapper transferItemMapper;
    private final InventoryMapper inventoryMapper;
    private final InventoryLogMapper inventoryLogMapper;
    private final WarehouseMapper warehouseMapper;
    private final SysUserMapper userMapper;
    private final ProductMapper productMapper;

    public TransferService(InventoryTransferMapper transferMapper,
                           InventoryTransferItemMapper transferItemMapper,
                           InventoryMapper inventoryMapper,
                           InventoryLogMapper inventoryLogMapper,
                           WarehouseMapper warehouseMapper,
                           SysUserMapper userMapper,
                           ProductMapper productMapper) {
        this.transferMapper = transferMapper;
        this.transferItemMapper = transferItemMapper;
        this.inventoryMapper = inventoryMapper;
        this.inventoryLogMapper = inventoryLogMapper;
        this.warehouseMapper = warehouseMapper;
        this.userMapper = userMapper;
        this.productMapper = productMapper;
    }

    public Page<InventoryTransfer> page(Page<InventoryTransfer> page, String orderNo,
                                        Long fromWarehouseId, Long toWarehouseId, Integer status,
                                        Integer minQuantity, Integer maxQuantity, String operatorName,
                                        LocalDate startDate, LocalDate endDate) {
        // MyBatis-Plus 的条件参数会先求值，需要先判空
        LocalDateTime startDateTime = startDate != null ? startDate.atStartOfDay() : null;
        LocalDateTime endDateTime = endDate != null ? endDate.atTime(LocalTime.MAX) : null;
        LambdaQueryWrapper<InventoryTransfer> wrapper = new LambdaQueryWrapper<InventoryTransfer>()
                .like(orderNo != null && !orderNo.isEmpty(), InventoryTransfer::getOrderNo, orderNo)
                .eq(fromWarehouseId != null, InventoryTransfer::getFromWarehouseId, fromWarehouseId)
                .eq(toWarehouseId != null, InventoryTransfer::getToWarehouseId, toWarehouseId)
                .eq(status != null, InventoryTransfer::getStatus, status)
                .ge(minQuantity != null, InventoryTransfer::getTotalQuantity, minQuantity)
                .le(maxQuantity != null, InventoryTransfer::getTotalQuantity, maxQuantity)
                .ge(startDateTime != null, InventoryTransfer::getCreateTime, startDateTime)
                .le(endDateTime != null, InventoryTransfer::getCreateTime, endDateTime)
                .ne(InventoryTransfer::getStatus, OrderStatus.VOIDED)
                .orderByDesc(InventoryTransfer::getId);
        Page<InventoryTransfer> result = transferMapper.selectPage(page, wrapper);

        // 先填充关联数据（操作人名称等）
        for (InventoryTransfer t : result.getRecords()) {
            enrichOrder(t);
        }

        // 如果传了操作人名称，在内存中过滤
        if (operatorName != null && !operatorName.isEmpty()) {
            List<InventoryTransfer> filtered = result.getRecords().stream()
                    .filter(t -> t.getOperatorName() != null && t.getOperatorName().contains(operatorName))
                    .collect(Collectors.toList());
            result.setRecords(filtered);
            result.setTotal(filtered.size());
        }

        return result;
    }

    public List<InventoryTransfer> listAll() {
        List<InventoryTransfer> list = transferMapper.selectList(
                new LambdaQueryWrapper<InventoryTransfer>().orderByDesc(InventoryTransfer::getId));
        for (InventoryTransfer t : list) enrichOrder(t);
        return list;
    }

    public InventoryTransfer getDetail(Long id) {
        InventoryTransfer transfer = transferMapper.selectById(id);
        if (transfer != null) {
            List<InventoryTransferItem> items = transferItemMapper.selectList(
                    new LambdaQueryWrapper<InventoryTransferItem>()
                            .eq(InventoryTransferItem::getTransferId, id));
            enrichItems(items);
            transfer.setItems(items);
            enrichOrder(transfer);
        }
        return transfer;
    }

    private void enrichOrder(InventoryTransfer t) {
        if (t.getFromWarehouseId() != null) {
            Warehouse w = warehouseMapper.selectById(t.getFromWarehouseId());
            if (w != null) t.setFromWarehouseName(w.getName());
        }
        if (t.getToWarehouseId() != null) {
            Warehouse w = warehouseMapper.selectById(t.getToWarehouseId());
            if (w != null) t.setToWarehouseName(w.getName());
        }
        if (t.getOperatorId() != null) {
            SysUser u = userMapper.selectById(t.getOperatorId());
            if (u != null) t.setOperatorName(u.getRealName());
        }
    }

    private void enrichItems(List<InventoryTransferItem> items) {
        for (InventoryTransferItem item : items) {
            if (item.getProductId() != null) {
                Product p = productMapper.selectById(item.getProductId());
                if (p != null) {
                    item.setProductName(p.getName());
                    item.setProductCode(p.getCode());
                }
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(InventoryTransfer transfer) {
        transfer.setOrderNo(generateOrderNo());
        transfer.setStatus(OrderStatus.DRAFT);
        if (transfer.getOrderDate() == null) {
            transfer.setOrderDate(LocalDate.now());
        }

        int totalQty = 0;
        List<InventoryTransferItem> items = transfer.getItems();
        if (items != null) {
            for (InventoryTransferItem item : items) {
                if (item.getProductId() == null) {
                    throw new BusinessException("商品ID不能为空");
                }
                item.setTransferId(null);
                totalQty += item.getQuantity();
            }
        }
        transfer.setTotalQuantity(totalQty);

        transferMapper.insert(transfer);

        if (items != null) {
            for (InventoryTransferItem item : items) {
                item.setTransferId(transfer.getId());
                transferItemMapper.insert(item);
            }
        }
        return transfer.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateDraft(InventoryTransfer transfer) {
        InventoryTransfer existing = transferMapper.selectById(transfer.getId());
        if (existing == null) throw new BusinessException("调拨单不存在");
        if (existing.getStatus() != OrderStatus.DRAFT) throw new BusinessException("仅草稿状态可编辑");

        // 删除旧明细
        transferItemMapper.delete(new LambdaQueryWrapper<InventoryTransferItem>()
                .eq(InventoryTransferItem::getTransferId, transfer.getId()));

        // 重新计算总数量
        int totalQty = 0;
        List<InventoryTransferItem> items = transfer.getItems();
        if (items != null) {
            for (InventoryTransferItem item : items) {
                item.setId(null);
                item.setTransferId(transfer.getId());
                if (item.getProductId() == null) {
                    throw new BusinessException("商品ID不能为空");
                }
                totalQty += item.getQuantity();
                transferItemMapper.insert(item);
            }
        }
        transfer.setTotalQuantity(totalQty);
        // 保留仓库信息（fromWarehouseId/toWarehouseId 应由控制器设好）
        if (transfer.getFromWarehouseId() == null) transfer.setFromWarehouseId(existing.getFromWarehouseId());
        if (transfer.getToWarehouseId() == null) transfer.setToWarehouseId(existing.getToWarehouseId());
        transferMapper.updateById(transfer);
    }

    @Transactional(rollbackFor = Exception.class)
    public synchronized void submit(Long id) {
        InventoryTransfer transfer = transferMapper.selectById(id);
        if (transfer == null) throw new BusinessException("调拨单不存在");
        if (transfer.getStatus() != OrderStatus.DRAFT) throw new BusinessException("当前状态不可提交");

        List<InventoryTransferItem> items = transferItemMapper.selectList(
                new LambdaQueryWrapper<InventoryTransferItem>().eq(InventoryTransferItem::getTransferId, id));

        // 扣减源仓库库存
        for (InventoryTransferItem item : items) {
            LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<Inventory>()
                    .eq(Inventory::getProductId, item.getProductId())
                    .eq(Inventory::getWarehouseId, transfer.getFromWarehouseId())
                    .eq(item.getBatchNo() != null, Inventory::getBatchNo, item.getBatchNo());
            Inventory inv = inventoryMapper.selectOne(wrapper);
            int available = (inv != null ? inv.getQuantity() : 0) - (inv != null ? inv.getLockedQty() : 0);
            if (available < item.getQuantity()) {
                throw new BusinessException("商品库存不足");
            }

            int beforeQty = inv.getQuantity();
            int afterQty = beforeQty - item.getQuantity();
            inv.setQuantity(afterQty);
            inventoryMapper.updateById(inv);

            // 记录调出日志
            InventoryLog outLog = new InventoryLog();
            outLog.setProductId(item.getProductId());
            outLog.setWarehouseId(transfer.getFromWarehouseId());
            outLog.setBatchNo(item.getBatchNo());
            outLog.setChangeType("TRANSFER_OUT");
            outLog.setChangeQty(-item.getQuantity());
            outLog.setBeforeQty(beforeQty);
            outLog.setAfterQty(afterQty);
            outLog.setRefOrderNo(transfer.getOrderNo());
            outLog.setOperatorId(transfer.getOperatorId());
            outLog.setRemark("调拨出库");
            inventoryLogMapper.insert(outLog);
        }

        // 增加目标仓库库存
        for (InventoryTransferItem item : items) {
            // 获取调出仓库该商品的成本价（同一商品同一仓库下所有批次成本价一致）
            Inventory srcInv = inventoryMapper.selectOne(
                    new LambdaQueryWrapper<Inventory>()
                            .eq(Inventory::getProductId, item.getProductId())
                            .eq(Inventory::getWarehouseId, transfer.getFromWarehouseId())
                            .last("LIMIT 1"));
            BigDecimal srcCost = srcInv != null && srcInv.getCostPrice() != null
                    ? srcInv.getCostPrice() : BigDecimal.ZERO;

            // 计算目标仓库调整前的总金额和总数量
            List<Inventory> destBatches = inventoryMapper.selectList(
                    new LambdaQueryWrapper<Inventory>()
                            .eq(Inventory::getProductId, item.getProductId())
                            .eq(Inventory::getWarehouseId, transfer.getToWarehouseId()));
            BigDecimal destOldValue = BigDecimal.ZERO;
            int destOldQty = 0;
            for (Inventory b : destBatches) {
                if (b.getCostPrice() != null) {
                    destOldValue = destOldValue.add(
                            b.getCostPrice().multiply(BigDecimal.valueOf(b.getQuantity())));
                }
                destOldQty += b.getQuantity();
            }

            LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<Inventory>()
                    .eq(Inventory::getProductId, item.getProductId())
                    .eq(Inventory::getWarehouseId, transfer.getToWarehouseId())
                    .eq(item.getBatchNo() != null, Inventory::getBatchNo, item.getBatchNo());
            Inventory inv = inventoryMapper.selectOne(wrapper);
            int beforeQty = inv != null ? inv.getQuantity() : 0;
            int afterQty = beforeQty + item.getQuantity();

            if (inv != null) {
                inv.setQuantity(afterQty);
                inventoryMapper.updateById(inv);
            } else {
                inv = new Inventory();
                inv.setProductId(item.getProductId());
                inv.setWarehouseId(transfer.getToWarehouseId());
                inv.setBatchNo(item.getBatchNo());
                inv.setQuantity(item.getQuantity());
                inv.setLockedQty(0);
                inventoryMapper.insert(inv);
            }

            // 移动加权平均：新均价 = (目标仓库原总金额 + 调入数量×源仓库成本价) ÷ (原数量 + 调入数量)
            int destNewQty = destOldQty + item.getQuantity();
            BigDecimal transferValue = srcCost.multiply(BigDecimal.valueOf(item.getQuantity()));
            BigDecimal destNewValue = destOldValue.add(transferValue);
            BigDecimal destAvg = BigDecimal.ZERO;
            if (destNewQty > 0) {
                destAvg = destNewValue.divide(BigDecimal.valueOf(destNewQty), 4, RoundingMode.HALF_UP);
            }
            // 统一设置目标仓库该商品所有批次的成本价
            List<Inventory> allDestBatches = inventoryMapper.selectList(
                    new LambdaQueryWrapper<Inventory>()
                            .eq(Inventory::getProductId, item.getProductId())
                            .eq(Inventory::getWarehouseId, transfer.getToWarehouseId()));
            for (Inventory b : allDestBatches) {
                b.setCostPrice(destAvg);
                inventoryMapper.updateById(b);
            }

            // 记录调入日志
            InventoryLog inLog = new InventoryLog();
            inLog.setProductId(item.getProductId());
            inLog.setWarehouseId(transfer.getToWarehouseId());
            inLog.setBatchNo(item.getBatchNo());
            inLog.setChangeType("TRANSFER_IN");
            inLog.setChangeQty(item.getQuantity());
            inLog.setBeforeQty(beforeQty);
            inLog.setAfterQty(afterQty);
            inLog.setRefOrderNo(transfer.getOrderNo());
            inLog.setOperatorId(transfer.getOperatorId());
            inLog.setRemark("调拨入库");
            inventoryLogMapper.insert(inLog);
        }

        transfer.setStatus(OrderStatus.CONFIRMED);
        transferMapper.updateById(transfer);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        InventoryTransfer transfer = transferMapper.selectById(id);
        if (transfer == null) throw new BusinessException("调拨单不存在");
        transfer.setStatus(OrderStatus.VOIDED);
        transferMapper.updateById(transfer);
    }

    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Long> ids) {
        for (Long id : ids) {
            InventoryTransfer transfer = transferMapper.selectById(id);
            if (transfer != null) {
                transfer.setStatus(OrderStatus.VOIDED);
                transferMapper.updateById(transfer);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void voidOrder(Long id, String reason) {
        InventoryTransfer transfer = transferMapper.selectById(id);
        if (transfer == null) throw new BusinessException("调拨单不存在");
        if (transfer.getStatus() == OrderStatus.CONFIRMED) throw new BusinessException("已确认的单据不可作废");
        transfer.setStatus(OrderStatus.VOIDED);
        transfer.setRemark((transfer.getRemark() != null ? transfer.getRemark() + " | " : "") + "作废原因: " + (reason != null ? reason : ""));
        transfer.setUpdateTime(LocalDateTime.now());
        transferMapper.updateById(transfer);
    }

    @Transactional(rollbackFor = Exception.class)
    public void batchVoid(List<Long> ids, String reason) {
        for (Long id : ids) {
            voidOrder(id, reason);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public synchronized void cancel(Long id) {
        InventoryTransfer transfer = transferMapper.selectById(id);
        if (transfer == null) throw new BusinessException("调拨单不存在");
        if (transfer.getStatus() == OrderStatus.CANCELED) throw new BusinessException("调拨单已取消");

        // 如果已确认，需要回滚库存
        if (transfer.getStatus() == OrderStatus.CONFIRMED) {
            List<InventoryTransferItem> items = transferItemMapper.selectList(
                    new LambdaQueryWrapper<InventoryTransferItem>().eq(InventoryTransferItem::getTransferId, id));

            // 回滚目标仓库：扣减调入的库存
            for (InventoryTransferItem item : items) {
                LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getProductId, item.getProductId())
                        .eq(Inventory::getWarehouseId, transfer.getToWarehouseId())
                        .eq(item.getBatchNo() != null, Inventory::getBatchNo, item.getBatchNo());
                Inventory inv = inventoryMapper.selectOne(wrapper);
                if (inv != null) {
                    int beforeQty = inv.getQuantity();
                    int afterQty = beforeQty - item.getQuantity();
                    inv.setQuantity(afterQty);
                    inventoryMapper.updateById(inv);

                    InventoryLog log = new InventoryLog();
                    log.setProductId(item.getProductId());
                    log.setWarehouseId(transfer.getToWarehouseId());
                    log.setBatchNo(item.getBatchNo());
                    log.setChangeType("TRANSFER_CANCEL");
                    log.setChangeQty(-item.getQuantity());
                    log.setBeforeQty(beforeQty);
                    log.setAfterQty(afterQty);
                    log.setRefOrderNo(transfer.getOrderNo());
                    log.setOperatorId(transfer.getOperatorId());
                    log.setRemark("调拨取消，回滚入库库存");
                    inventoryLogMapper.insert(log);
                }
            }

            // 回滚源仓库：恢复调出的库存
            for (InventoryTransferItem item : items) {
                LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getProductId, item.getProductId())
                        .eq(Inventory::getWarehouseId, transfer.getFromWarehouseId())
                        .eq(item.getBatchNo() != null, Inventory::getBatchNo, item.getBatchNo());
                Inventory inv = inventoryMapper.selectOne(wrapper);
                if (inv != null) {
                    int beforeQty = inv.getQuantity();
                    int afterQty = beforeQty + item.getQuantity();
                    inv.setQuantity(afterQty);
                    inventoryMapper.updateById(inv);

                    InventoryLog log = new InventoryLog();
                    log.setProductId(item.getProductId());
                    log.setWarehouseId(transfer.getFromWarehouseId());
                    log.setBatchNo(item.getBatchNo());
                    log.setChangeType("TRANSFER_CANCEL");
                    log.setChangeQty(item.getQuantity());
                    log.setBeforeQty(beforeQty);
                    log.setAfterQty(afterQty);
                    log.setRefOrderNo(transfer.getOrderNo());
                    log.setOperatorId(transfer.getOperatorId());
                    log.setRemark("调拨取消，回滚出库库存");
                    inventoryLogMapper.insert(log);
                }
            }
        }

        transfer.setStatus(OrderStatus.CANCELED);
        transferMapper.updateById(transfer);
    }

    private synchronized String generateOrderNo() {
        String prefix = "DB";
        String dateStr = DateUtil.format(new Date(), "yyyyMMdd");
        String likePrefix = prefix + dateStr;
        LambdaQueryWrapper<InventoryTransfer> wrapper = new LambdaQueryWrapper<InventoryTransfer>()
                .likeRight(InventoryTransfer::getOrderNo, likePrefix)
                .orderByDesc(InventoryTransfer::getOrderNo);
        Page<InventoryTransfer> page = transferMapper.selectPage(new Page<>(1, 1), wrapper);
        int seq = 1;
        if (!page.getRecords().isEmpty()) {
            String lastNo = page.getRecords().get(0).getOrderNo();
            seq = Integer.parseInt(lastNo.substring(lastNo.length() - 4)) + 1;
        }
        String orderNo = prefix + dateStr + String.format("%04d", seq);
        while (transferMapper.selectCount(new LambdaQueryWrapper<InventoryTransfer>().eq(InventoryTransfer::getOrderNo, orderNo)) > 0) {
            seq++;
            orderNo = prefix + dateStr + String.format("%04d", seq);
        }
        return orderNo;
    }
}
