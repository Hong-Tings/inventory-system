package com.inventory.warehouse.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inventory.common.exception.BusinessException;
import com.inventory.inventory.entity.Inventory;
import com.inventory.inventory.mapper.InventoryMapper;
import com.inventory.product.entity.Product;
import com.inventory.product.mapper.ProductMapper;
import com.inventory.purchase.entity.PurchaseOrder;
import com.inventory.purchase.mapper.PurchaseOrderMapper;
import com.inventory.sales.entity.SalesOrder;
import com.inventory.sales.mapper.SalesOrderMapper;
import com.inventory.transfer.entity.InventoryTransfer;
import com.inventory.transfer.mapper.InventoryTransferMapper;
import com.inventory.warehouse.entity.Warehouse;
import com.inventory.warehouse.mapper.WarehouseMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import cn.hutool.core.date.DateUtil;
import java.util.Date;

@Service
public class WarehouseService {

    private final WarehouseMapper warehouseMapper;
    private final InventoryMapper inventoryMapper;
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final SalesOrderMapper salesOrderMapper;
    private final InventoryTransferMapper transferMapper;
    private final ProductMapper productMapper;

    public WarehouseService(WarehouseMapper warehouseMapper, InventoryMapper inventoryMapper,
                            PurchaseOrderMapper purchaseOrderMapper, SalesOrderMapper salesOrderMapper,
                            InventoryTransferMapper transferMapper,
                            ProductMapper productMapper) {
        this.warehouseMapper = warehouseMapper;
        this.inventoryMapper = inventoryMapper;
        this.purchaseOrderMapper = purchaseOrderMapper;
        this.salesOrderMapper = salesOrderMapper;
        this.transferMapper = transferMapper;
        this.productMapper = productMapper;
    }

    public Warehouse getById(Long id) { return warehouseMapper.selectById(id); }

    public List<Warehouse> tree() {
        List<Warehouse> all = warehouseMapper.selectList(new LambdaQueryWrapper<Warehouse>()
                .eq(Warehouse::getStatus, 1).orderByAsc(Warehouse::getId));
        for (Warehouse w : all) enrichStats(w);
        Map<Long, Warehouse> map = all.stream().collect(Collectors.toMap(Warehouse::getId, w -> w));
        List<Warehouse> roots = new java.util.ArrayList<>();
        for (Warehouse w : all) {
            if (w.getParentId() == null) {
                roots.add(w);
            } else {
                Warehouse parent = map.get(w.getParentId());
                if (parent != null) {
                    if (parent.getChildren() == null) parent.setChildren(new java.util.ArrayList<>());
                    parent.getChildren().add(w);
                }
            }
        }
        return roots;
    }

    public List<Warehouse> children(Long parentId) {
        return warehouseMapper.selectList(new LambdaQueryWrapper<Warehouse>()
                .eq(Warehouse::getParentId, parentId)
                .eq(Warehouse::getStatus, 1)
                .orderByAsc(Warehouse::getId));
    }

    public List<Warehouse> search(String keyword) {
        return warehouseMapper.selectList(new LambdaQueryWrapper<Warehouse>()
                .eq(Warehouse::getStatus, 1)
                .and(w -> w.like(Warehouse::getName, keyword).or().like(Warehouse::getCode, keyword))
                .orderByAsc(Warehouse::getId));
    }

    public List<Warehouse> listAll() {
        List<Warehouse> list = warehouseMapper.selectList(new LambdaQueryWrapper<Warehouse>()
                .eq(Warehouse::getStatus, 1).eq(Warehouse::getLevel, 4).orderByAsc(Warehouse::getId));
        for (Warehouse w : list) enrichStats(w);
        return list;
    }

