package com.inventory.common.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.inventory.common.result.R;
import com.inventory.customer.entity.Customer;
import com.inventory.customer.mapper.CustomerMapper;
import com.inventory.product.entity.Product;
import com.inventory.product.entity.ProductCategory;
import com.inventory.product.mapper.ProductCategoryMapper;
import com.inventory.product.mapper.ProductMapper;
import com.inventory.purchase.entity.PurchaseOrder;
import com.inventory.purchase.mapper.PurchaseOrderMapper;
import com.inventory.sales.entity.SalesOrder;
import com.inventory.sales.mapper.SalesOrderMapper;
import com.inventory.stocktake.entity.StockTake;
import com.inventory.stocktake.mapper.StockTakeMapper;
import com.inventory.supplier.entity.Supplier;
import com.inventory.supplier.mapper.SupplierMapper;
import com.inventory.transfer.entity.InventoryTransfer;
import com.inventory.transfer.mapper.InventoryTransferMapper;
import com.inventory.warehouse.entity.Warehouse;
import com.inventory.warehouse.mapper.WarehouseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Tag(name = "已作废列表")
@RestController
@RequestMapping("/api/v1/recycle")
@SaCheckRole("role_1")
@RequiredArgsConstructor
public class RecycleBinController {

    private final ProductMapper productMapper;
    private final ProductCategoryMapper productCategoryMapper;
    private final SupplierMapper supplierMapper;
    private final CustomerMapper customerMapper;
    private final WarehouseMapper warehouseMapper;
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final SalesOrderMapper salesOrderMapper;
    private final InventoryTransferMapper transferMapper;
    private final StockTakeMapper stockTakeMapper;

    @Operation(summary = "获取所有数据（分类返回）")
    @GetMapping("/list")
    public R<Map<String, Object>> list() {
        Map<String, Object> result = new LinkedHashMap<>();

        // 基础数据（软删除：deleted=1）
        result.put("products", productMapper.selectDeleted());
        result.put("categories", productCategoryMapper.selectDeleted());
        result.put("suppliers", supplierMapper.selectDeleted());
        result.put("customers", customerMapper.selectDeleted());
        result.put("warehouses", warehouseMapper.selectDeleted());

        // 订单类（status=3 = 已作废），填充关联名称
        List<PurchaseOrder> purchaseOrders = purchaseOrderMapper.selectList(
                new LambdaQueryWrapper<PurchaseOrder>().eq(PurchaseOrder::getStatus, 3));
        for (PurchaseOrder o : purchaseOrders) {
            if (o.getSupplierId() != null) {
                Supplier s = supplierMapper.selectAnyById(o.getSupplierId());
                if (s != null) o.setSupplierName(s.getName());
            }
            if (o.getWarehouseId() != null) {
                Warehouse w = warehouseMapper.selectAnyById(o.getWarehouseId());
                if (w != null) o.setWarehouseName(w.getName());
            }
        }
        result.put("purchaseOrders", purchaseOrders);

        List<SalesOrder> salesOrders = salesOrderMapper.selectList(
                new LambdaQueryWrapper<SalesOrder>().eq(SalesOrder::getStatus, 3));
        for (SalesOrder o : salesOrders) {
            if (o.getCustomerId() != null) {
                Customer c = customerMapper.selectAnyById(o.getCustomerId());
                if (c != null) o.setCustomerName(c.getName());
            }
            if (o.getWarehouseId() != null) {
                Warehouse w = warehouseMapper.selectAnyById(o.getWarehouseId());
                if (w != null) o.setWarehouseName(w.getName());
            }
        }
        result.put("salesOrders", salesOrders);

        List<InventoryTransfer> transfers = transferMapper.selectList(
                new LambdaQueryWrapper<InventoryTransfer>().eq(InventoryTransfer::getStatus, 3));
        for (InventoryTransfer o : transfers) {
            if (o.getFromWarehouseId() != null) {
                Warehouse w = warehouseMapper.selectAnyById(o.getFromWarehouseId());
                if (w != null) o.setFromWarehouseName(w.getName());
            }
            if (o.getToWarehouseId() != null) {
                Warehouse w = warehouseMapper.selectAnyById(o.getToWarehouseId());
                if (w != null) o.setToWarehouseName(w.getName());
            }
        }
        result.put("transfers", transfers);

        List<StockTake> stockTakes = stockTakeMapper.selectList(
                new LambdaQueryWrapper<StockTake>().eq(StockTake::getStatus, 3));
        for (StockTake o : stockTakes) {
            if (o.getWarehouseId() != null) {
                Warehouse w = warehouseMapper.selectAnyById(o.getWarehouseId());
                if (w != null) o.setWarehouseName(w.getName());
            }
        }
        result.put("stockTakes", stockTakes);

        return R.ok(result);
    }
}
