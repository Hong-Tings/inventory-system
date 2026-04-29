package com.inventory.sales.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inventory.common.constant.OrderStatus;
import com.inventory.common.result.PageResult;
import com.inventory.common.result.R;
import com.inventory.common.util.ExcelUtil;
import com.inventory.sales.entity.SalesOrder;
import com.inventory.sales.entity.SalesOrderExportVO;
import com.inventory.sales.service.SalesOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "销售订单管理")
@RestController
@RequestMapping("/api/v1/sales-order")
@RequiredArgsConstructor
public class SalesOrderController {

    private final SalesOrderService salesOrderService;

    @Operation(summary = "分页查询销售订单")
    @GetMapping("/page")
    public R<PageResult<SalesOrder>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Integer minQuantity,
            @RequestParam(required = false) Integer maxQuantity,
            @RequestParam(required = false) BigDecimal minAmount,
            @RequestParam(required = false) BigDecimal maxAmount,
            @RequestParam(required = false) String salesman,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return R.ok(PageResult.of(salesOrderService.page(new Page<>(page, size), orderNo, customerId, warehouseId, minQuantity, maxQuantity, minAmount, maxAmount, salesman, status, startDate, endDate)));
    }

    @Operation(summary = "获取销售订单详情")
    @GetMapping("/{id}")
    public R<SalesOrder> getById(@PathVariable Long id) {
        return R.ok(salesOrderService.getDetail(id));
    }

    @Operation(summary = "新增销售订单")
    @PostMapping
    public R<Long> create(@RequestBody SalesOrder order) {
        order.setOperatorId(StpUtil.getLoginIdAsLong());
        return R.ok(salesOrderService.create(order));
    }

    @Operation(summary = "提交销售订单")
    @PutMapping("/{id}/submit")
    public R<Void> submit(@PathVariable Long id) {
        salesOrderService.submit(id);
        return R.ok();
    }

    @Operation(summary = "取消销售订单")
    @PutMapping("/{id}/cancel")
    public R<Void> cancel(@PathVariable Long id) {
        salesOrderService.cancel(id);
        return R.ok();
    }

    @Operation(summary = "更新草稿销售订单")
    @PutMapping("/{id}/draft")
    public R<Void> updateDraft(@PathVariable Long id, @RequestBody SalesOrder order) {
        order.setId(id);
        salesOrderService.updateDraft(order);
        return R.ok();
    }

    @Operation(summary = "导出销售订单")
    @GetMapping("/export")
    public void export(HttpServletResponse response) {
        List<SalesOrder> list = salesOrderService.listAll();
        List<SalesOrderExportVO> voList = list.stream()
                .filter(o -> o.getStatus() != null && o.getStatus() != 3)
                .map(order -> {
            SalesOrderExportVO vo = new SalesOrderExportVO();
            vo.setOrderNo(order.getOrderNo());
            vo.setCustomerName(order.getCustomerName());
            vo.setWarehouseName(order.getWarehouseName());
            vo.setTotalQuantity(order.getTotalQuantity());
            vo.setTotalAmount(order.getTotalAmount());
            vo.setSalesman(order.getSalesman());
            vo.setOperatorName(order.getOperatorName());
            vo.setOrderDate(order.getOrderDate());
            if (order.getCreateTime() != null) vo.setCreateTime(order.getCreateTime());
            String statusText;
            if (order.getStatus() == OrderStatus.DRAFT) {
                statusText = "草稿";
            } else if (order.getStatus() == OrderStatus.CONFIRMED) {
                statusText = "已出库";
            } else if (order.getStatus() == OrderStatus.CANCELED) {
                statusText = "已取消";
            } else {
                statusText = "未知";
            }
            vo.setStatus(statusText);
            return vo;
        }).collect(Collectors.toList());
        ExcelUtil.export(response, voList, "销售订单列表", SalesOrderExportVO.class);
    }

    @SaCheckRole("role_1")
    @Operation(summary = "删除销售订单")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        salesOrderService.delete(id);
        return R.ok();
    }

    @SaCheckRole("role_1")
    @Operation(summary = "批量删除销售订单")
    @DeleteMapping("/batch")
    public R<Void> batchDelete(@RequestBody List<Long> ids) {
        salesOrderService.batchDelete(ids);
        return R.ok();
    }

    @SaCheckRole("role_1")
    @Operation(summary = "作废销售订单")
    @PutMapping("/{id}/void")
    public R<Void> voidOrder(@PathVariable Long id, @RequestBody Map<String, String> body) {
        salesOrderService.voidOrder(id, body.getOrDefault("reason", ""));
        return R.ok();
    }

    @SaCheckRole("role_1")
    @Operation(summary = "批量作废销售订单")
    @PutMapping("/batch-void")
    public R<Void> batchVoid(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Integer> ids = (List<Integer>) body.get("ids");
        String reason = (String) body.getOrDefault("reason", "");
        salesOrderService.batchVoid(ids.stream().map(Long::valueOf).collect(Collectors.toList()), reason);
        return R.ok();
    }
}
