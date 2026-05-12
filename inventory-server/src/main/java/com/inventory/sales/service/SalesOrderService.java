package com.inventory.sales.service;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inventory.common.constant.OrderStatus;
import com.inventory.common.exception.BusinessException;
import com.inventory.customer.entity.Customer;
import com.inventory.customer.mapper.CustomerMapper;
import com.inventory.inventory.entity.Inventory;
import com.inventory.inventory.entity.InventoryLog;
import com.inventory.inventory.mapper.InventoryLogMapper;
import com.inventory.inventory.mapper.InventoryMapper;
import com.inventory.product.entity.Product;
import com.inventory.product.mapper.ProductMapper;
import com.inventory.sales.entity.SalesOrder;
import com.inventory.sales.entity.SalesOrderItem;
import com.inventory.sales.mapper.SalesOrderItemMapper;
import com.inventory.sales.mapper.SalesOrderMapper;
import com.inventory.system.mapper.SysUserMapper;
import com.inventory.warehouse.entity.Warehouse;
import com.inventory.warehouse.mapper.WarehouseMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Service
public class SalesOrderService {

    private final SalesOrderMapper salesOrderMapper;
    private final SalesOrderItemMapper salesOrderItemMapper;
    private final InventoryMapper inventoryMapper;
    private final InventoryLogMapper inventoryLogMapper;
    private final CustomerMapper customerMapper;
    private final WarehouseMapper warehouseMapper;
    private final SysUserMapper userMapper;
    private final ProductMapper productMapper;

    public SalesOrderService(SalesOrderMapper salesOrderMapper,
                             SalesOrderItemMapper salesOrderItemMapper,
                             InventoryMapper inventoryMapper,
                             InventoryLogMapper inventoryLogMapper,
                             CustomerMapper customerMapper,
                             WarehouseMapper warehouseMapper,
                             SysUserMapper userMapper,
                             ProductMapper productMapper) {
        this.salesOrderMapper = salesOrderMapper;
        this.salesOrderItemMapper = salesOrderItemMapper;
        this.inventoryMapper = inventoryMapper;
        this.inventoryLogMapper = inventoryLogMapper;
        this.customerMapper = customerMapper;
        this.warehouseMapper = warehouseMapper;
        this.userMapper = userMapper;
        this.productMapper = productMapper;
    }

    public Page<SalesOrder> page(Page<SalesOrder> page, String orderNo, Long customerId, Long warehouseId,
                                 Integer minQuantity, Integer maxQuantity, BigDecimal minAmount, BigDecimal maxAmount,
                                 String salesman, Integer status,
                                 LocalDate startDate, LocalDate endDate) {
        LambdaQueryWrapper<SalesOrder> wrapper = new LambdaQueryWrapper<SalesOrder>()
                .like(orderNo != null, SalesOrder::getOrderNo, orderNo)
                .eq(customerId != null, SalesOrder::getCustomerId, customerId)
                .eq(warehouseId != null, SalesOrder::getWarehouseId, warehouseId)
                .ge(minQuantity != null, SalesOrder::getTotalQuantity, minQuantity)
                .le(maxQuantity != null, SalesOrder::getTotalQuantity, maxQuantity)
                .ge(minAmount != null, SalesOrder::getTotalAmount, minAmount)
                .le(maxAmount != null, SalesOrder::getTotalAmount, maxAmount)
                .like(salesman != null, SalesOrder::getSalesman, salesman)
                .eq(status != null, SalesOrder::getStatus, status)
                .ge(startDate != null, SalesOrder::getOrderDate, startDate)
                .le(endDate != null, SalesOrder::getOrderDate, endDate)
                .ne(SalesOrder::getStatus, OrderStatus.VOIDED)
                .orderByDesc(SalesOrder::getId);
        Page<SalesOrder> result = salesOrderMapper.selectPage(page, wrapper);
        for (SalesOrder o : result.getRecords()) enrichOrder(o);
        return result;
    }

