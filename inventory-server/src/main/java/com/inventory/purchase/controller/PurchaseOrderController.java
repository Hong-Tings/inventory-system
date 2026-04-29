package com.inventory.purchase.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inventory.common.constant.OrderStatus;
import com.inventory.common.result.PageResult;
import com.inventory.common.result.R;
import com.inventory.common.util.ExcelUtil;
import com.inventory.purchase.entity.PurchaseOrder;
import com.inventory.purchase.entity.PurchaseOrderExportVO;
import com.inventory.purchase.service.PurchaseOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "采购订单管理")
@RestController
@RequestMapping("/api/v1/purchase-order")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final PurchaseOrderService purchaseOrderService;

    @Operation(summary = "分页查询采购订单")
    @GetMapping("/page")
    public R<PageResult<PurchaseOrder>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Integer minQuantity,
            @RequestParam(required = false) Integer maxQuantity,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return R.ok(PageResult.of(purchaseOrderService.page(new Page<>(page, size), orderNo, supplierId, warehouseId, minQuantity, maxQuantity, status, startDate, endDate)));
    }

    @Operation(summary = "获取采购订单详情")
    @GetMapping("/{id}")
    public R<PurchaseOrder> getById(@PathVariable Long id) {
        return R.ok(purchaseOrderService.getDetail(id));
    }

    @Operation(summary = "新增采购订单")
    @PostMapping
    public R<Long> create(@RequestBody PurchaseOrder order) {
        order.setOperatorId(StpUtil.getLoginIdAsLong());
        return R.ok(purchaseOrderService.create(order));
    }

    @Operation(summary = "提交采购订单")
    @PutMapping("/{id}/submit")
    public R<Void> submit(@PathVariable Long id) {
        purchaseOrderService.submit(id);
        return R.ok();
    }

    @Operation(summary = "取消采购订单")
    @PutMapping("/{id}/cancel")
    public R<Void> cancel(@PathVariable Long id) {
        purchaseOrderService.cancel(id);
        return R.ok();
    }

    @Operation(summary = "导出采购订单")
    @GetMapping("/export")
    public void export(HttpServletResponse response) {
        List<PurchaseOrder> list = purchaseOrderService.listAll();
        List<PurchaseOrderExportVO> voList = list.stream()
                .filter(o -> o.getStatus() != null && o.getStatus() != 3)
                .map(order -> {
            PurchaseOrderExportVO vo = new PurchaseOrderExportVO();
            vo.setOrderNo(order.getOrderNo());
            vo.setSupplierName(order.getSupplierName());
            vo.setWarehouseName(order.getWarehouseName());
            vo.setTotalQuantity(order.getTotalQuantity());
            vo.setTotalAmount(order.getTotalAmount());
            vo.setOperatorName(order.getOperatorName());
            vo.setOrderDate(order.getOrderDate());
            if (order.getCreateTime() != null) vo.setCreateTime(order.getCreateTime());
            String statusText;
            if (order.getStatus() == OrderStatus.DRAFT) {
                statusText = "草稿";
            } else if (order.getStatus() == OrderStatus.CONFIRMED) {
                statusText = "已入库";
            } else if (order.getStatus() == OrderStatus.CANCELED) {
                statusText = "已取消";
            } else {
                statusText = "未知";
            }
            vo.setStatus(statusText);
            return vo;
        }).collect(Collectors.toList());
        ExcelUtil.export(response, voList, "采购订单列表", PurchaseOrderExportVO.class);
    }

    @Operation(summary = "更新草稿采购订单")
    @PutMapping("/{id}/draft")
    public R<Void> updateDraft(@PathVariable Long id, @RequestBody PurchaseOrder order) {
        order.setId(id);
        purchaseOrderService.updateDraft(order);
        return R.ok();
    }

    @SaCheckRole("role_1")
    @Operation(summary = "删除采购订单")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        purchaseOrderService.delete(id);
        return R.ok();
    }

    @SaCheckRole("role_1")
    @Operation(summary = "批量删除采购订单")
    @DeleteMapping("/batch")
    public R<Void> batchDelete(@RequestBody List<Long> ids) {
        purchaseOrderService.batchDelete(ids);
        return R.ok();
    }

    @SaCheckRole("role_1")
    @Operation(summary = "作废采购订单")
    @PutMapping("/{id}/void")
    public R<Void> voidOrder(@PathVariable Long id, @RequestBody Map<String, String> body) {
        purchaseOrderService.voidOrder(id, body.getOrDefault("reason", ""));
        return R.ok();
    }

    @SaCheckRole("role_1")
    @Operation(summary = "批量作废采购订单")
    @PutMapping("/batch-void")
    public R<Void> batchVoid(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Integer> ids = (List<Integer>) body.get("ids");
        String reason = (String) body.getOrDefault("reason", "");
        purchaseOrderService.batchVoid(ids.stream().map(Long::valueOf).collect(Collectors.toList()), reason);
        return R.ok();
    }
}
