package com.inventory.purchase.service;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inventory.common.constant.OrderStatus;
import com.inventory.common.exception.BusinessException;
import com.inventory.customer.mapper.CustomerMapper;
import com.inventory.customer.entity.Customer;
import com.inventory.inventory.entity.Inventory;
import com.inventory.inventory.entity.InventoryLog;
import com.inventory.inventory.mapper.InventoryLogMapper;
import com.inventory.inventory.mapper.InventoryMapper;
import com.inventory.product.entity.Product;
import com.inventory.product.mapper.ProductMapper;
import com.inventory.purchase.entity.PurchaseOrder;
import com.inventory.purchase.entity.PurchaseOrderItem;
import com.inventory.purchase.mapper.PurchaseOrderItemMapper;
import com.inventory.purchase.mapper.PurchaseOrderMapper;
import com.inventory.supplier.entity.Supplier;
import com.inventory.supplier.mapper.SupplierMapper;
import com.inventory.system.entity.SysUser;
import com.inventory.system.mapper.SysUserMapper;
import com.inventory.warehouse.entity.Warehouse;
import com.inventory.warehouse.mapper.WarehouseMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PurchaseOrderService {

    private final PurchaseOrderMapper purchaseOrderMapper;
    private final PurchaseOrderItemMapper purchaseOrderItemMapper;
    private final InventoryMapper inventoryMapper;
    private final InventoryLogMapper inventoryLogMapper;
    private final SupplierMapper supplierMapper;
    private final WarehouseMapper warehouseMapper;
    private final SysUserMapper userMapper;
    private final ProductMapper productMapper;

    public PurchaseOrderService(PurchaseOrderMapper purchaseOrderMapper,
                                PurchaseOrderItemMapper purchaseOrderItemMapper,
                                InventoryMapper inventoryMapper,
                                InventoryLogMapper inventoryLogMapper,
                                SupplierMapper supplierMapper,
                                WarehouseMapper warehouseMapper,
                                SysUserMapper userMapper,
                                ProductMapper productMapper) {
        this.purchaseOrderMapper = purchaseOrderMapper;
        this.purchaseOrderItemMapper = purchaseOrderItemMapper;
        this.inventoryMapper = inventoryMapper;
        this.inventoryLogMapper = inventoryLogMapper;
        this.supplierMapper = supplierMapper;
        this.warehouseMapper = warehouseMapper;
        this.userMapper = userMapper;
        this.productMapper = productMapper;
    }

    public Page<PurchaseOrder> page(Page<PurchaseOrder> page, String orderNo, Long supplierId, Long warehouseId,
                                    Integer minQuantity, Integer maxQuantity, Integer status,
                                    LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<PurchaseOrder> wrapper = new LambdaQueryWrapper<PurchaseOrder>()
                .like(orderNo != null, PurchaseOrder::getOrderNo, orderNo)
                .eq(supplierId != null, PurchaseOrder::getSupplierId, supplierId)
                .eq(warehouseId != null, PurchaseOrder::getWarehouseId, warehouseId)
                .ge(startDate != null, PurchaseOrder::getOrderDate, startDate)
                .le(endDate != null, PurchaseOrder::getOrderDate, endDate)
                .orderByDesc(PurchaseOrder::getId);

        // 默认排除已作废
        wrapper.ne(PurchaseOrder::getStatus, OrderStatus.VOIDED);
        // 数量和金额筛选需要单独处理
        if (minQuantity != null) wrapper.ge(PurchaseOrder::getTotalQuantity, minQuantity);
        if (maxQuantity != null) wrapper.le(PurchaseOrder::getTotalQuantity, maxQuantity);

        Page<PurchaseOrder> result = purchaseOrderMapper.selectPage(page, wrapper);
        for (PurchaseOrder o : result.getRecords()) {
            enrichOrder(o);
            // 加载 items 以便前端显示采购单价
            List<PurchaseOrderItem> items = purchaseOrderItemMapper.selectList(
                    new LambdaQueryWrapper<PurchaseOrderItem>().eq(PurchaseOrderItem::getOrderId, o.getId()));
            enrichItems(items);
            o.setItems(items);
        }
        return result;
    }

    public List<PurchaseOrder> listAll() {
        List<PurchaseOrder> list = purchaseOrderMapper.selectList(
                new LambdaQueryWrapper<PurchaseOrder>().orderByDesc(PurchaseOrder::getId));
        for (PurchaseOrder o : list) {
            enrichOrder(o);
        }
        return list;
    }

    public PurchaseOrder getDetail(Long id) {
        PurchaseOrder order = purchaseOrderMapper.selectById(id);
        if (order != null) {
            List<PurchaseOrderItem> items = purchaseOrderItemMapper.selectList(
                    new LambdaQueryWrapper<PurchaseOrderItem>()
                            .eq(PurchaseOrderItem::getOrderId, id));
            enrichItems(items);
            order.setItems(items);
            enrichOrder(order);
        }
        return order;
    }

    private void enrichOrder(PurchaseOrder o) {
        if (o.getSupplierId() != null) {
            Supplier s = supplierMapper.selectById(o.getSupplierId());
            if (s != null) o.setSupplierName(s.getName());
        }
        if (o.getWarehouseId() != null) {
            Warehouse w = warehouseMapper.selectById(o.getWarehouseId());
            if (w != null) o.setWarehouseName(w.getName());
        }
        if (o.getOperatorId() != null) {
            SysUser u = userMapper.selectById(o.getOperatorId());
            if (u != null) o.setOperatorName(u.getRealName());
        }
    }

    private void enrichItems(List<PurchaseOrderItem> items) {
        for (PurchaseOrderItem item : items) {
            if (item.getProductId() != null) {
                Product p = productMapper.selectById(item.getProductId());
                if (p != null) {
                    item.setProductName(p.getName());
                    item.setProductCode(p.getCode());
                }
            }
        }
    }

    private void enrichItems(Long orderId) {
        List<PurchaseOrderItem> items = purchaseOrderItemMapper.selectList(
                new LambdaQueryWrapper<PurchaseOrderItem>()
                        .eq(PurchaseOrderItem::getOrderId, orderId));
        if (items != null && !items.isEmpty()) {
            enrichItems(items);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public Long create(PurchaseOrder order) {
        order.setOrderNo(generateOrderNo());
        order.setStatus(OrderStatus.DRAFT);
        order.setOrderDate(order.getOrderDate() != null ? order.getOrderDate() : LocalDate.now());

        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalQty = 0;
        List<PurchaseOrderItem> items = order.getItems();
        if (items != null) {
            for (PurchaseOrderItem item : items) {
                if (item.getProductId() == null) {
                    throw new BusinessException("商品ID不能为空");
                }
                item.setOrderId(null);
                if (item.getUnitPrice() == null) item.setUnitPrice(BigDecimal.ZERO);
                BigDecimal amount = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                item.setAmount(amount);
                totalAmount = totalAmount.add(amount);
                totalQty += item.getQuantity();
            }
        }
        order.setTotalAmount(totalAmount);
        order.setTotalQuantity(totalQty);

        purchaseOrderMapper.insert(order);

        if (items != null) {
            for (PurchaseOrderItem item : items) {
                item.setOrderId(order.getId());
                purchaseOrderItemMapper.insert(item);
            }
        }
        return order.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public synchronized void submit(Long id) {
        PurchaseOrder order = purchaseOrderMapper.selectById(id);
        if (order == null) throw new BusinessException("采购单不存在");
        if (order.getStatus() != OrderStatus.DRAFT) throw new BusinessException("当前状态不可提交");

        List<PurchaseOrderItem> items = purchaseOrderItemMapper.selectList(
                new LambdaQueryWrapper<PurchaseOrderItem>().eq(PurchaseOrderItem::getOrderId, id));

        for (PurchaseOrderItem item : items) {
            // 移动加权平均法：先计算该商品该仓库当前的库存总金额和总数量
            List<Inventory> existingBatches = inventoryMapper.selectList(
                    new LambdaQueryWrapper<Inventory>()
                            .eq(Inventory::getProductId, item.getProductId())
                            .eq(Inventory::getWarehouseId, order.getWarehouseId()));
            BigDecimal oldTotalValue = BigDecimal.ZERO;
            int oldTotalQty = 0;
            for (Inventory b : existingBatches) {
                if (b.getCostPrice() != null) {
                    oldTotalValue = oldTotalValue.add(
                            b.getCostPrice().multiply(BigDecimal.valueOf(b.getQuantity())));
                }
                oldTotalQty += b.getQuantity();
            }

            LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<Inventory>()
                    .eq(Inventory::getProductId, item.getProductId())
                    .eq(Inventory::getWarehouseId, order.getWarehouseId())
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
                inv.setWarehouseId(order.getWarehouseId());
                inv.setLocationId(order.getLocationId());
                inv.setBatchNo(item.getBatchNo());
                inv.setQuantity(item.getQuantity());
                inv.setLockedQty(0);
                inventoryMapper.insert(inv);
            }

            // 移动加权平均法：新均价 = (原库存总金额 + 本次进货金额) ÷ (原库存总数量 + 本次进货数量)
            int newTotalQty = oldTotalQty + item.getQuantity();
            BigDecimal newTotalValue = oldTotalValue.add(
                    item.getAmount() != null ? item.getAmount() : BigDecimal.ZERO);
            BigDecimal avgPrice = BigDecimal.ZERO;
            if (newTotalQty > 0) {
                avgPrice = newTotalValue.divide(BigDecimal.valueOf(newTotalQty), 4, RoundingMode.HALF_UP);
            }
            // 统一设置该商品该仓库下所有批次的成本价
            List<Inventory> allBatches = inventoryMapper.selectList(
                    new LambdaQueryWrapper<Inventory>()
                            .eq(Inventory::getProductId, item.getProductId())
                            .eq(Inventory::getWarehouseId, order.getWarehouseId()));
            for (Inventory b : allBatches) {
                b.setCostPrice(avgPrice);
                inventoryMapper.updateById(b);
            }

            InventoryLog log = new InventoryLog();
            log.setProductId(item.getProductId());
            log.setWarehouseId(order.getWarehouseId());
            log.setLocationId(order.getLocationId());
            log.setBatchNo(item.getBatchNo());
            log.setChangeType(OrderStatus.PURCHASE_IN);
            log.setChangeQty(item.getQuantity());
            log.setBeforeQty(beforeQty);
            log.setAfterQty(afterQty);
            log.setUnitPrice(item.getUnitPrice());
            log.setAmount(item.getAmount());
            log.setRefOrderNo(order.getOrderNo());
            log.setOperatorId(order.getOperatorId());
            log.setRemark("采购入库");
            inventoryLogMapper.insert(log);
        }

        order.setStatus(OrderStatus.CONFIRMED);
        purchaseOrderMapper.updateById(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public synchronized void cancel(Long id) {
        PurchaseOrder order = purchaseOrderMapper.selectById(id);
        if (order == null) throw new BusinessException("采购单不存在");
        if (order.getStatus() == OrderStatus.CANCELED) throw new BusinessException("采购单已取消");

        if (order.getStatus() == OrderStatus.CONFIRMED) {
            List<PurchaseOrderItem> items = purchaseOrderItemMapper.selectList(
                    new LambdaQueryWrapper<PurchaseOrderItem>().eq(PurchaseOrderItem::getOrderId, id));
            for (PurchaseOrderItem item : items) {
                // 计算取消前的库存总金额和总数量
                List<Inventory> beforeBatches = inventoryMapper.selectList(
                        new LambdaQueryWrapper<Inventory>()
                                .eq(Inventory::getProductId, item.getProductId())
                                .eq(Inventory::getWarehouseId, order.getWarehouseId()));
                BigDecimal beforeTotalValue = BigDecimal.ZERO;
                int beforeTotalQty = 0;
                for (Inventory b : beforeBatches) {
                    if (b.getCostPrice() != null) {
                        beforeTotalValue = beforeTotalValue.add(
                                b.getCostPrice().multiply(BigDecimal.valueOf(b.getQuantity())));
                    }
                    beforeTotalQty += b.getQuantity();
                }

                LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getProductId, item.getProductId())
                        .eq(Inventory::getWarehouseId, order.getWarehouseId())
                        .eq(item.getBatchNo() != null, Inventory::getBatchNo, item.getBatchNo());
                Inventory inv = inventoryMapper.selectOne(wrapper);
                if (inv != null) {
                    int beforeQty = inv.getQuantity();
                    inv.setQuantity(beforeQty - item.getQuantity());
                    inventoryMapper.updateById(inv);

                    // 取消后重新计算加权平均成本价 = (原总金额 - 取消批次金额) ÷ 剩余总数量
                    int afterTotalQty = beforeTotalQty - item.getQuantity();
                    if (afterTotalQty > 0) {
                        BigDecimal afterTotalValue = beforeTotalValue.subtract(
                                item.getAmount() != null ? item.getAmount() : BigDecimal.ZERO);
                        BigDecimal avgPrice = afterTotalValue.divide(
                                BigDecimal.valueOf(afterTotalQty), 4, RoundingMode.HALF_UP);
                        List<Inventory> remainingBatches = inventoryMapper.selectList(
                                new LambdaQueryWrapper<Inventory>()
                                        .eq(Inventory::getProductId, item.getProductId())
                                        .eq(Inventory::getWarehouseId, order.getWarehouseId()));
                        for (Inventory b : remainingBatches) {
                            b.setCostPrice(avgPrice);
                            inventoryMapper.updateById(b);
                        }
                    }

                    InventoryLog log = new InventoryLog();
                    log.setProductId(item.getProductId());
                    log.setWarehouseId(order.getWarehouseId());
                    log.setLocationId(order.getLocationId());
                    log.setBatchNo(item.getBatchNo());
                    log.setChangeType("PURCHASE_CANCEL");
                    log.setChangeQty(-item.getQuantity());
                    log.setBeforeQty(beforeQty);
                    log.setAfterQty(inv.getQuantity());
                    log.setRefOrderNo(order.getOrderNo());
                    log.setOperatorId(order.getOperatorId());
                    log.setRemark("采购入库取消，回滚库存");
                    inventoryLogMapper.insert(log);
                }
            }
        }

        order.setStatus(OrderStatus.CANCELED);
        purchaseOrderMapper.updateById(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateDraft(PurchaseOrder order) {
        PurchaseOrder existing = purchaseOrderMapper.selectById(order.getId());
        if (existing == null) throw new BusinessException("采购单不存在");
        if (existing.getStatus() != OrderStatus.DRAFT) throw new BusinessException("仅草稿状态可编辑");

        // 删除旧明细
        purchaseOrderItemMapper.delete(new LambdaQueryWrapper<PurchaseOrderItem>()
                .eq(PurchaseOrderItem::getOrderId, order.getId()));

        // 重新计算
        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalQty = 0;
        List<PurchaseOrderItem> items = order.getItems();
        if (items != null) {
            for (PurchaseOrderItem item : items) {
                item.setId(null);
                item.setOrderId(order.getId());
                if (item.getUnitPrice() == null) item.setUnitPrice(BigDecimal.ZERO);
                BigDecimal amount = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                item.setAmount(amount);
                totalAmount = totalAmount.add(amount);
                totalQty += item.getQuantity();
                purchaseOrderItemMapper.insert(item);
            }
        }
        order.setTotalAmount(totalAmount);
        order.setTotalQuantity(totalQty);
        purchaseOrderMapper.updateById(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        PurchaseOrder order = purchaseOrderMapper.selectById(id);
        if (order == null) throw new BusinessException("采购单不存在");
        if (order.getStatus() == OrderStatus.CONFIRMED) throw new BusinessException("已入库的订单不能作废，请先取消");
        order.setStatus(OrderStatus.VOIDED);
        purchaseOrderMapper.updateById(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Long> ids) {
        for (Long id : ids) {
            PurchaseOrder order = purchaseOrderMapper.selectById(id);
            if (order != null && order.getStatus() != OrderStatus.CONFIRMED) {
                order.setStatus(OrderStatus.VOIDED);
                purchaseOrderMapper.updateById(order);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void voidOrder(Long id, String reason) {
        PurchaseOrder order = purchaseOrderMapper.selectById(id);
        if (order == null) throw new BusinessException("采购单不存在");
        if (order.getStatus() == OrderStatus.CONFIRMED) throw new BusinessException("已确认的单据不可作废");
        order.setStatus(OrderStatus.VOIDED);
        order.setRemark((order.getRemark() != null ? order.getRemark() + " | " : "") + "作废原因: " + (reason != null ? reason : ""));
        order.setUpdateTime(LocalDateTime.now());
        purchaseOrderMapper.updateById(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public void batchVoid(List<Long> ids, String reason) {
        for (Long id : ids) {
            voidOrder(id, reason);
        }
    }

    private synchronized String generateOrderNo() {
        String prefix = "PO";
        String dateStr = DateUtil.format(new Date(), "yyyyMMdd");
        String likePrefix = prefix + dateStr;
        LambdaQueryWrapper<PurchaseOrder> wrapper = new LambdaQueryWrapper<PurchaseOrder>()
                .likeRight(PurchaseOrder::getOrderNo, likePrefix)
                .orderByDesc(PurchaseOrder::getOrderNo);
        Page<PurchaseOrder> page = purchaseOrderMapper.selectPage(new Page<>(1, 1), wrapper);
        int seq = 1;
        if (!page.getRecords().isEmpty()) {
            String lastNo = page.getRecords().get(0).getOrderNo();
            seq = Integer.parseInt(lastNo.substring(lastNo.length() - 4)) + 1;
        }
        String orderNo = prefix + dateStr + String.format("%04d", seq);
        while (purchaseOrderMapper.selectCount(new LambdaQueryWrapper<PurchaseOrder>().eq(PurchaseOrder::getOrderNo, orderNo)) > 0) {
            seq++;
            orderNo = prefix + dateStr + String.format("%04d", seq);
        }
        return orderNo;
    }
}