    public List<SalesOrder> listAll() {
        List<SalesOrder> list = salesOrderMapper.selectList(
                new LambdaQueryWrapper<SalesOrder>().orderByDesc(SalesOrder::getId));
        for (SalesOrder o : list) {
            enrichOrder(o);
        }
        return list;
    }

    public SalesOrder getDetail(Long id) {
        SalesOrder order = salesOrderMapper.selectById(id);
        if (order != null) {
            List<SalesOrderItem> items = salesOrderItemMapper.selectList(
                    new LambdaQueryWrapper<SalesOrderItem>().eq(SalesOrderItem::getOrderId, id));
            enrichItems(items);
            order.setItems(items);
            enrichOrder(order);
        }
        return order;
    }

    private void enrichOrder(SalesOrder o) {
        if (o.getCustomerId() != null) {
            Customer c = customerMapper.selectById(o.getCustomerId());
            if (c != null) o.setCustomerName(c.getName());
        }
        if (o.getWarehouseId() != null) {
            Warehouse w = warehouseMapper.selectById(o.getWarehouseId());
            if (w != null) o.setWarehouseName(w.getName());
        }
        if (o.getOperatorId() != null) {
            var u = userMapper.selectById(o.getOperatorId());
            if (u != null) o.setOperatorName(u.getRealName());
        }
        if (o.getApproverId() != null) {
            var u = userMapper.selectById(o.getApproverId());
            if (u != null) o.setApproverName(u.getRealName());
        }
    }