    public Page<Warehouse> page(Page<Warehouse> page, String name, String contact, String phone,
                                String address, Integer status, Integer level, Long parentId) {
        LambdaQueryWrapper<Warehouse> wrapper = new LambdaQueryWrapper<Warehouse>()
                .like(name != null, Warehouse::getName, name)
                .like(contact != null, Warehouse::getContact, contact)
                .like(phone != null, Warehouse::getPhone, phone)
                .like(address != null, Warehouse::getAddress, address)
                .eq(status != null, Warehouse::getStatus, status)
                .eq(level != null, Warehouse::getLevel, level)
                .eq(parentId != null, Warehouse::getParentId, parentId)
                .orderByDesc(Warehouse::getId);
        Page<Warehouse> result = warehouseMapper.selectPage(page, wrapper);
        for (Warehouse w : result.getRecords()) {
            enrichStats(w);
            if (w.getParentId() != null) {
                Warehouse parent = warehouseMapper.selectById(w.getParentId());
                if (parent != null) w.setParentName(parent.getName());
            }
        }
        return result;
    }

    private void enrichStats(Warehouse w) {
        List<Inventory> invs = inventoryMapper.selectList(
                new LambdaQueryWrapper<Inventory>().eq(Inventory::getWarehouseId, w.getId()));
        int count = invs.stream().mapToInt(Inventory::getQuantity).sum();
        w.setProductCount(count);

        BigDecimal amount = BigDecimal.ZERO;
        for (Inventory inv : invs) {
            if (inv.getCostPrice() != null) {
                amount = amount.add(inv.getCostPrice().multiply(BigDecimal.valueOf(inv.getQuantity())));
            }
        }
        w.setTotalAmount(amount);
    }

    @Transactional(rollbackFor = Exception.class)
    public void save(Warehouse warehouse) {
        if (warehouse.getLevel() == null || warehouse.getLevel() == 4) {
            warehouse.setCode(generateWarehouseCode());
        }
        warehouseMapper.insert(warehouse);
    }

    private synchronized String generateWarehouseCode() {
        String prefix = "WH";
        String dateStr = DateUtil.format(new Date(), "yyyyMMdd");
        String likePrefix = prefix + dateStr;

        String maxCode = warehouseMapper.selectMaxCodeByPrefix(likePrefix);
        int seq = 1;
        if (maxCode != null) {
            seq = Integer.parseInt(maxCode.substring(maxCode.length() - 3)) + 1;
        }

        String code = likePrefix + String.format("%03d", seq);
        while (warehouseMapper.countAllByCode(code) > 0) {
            seq++;
            code = likePrefix + String.format("%03d", seq);
        }
        return code;
    }

    @Transactional
    public void update(Warehouse warehouse) {
        warehouseMapper.updateById(warehouse);
    }

    @Transactional
    public void delete(Long id) {
        // 检查库存
        long invCount = inventoryMapper.selectCount(
                new LambdaQueryWrapper<Inventory>().eq(Inventory::getWarehouseId, id));
        if (invCount > 0) throw new BusinessException("该仓库存在库存记录，无法删除，请先清空库存");

        // 检查采购入库单引用（包含所有状态）
        long purchaseCount = purchaseOrderMapper.selectCount(
                new LambdaQueryWrapper<PurchaseOrder>().eq(PurchaseOrder::getWarehouseId, id));
        if (purchaseCount > 0) throw new BusinessException("该仓库已被采购入库单引用，无法删除");

        // 检查销售出库单引用（包含所有状态）
        long salesCount = salesOrderMapper.selectCount(
                new LambdaQueryWrapper<SalesOrder>().eq(SalesOrder::getWarehouseId, id));
        if (salesCount > 0) throw new BusinessException("该仓库已被销售出库单引用，无法删除");

        // 检查调拨单引用（包含所有状态）
        long transferCount = transferMapper.selectCount(
                new LambdaQueryWrapper<InventoryTransfer>()
                        .eq(InventoryTransfer::getFromWarehouseId, id)
                        .or()
                        .eq(InventoryTransfer::getToWarehouseId, id));
        if (transferCount > 0) throw new BusinessException("该仓库已被调拨单引用，无法删除");

        warehouseMapper.deleteById(id);
    }

    @Transactional
    public void restore(Long id) {
        Warehouse w = warehouseMapper.selectById(id);
        if (w == null) throw new BusinessException("仓库不存在");
        w.setDeleted(0);
        warehouseMapper.updateById(w);
    }
}
