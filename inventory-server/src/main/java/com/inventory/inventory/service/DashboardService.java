package com.inventory.inventory.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.inventory.product.mapper.ProductMapper;
import com.inventory.purchase.entity.PurchaseOrder;
import com.inventory.purchase.mapper.PurchaseOrderMapper;
import com.inventory.sales.entity.SalesOrder;
import com.inventory.sales.mapper.SalesOrderMapper;
import com.inventory.warehouse.mapper.WarehouseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final ProductMapper productMapper;
    private final WarehouseMapper warehouseMapper;
    private final PurchaseOrderMapper purchaseOrderMapper;
    private final SalesOrderMapper salesOrderMapper;
    private final InventoryService inventoryService;

    public Map<String, Object> stats() {
        Map<String, Object> result = new HashMap<>();
        result.put("productCount", productMapper.selectCount(null));
        result.put("warehouseCount", warehouseMapper.selectCount(null));

        LocalDate today = LocalDate.now();

        // 今日入库单数和入库总量
        List<PurchaseOrder> todayPurchases = purchaseOrderMapper.selectList(
                new LambdaQueryWrapper<PurchaseOrder>()
                        .eq(PurchaseOrder::getOrderDate, today)
                        .eq(PurchaseOrder::getStatus, 1));
        result.put("todayPurchaseCount", todayPurchases.size());
        result.put("todayPurchaseQty", todayPurchases.stream().mapToInt(PurchaseOrder::getTotalQuantity).sum());

        // 今日出库单数和出库总量
        List<SalesOrder> todaySales = salesOrderMapper.selectList(
                new LambdaQueryWrapper<SalesOrder>()
                        .eq(SalesOrder::getOrderDate, today)
                        .eq(SalesOrder::getStatus, 1));
        result.put("todaySalesCount", todaySales.size());
        result.put("todaySalesQty", todaySales.stream().mapToInt(SalesOrder::getTotalQuantity).sum());

        long alertCount = inventoryService.getAlertList().size();
        result.put("alertCount", alertCount);
        result.put("alertList", alertCount > 0 ? inventoryService.getAlertList() : java.util.Collections.emptyList());
        return result;
    }
}