    private void enrichItems(List<SalesOrderItem> items) {
        for (SalesOrderItem item : items) {
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
    public Long create(SalesOrder order) {
        order.setOrderNo(generateOrderNo());
        order.setStatus(OrderStatus.DRAFT);
        order.setOrderDate(order.getOrderDate() != null ? order.getOrderDate() : LocalDate.now());

        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalQty = 0;
        List<SalesOrderItem> items = order.getItems();
        if (items != null) {
            for (SalesOrderItem item : items) {
                if (item.getProductId() == null) throw new BusinessException("商品ID不能为空");
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
        salesOrderMapper.insert(order);
        if (items != null) {
            for (SalesOrderItem item : items) {
                item.setOrderId(order.getId());
                salesOrderItemMapper.insert(item);
            }
        }
        return order.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public synchronized void submit(Long id) {
        SalesOrder order = salesOrderMapper.selectById(id);
        if (order == null) throw new BusinessException("销售单不存在");
        if (order.getStatus() != OrderStatus.DRAFT) throw new BusinessException("当前状态不可提交");
        order.setStatus(OrderStatus.PENDING);
        salesOrderMapper.updateById(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public synchronized void approve(Long id) {
        SalesOrder order = salesOrderMapper.selectById(id);
        if (order == null) throw new BusinessException("销售单不存在");
        if (order.getStatus() != OrderStatus.PENDING) throw new BusinessException("当前状态不可审核");

        List<SalesOrderItem> items = salesOrderItemMapper.selectList(
                new LambdaQueryWrapper<SalesOrderItem>().eq(SalesOrderItem::getOrderId, id));

        for (SalesOrderItem item : items) {
            List<Inventory> batchList = inventoryMapper.selectList(
                    new LambdaQueryWrapper<Inventory>()
                            .eq(Inventory::getProductId, item.getProductId())
                            .eq(Inventory::getWarehouseId, order.getWarehouseId())
                            .gt(Inventory::getQuantity, 0)
                            .orderByAsc(Inventory::getId));

            int needQty = item.getQuantity();
            int totalAvailable = batchList.stream().mapToInt(Inventory::getQuantity).sum();
            if (totalAvailable < needQty) {
                throw new BusinessException("商品库存不足");
            }

            for (Inventory batch : batchList) {
                if (needQty <= 0) break;
                int deductQty = Math.min(needQty, batch.getQuantity());
                int beforeQty = batch.getQuantity();
                batch.setQuantity(beforeQty - deductQty);
                inventoryMapper.updateById(batch);

                InventoryLog log = new InventoryLog();
                log.setProductId(item.getProductId());
                log.setWarehouseId(order.getWarehouseId());
                log.setLocationId(batch.getLocationId());
                log.setBatchNo(batch.getBatchNo());
                log.setChangeType(OrderStatus.SALES_OUT);
                log.setChangeQty(-deductQty);
                log.setBeforeQty(beforeQty);
                log.setAfterQty(batch.getQuantity());
                log.setUnitPrice(item.getUnitPrice());
                log.setAmount(item.getUnitPrice() != null
                        ? item.getUnitPrice().multiply(BigDecimal.valueOf(deductQty)) : BigDecimal.ZERO);
                log.setRefOrderNo(order.getOrderNo());
                log.setOperatorId(order.getOperatorId());
                log.setRemark("销售出库(FIFO)");
                inventoryLogMapper.insert(log);
                needQty -= deductQty;
            }
        }

        order.setApproverId(cn.dev33.satoken.stp.StpUtil.getLoginIdAsLong());
        order.setApproveTime(LocalDateTime.now());
        order.setStatus(OrderStatus.CONFIRMED);
        salesOrderMapper.updateById(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public void reject(Long id, String reason) {
        SalesOrder order = salesOrderMapper.selectById(id);
        if (order == null) throw new BusinessException("销售单不存在");
        if (order.getStatus() != OrderStatus.PENDING) throw new BusinessException("当前状态不可驳回");
        order.setStatus(OrderStatus.DRAFT);
        order.setRemark((order.getRemark() != null ? order.getRemark() + " | " : "") + "驳回原因: " + (reason != null ? reason : ""));
        salesOrderMapper.updateById(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public synchronized void cancel(Long id) {
        SalesOrder order = salesOrderMapper.selectById(id);
        if (order == null) throw new BusinessException("销售单不存在");
        if (order.getStatus() == OrderStatus.CANCELED) throw new BusinessException("销售单已取消");
        if (order.getStatus() == OrderStatus.PENDING) {
            long uid = cn.dev33.satoken.stp.StpUtil.getLoginIdAsLong();
            var u = userMapper.selectById(uid);
            if (u == null || u.getRole() == null || u.getRole() != 1) {
                throw new BusinessException("待审批状态仅管理员可取消");
            }
            order.setStatus(OrderStatus.CANCELED);
            salesOrderMapper.updateById(order);
            return;
        }
        if (order.getStatus() == OrderStatus.CONFIRMED) {
            List<SalesOrderItem> items = salesOrderItemMapper.selectList(
                    new LambdaQueryWrapper<SalesOrderItem>().eq(SalesOrderItem::getOrderId, id));
            for (SalesOrderItem item : items) {
                LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getProductId, item.getProductId())
                        .eq(Inventory::getWarehouseId, order.getWarehouseId())
                        .eq(item.getBatchNo() != null && !item.getBatchNo().isEmpty(), Inventory::getBatchNo, item.getBatchNo());
                Inventory inv = inventoryMapper.selectOne(wrapper);
                if (inv != null) {
                    int beforeQty = inv.getQuantity();
                    inv.setQuantity(beforeQty + item.getQuantity());
                    inventoryMapper.updateById(inv);
                    InventoryLog log = new InventoryLog();
                    log.setProductId(item.getProductId());
                    log.setWarehouseId(order.getWarehouseId());
                    log.setChangeType("SALES_CANCEL");
                    log.setChangeQty(item.getQuantity());
                    log.setBeforeQty(beforeQty);
                    log.setAfterQty(inv.getQuantity());
                    log.setRefOrderNo(order.getOrderNo());
                    log.setOperatorId(order.getOperatorId());
                    log.setRemark("销售出库取消，回滚库存");
                    inventoryLogMapper.insert(log);
                }
            }
        }
        order.setStatus(OrderStatus.CANCELED);
        salesOrderMapper.updateById(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateDraft(SalesOrder order) {
        SalesOrder existing = salesOrderMapper.selectById(order.getId());
        if (existing == null) throw new BusinessException("销售单不存在");
        if (existing.getStatus() != OrderStatus.DRAFT) throw new BusinessException("仅草稿状态可编辑");

        salesOrderItemMapper.delete(new LambdaQueryWrapper<SalesOrderItem>()
                .eq(SalesOrderItem::getOrderId, order.getId()));

        BigDecimal totalAmount = BigDecimal.ZERO;
        int totalQty = 0;
        List<SalesOrderItem> items = order.getItems();
        if (items != null) {
            for (SalesOrderItem item : items) {
                item.setId(null);
                item.setOrderId(order.getId());
                if (item.getUnitPrice() == null) item.setUnitPrice(BigDecimal.ZERO);
                BigDecimal amount = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                item.setAmount(amount);
                totalAmount = totalAmount.add(amount);
                totalQty += item.getQuantity();
                salesOrderItemMapper.insert(item);
            }
        }
        order.setTotalAmount(totalAmount);
        order.setTotalQuantity(totalQty);
        salesOrderMapper.updateById(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        SalesOrder order = salesOrderMapper.selectById(id);
        if (order == null) throw new BusinessException("销售单不存在");
        if (order.getStatus() == OrderStatus.CONFIRMED) throw new BusinessException("已出库的订单不能作废，请先取消");
        order.setStatus(OrderStatus.VOIDED);
        salesOrderMapper.updateById(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Long> ids) {
        for (Long id : ids) {
            SalesOrder order = salesOrderMapper.selectById(id);
            if (order != null && order.getStatus() != OrderStatus.CONFIRMED) {
                order.setStatus(OrderStatus.VOIDED);
                salesOrderMapper.updateById(order);
            }
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void voidOrder(Long id, String reason) {
        SalesOrder order = salesOrderMapper.selectById(id);
        if (order == null) throw new BusinessException("销售单不存在");
        if (order.getStatus() == OrderStatus.CONFIRMED) throw new BusinessException("已确认的单据不可作废");
        order.setStatus(OrderStatus.VOIDED);
        order.setRemark((order.getRemark() != null ? order.getRemark() + " | " : "") + "作废原因: " + (reason != null ? reason : ""));
        order.setUpdateTime(LocalDateTime.now());
        salesOrderMapper.updateById(order);
    }

    @Transactional(rollbackFor = Exception.class)
    public void batchVoid(List<Long> ids, String reason) {
        for (Long id : ids) {
            voidOrder(id, reason);
        }
    }

    private synchronized String generateOrderNo() {
        String prefix = "SO";
        String dateStr = DateUtil.format(new Date(), "yyyyMMdd");
        String likePrefix = prefix + dateStr;
        LambdaQueryWrapper<SalesOrder> wrapper = new LambdaQueryWrapper<SalesOrder>()
                .likeRight(SalesOrder::getOrderNo, likePrefix)
                .orderByDesc(SalesOrder::getOrderNo);
        Page<SalesOrder> page = salesOrderMapper.selectPage(new Page<>(1, 1), wrapper);
        int seq = 1;
        if (!page.getRecords().isEmpty()) {
            String lastNo = page.getRecords().get(0).getOrderNo();
            seq = Integer.parseInt(lastNo.substring(lastNo.length() - 4)) + 1;
        }
        String orderNo = prefix + dateStr + String.format("%04d", seq);
        while (salesOrderMapper.selectCount(new LambdaQueryWrapper<SalesOrder>().eq(SalesOrder::getOrderNo, orderNo)) > 0) {
            seq++;
            orderNo = prefix + dateStr + String.format("%04d", seq);
        }
        return orderNo;
    }
}
