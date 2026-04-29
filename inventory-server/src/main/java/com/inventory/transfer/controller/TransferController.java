package com.inventory.transfer.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inventory.common.result.PageResult;
import com.inventory.common.result.R;
import com.inventory.common.util.ExcelUtil;
import com.inventory.transfer.entity.InventoryTransfer;
import com.inventory.transfer.entity.TransferExportVO;
import com.inventory.transfer.service.TransferService;
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

@Tag(name = "库存调拨管理")
@RestController
@RequestMapping("/api/v1/transfer")
@RequiredArgsConstructor
public class TransferController {

    private final TransferService transferService;

    @Operation(summary = "分页查询调拨单")
    @GetMapping("/page")
    public R<PageResult<InventoryTransfer>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Long fromWh,
            @RequestParam(required = false) Long toWh,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer minQuantity,
            @RequestParam(required = false) Integer maxQuantity,
            @RequestParam(required = false) String operatorName,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate endDate) {
        return R.ok(PageResult.of(transferService.page(new Page<>(page, size), orderNo, fromWh, toWh, status, minQuantity, maxQuantity, operatorName, startDate, endDate)));
    }

    @Operation(summary = "获取调拨单详情")
    @GetMapping("/{id}")
    public R<InventoryTransfer> getById(@PathVariable Long id) {
        return R.ok(transferService.getDetail(id));
    }

    @Operation(summary = "新增调拨单")
    @PostMapping
    public R<Long> create(@RequestBody InventoryTransfer transfer) {
        // 兼容前端字段名 outWarehouseId -> fromWarehouseId, inWarehouseId -> toWarehouseId
        if (transfer.getFromWarehouseId() == null && transfer.getOutWarehouseId() != null) {
            transfer.setFromWarehouseId(transfer.getOutWarehouseId());
        }
        if (transfer.getToWarehouseId() == null && transfer.getInWarehouseId() != null) {
            transfer.setToWarehouseId(transfer.getInWarehouseId());
        }
        transfer.setOperatorId(StpUtil.getLoginIdAsLong());
        return R.ok(transferService.create(transfer));
    }

    @Operation(summary = "更新草稿调拨单")
    @PutMapping("/{id}/draft")
    public R<Void> updateDraft(@PathVariable Long id, @RequestBody InventoryTransfer transfer) {
        transfer.setId(id);
        // 兼容前端字段名
        if (transfer.getFromWarehouseId() == null && transfer.getOutWarehouseId() != null) {
            transfer.setFromWarehouseId(transfer.getOutWarehouseId());
        }
        if (transfer.getToWarehouseId() == null && transfer.getInWarehouseId() != null) {
            transfer.setToWarehouseId(transfer.getInWarehouseId());
        }
        transferService.updateDraft(transfer);
        return R.ok();
    }

    @Operation(summary = "提交调拨单")
    @PutMapping("/{id}/submit")
    public R<Void> submit(@PathVariable Long id) {
        transferService.submit(id);
        return R.ok();
    }

    @Operation(summary = "取消调拨单")
    @PutMapping("/{id}/cancel")
    public R<Void> cancel(@PathVariable Long id) {
        transferService.cancel(id);
        return R.ok();
    }

    @SaCheckRole("role_1")
    @Operation(summary = "删除调拨单")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        transferService.delete(id);
        return R.ok();
    }

    @SaCheckRole("role_1")
    @Operation(summary = "批量删除调拨单")
    @DeleteMapping("/batch")
    public R<Void> batchDelete(@RequestBody List<Long> ids) {
        transferService.batchDelete(ids);
        return R.ok();
    }

    @SaCheckRole("role_1")
    @Operation(summary = "作废调拨单")
    @PutMapping("/{id}/void")
    public R<Void> voidOrder(@PathVariable Long id, @RequestBody Map<String, String> body) {
        transferService.voidOrder(id, body.getOrDefault("reason", ""));
        return R.ok();
    }

    @SaCheckRole("role_1")
    @Operation(summary = "批量作废调拨单")
    @PutMapping("/batch-void")
    public R<Void> batchVoid(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Integer> ids = (List<Integer>) body.get("ids");
        String reason = (String) body.getOrDefault("reason", "");
        transferService.batchVoid(ids.stream().map(Long::valueOf).collect(Collectors.toList()), reason);
        return R.ok();
    }

    @Operation(summary = "导出调拨单")
    @GetMapping("/export")
    public void export(HttpServletResponse response) {
        List<InventoryTransfer> list = transferService.listAll().stream()
                .filter(t -> t.getStatus() != null && t.getStatus() != 3)
                .collect(Collectors.toList());
        List<TransferExportVO> voList = list.stream().map(s -> {
            TransferExportVO vo = new TransferExportVO();
            vo.setOrderNo(s.getOrderNo());
            vo.setFromWarehouseName(s.getFromWarehouseName());
            vo.setToWarehouseName(s.getToWarehouseName());
            vo.setTotalQuantity(s.getTotalQuantity());
            if (s.getStatus() != null) {
                switch (s.getStatus()) {
                    case 0: vo.setStatus("草稿"); break;
                    case 1: vo.setStatus("已完成"); break;
                    case 2: vo.setStatus("已取消"); break;
                    default: vo.setStatus("未知"); break;
                }
            }
            if (s.getOrderDate() != null) vo.setOrderDate(s.getOrderDate());
            if (s.getCreateTime() != null) vo.setCreateTime(s.getCreateTime());
            if (s.getUpdateTime() != null) vo.setUpdateTime(s.getUpdateTime());
            return vo;
        }).collect(Collectors.toList());
        ExcelUtil.export(response, voList, "调拨单列表", TransferExportVO.class);
    }
}
