package com.inventory.inventory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inventory.inventory.entity.Inventory;
import com.inventory.inventory.mapper.InventoryMapper;
import com.inventory.product.entity.Product;
import com.inventory.product.mapper.ProductMapper;
import com.inventory.warehouse.entity.Warehouse;
import com.inventory.warehouse.mapper.WarehouseMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class InventoryService {

    private final InventoryMapper inventoryMapper;
    private final ProductMapper productMapper;
    private final WarehouseMapper warehouseMapper;

    public InventoryService(InventoryMapper inventoryMapper, ProductMapper productMapper,
                            WarehouseMapper warehouseMapper) {
        this.inventoryMapper = inventoryMapper;
        this.productMapper = productMapper;
        this.warehouseMapper = warehouseMapper;
    }

    public Page<Inventory> page(Page<Inventory> page, Long productId, String productName, Long warehouseId) {
        LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<Inventory>()
                .eq(productId != null, Inventory::getProductId, productId)
                .eq(warehouseId != null, Inventory::getWarehouseId, warehouseId)
                .orderByDesc(Inventory::getId);

        // 按商品名称搜索：先查出匹配的商品ID，再过滤库存
        if (productName != null && !productName.isEmpty() && productId == null) {
            List<Product> matchedProducts = productMapper.selectList(
                    new LambdaQueryWrapper<Product>()
                            .like(Product::getName, productName)
                            .or().like(Product::getCode, productName)
            );
            if (!matchedProducts.isEmpty()) {
                List<Long> productIds = matchedProducts.stream().map(Product::getId).collect(Collectors.toList());
                wrapper.in(Inventory::getProductId, productIds);
            } else {
                // 无匹配商品，返回空结果
                return page;
            }
        }
        Page<Inventory> result = inventoryMapper.selectPage(page, wrapper);
        // 填充名称
        for (Inventory inv : result.getRecords()) {
            if (inv.getProductId() != null) {
                Product p = productMapper.selectById(inv.getProductId());
                if (p != null) { inv.setProductName(p.getName()); inv.setProductCode(p.getCode()); }
            }
            if (inv.getWarehouseId() != null) {
                Warehouse w = warehouseMapper.selectById(inv.getWarehouseId());
                if (w != null) { inv.setWarehouseName(w.getName()); inv.setWarehouseCode(w.getCode()); }
            }
        }
        return result;
    }

    public List<Inventory> listAll() {
        List<Inventory> list = inventoryMapper.selectList(null);
        for (Inventory inv : list) {
            if (inv.getProductId() != null) {
                Product p = productMapper.selectById(inv.getProductId());
                if (p != null) {
                    inv.setProductName(p.getName());
                    inv.setProductCode(p.getCode());
                }
            }
            if (inv.getWarehouseId() != null) {
                Warehouse w = warehouseMapper.selectById(inv.getWarehouseId());
                if (w != null) { inv.setWarehouseName(w.getName()); inv.setWarehouseCode(w.getCode()); }
            }
        }
        return list;
    }

    public List<Map<String, Object>> getAlertList() {
        List<Inventory> all = inventoryMapper.selectList(null);
        Map<Long, Product> productCache = new HashMap<>();
        List<Map<String, Object>> alerts = new ArrayList<>();
        for (Inventory inv : all) {
            Product p = productCache.computeIfAbsent(inv.getProductId(), id -> productMapper.selectById(id));
            if (p != null && p.getMinStock() != null && inv.getQuantity() < p.getMinStock()) {
                Map<String, Object> item = new HashMap<>();
                item.put("productName", p.getName());
                item.put("productCode", p.getCode());
                item.put("quantity", inv.getQuantity());
                item.put("minStock", p.getMinStock());
                Warehouse w = warehouseMapper.selectById(inv.getWarehouseId());
                item.put("warehouseName", w != null ? w.getName() : "");
                alerts.add(item);
            }
        }
        return alerts;
    }
}
