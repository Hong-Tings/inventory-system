package com.inventory.report.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.inventory.inventory.entity.Inventory;
import com.inventory.inventory.mapper.InventoryMapper;
import com.inventory.inventory.service.InventoryService;
import com.inventory.product.entity.Product;
import com.inventory.product.mapper.ProductMapper;
import com.inventory.purchase.entity.PurchaseOrder;
import com.inventory.purchase.mapper.PurchaseOrderMapper;
import com.inventory.sales.entity.SalesOrder;
import com.inventory.sales.entity.SalesOrderItem;
import com.inventory.sales.mapper.SalesOrderItemMapper;
import com.inventory.sales.mapper.SalesOrderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final PurchaseOrderMapper purchaseOrderMapper;
    private final SalesOrderMapper salesOrderMapper;
    private final SalesOrderItemMapper salesOrderItemMapper;
    private final InventoryMapper inventoryMapper;
    private final ProductMapper productMapper;
    private final InventoryService inventoryService;

    public Map<String, Object> purchaseSummary(int days) {
        Map<String, Object> result = new HashMap<>();
        LocalDate since = LocalDate.now().minusDays(days);

        List<PurchaseOrder> orders = purchaseOrderMapper.selectList(
                new LambdaQueryWrapper<PurchaseOrder>()
                        .eq(PurchaseOrder::getStatus, 1)
                        .ge(PurchaseOrder::getOrderDate, since)
                        .orderByAsc(PurchaseOrder::getOrderDate));

        List<String> labels = new ArrayList<>();
        List<Integer> inQty = new ArrayList<>();
        List<BigDecimal> inAmt = new ArrayList<>();

        Map<LocalDate, List<PurchaseOrder>> grouped = orders.stream()
                .collect(Collectors.groupingBy(PurchaseOrder::getOrderDate));
        List<LocalDate> sortedDates = new ArrayList<>(grouped.keySet());
        Collections.sort(sortedDates);

        for (LocalDate date : sortedDates) {
            List<PurchaseOrder> dayOrders = grouped.get(date);
            labels.add(date.toString());
            inQty.add(dayOrders.stream().mapToInt(PurchaseOrder::getTotalQuantity).sum());
            inAmt.add(dayOrders.stream().map(PurchaseOrder::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
        }

        result.put("labels", labels);
        result.put("inQty", inQty);
        result.put("inAmt", inAmt);
        result.put("totalOrders", orders.size());
        return result;
    }

    public Map<String, Object> salesSummary(int days) {
        Map<String, Object> result = new HashMap<>();
        LocalDate since = LocalDate.now().minusDays(days);

        List<SalesOrder> orders = salesOrderMapper.selectList(
                new LambdaQueryWrapper<SalesOrder>()
                        .eq(SalesOrder::getStatus, 1)
                        .ge(SalesOrder::getOrderDate, since)
                        .orderByAsc(SalesOrder::getOrderDate));

        List<String> labels = new ArrayList<>();
        List<Integer> outQty = new ArrayList<>();
        List<BigDecimal> outAmt = new ArrayList<>();

        Map<LocalDate, List<SalesOrder>> grouped = orders.stream()
                .collect(Collectors.groupingBy(SalesOrder::getOrderDate));
        List<LocalDate> sortedDates = new ArrayList<>(grouped.keySet());
        Collections.sort(sortedDates);

        for (LocalDate date : sortedDates) {
            List<SalesOrder> dayOrders = grouped.get(date);
            labels.add(date.toString());
            outQty.add(dayOrders.stream().mapToInt(SalesOrder::getTotalQuantity).sum());
            outAmt.add(dayOrders.stream().map(SalesOrder::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add));
        }

        result.put("labels", labels);
        result.put("outQty", outQty);
        result.put("outAmt", outAmt);
        result.put("totalOrders", orders.size());
        return result;
    }

    public List<Map<String, Object>> turnoverRate(int days) {
        LocalDate since = LocalDate.now().minusDays(days);
        List<Map<String, Object>> result = new ArrayList<>();

        List<SalesOrder> orders = salesOrderMapper.selectList(
                new LambdaQueryWrapper<SalesOrder>()
                        .eq(SalesOrder::getStatus, 1)
                        .ge(SalesOrder::getOrderDate, since));

        Map<Long, Integer> productOutQty = new HashMap<>();
        for (SalesOrder order : orders) {
            List<SalesOrderItem> items = salesOrderItemMapper.selectList(
                    new LambdaQueryWrapper<SalesOrderItem>().eq(SalesOrderItem::getOrderId, order.getId()));
            for (SalesOrderItem item : items) {
                productOutQty.merge(item.getProductId(), item.getQuantity(), Integer::sum);
            }
        }

        List<Inventory> inventoryList = inventoryMapper.selectList(null);
        Map<Long, Integer> productInventory = new HashMap<>();
        for (Inventory inv : inventoryList) {
            productInventory.merge(inv.getProductId(), inv.getQuantity(), Integer::sum);
        }

        for (Map.Entry<Long, Integer> entry : productOutQty.entrySet()) {
            Long pid = entry.getKey();
            int outQty = entry.getValue();
            int avgInv = productInventory.getOrDefault(pid, 0);
            if (avgInv == 0) continue;

            Map<String, Object> item = new HashMap<>();
            Product p = productMapper.selectById(pid);
            item.put("productName", p != null ? p.getName() : "未知");
            item.put("totalOut", outQty);
            item.put("avgInventory", avgInv);
            item.put("turnoverRate", (double) outQty / avgInv);
            result.add(item);
        }
        result.sort((a, b) -> Double.compare(
                ((Number) b.get("turnoverRate")).doubleValue(),
                ((Number) a.get("turnoverRate")).doubleValue()));
        return result;
    }

    public List<Map<String, Object>> inventoryAlert() {
        return inventoryService.getAlertList();
    }
}
