package com.inventory.transfer.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inventory.common.result.PageResult;
import com.inventory.common.result.R;
import com.inventory.common.util.ExcelUtil;
import com.inventory.transfer.entity.InventoryTransfer;
import com.inventory.transfer.entity.TransferDetailExportVO;
import com.inventory.transfer.entity.TransferExportVO;
import com.inventory.transfer.service.TransferService;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.metadata.WriteSheet;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Arrays;
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

    @SaCheckRole("role_1")
    @Operation(summary = "审核通过调拨单")
    @PutMapping("/{id}/approve")
    public R<Void> approve(@PathVariable Long id) {
        transferService.approve(id);
        return R.ok();
    }

    @SaCheckRole("role_1")
    @Operation(summary = "驳回调拨单")
    @PutMapping("/{id}/reject")
    public R<Void> reject(@PathVariable Long id, @RequestBody Map<String, String> body) {
        transferService.reject(id, body.getOrDefault("reason", ""));
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
    public void export(HttpServletResponse response,
                       @RequestParam(required = false) String ids) {
        List<InventoryTransfer> list = transferService.listAll().stream()
                .filter(t -> t.getStatus() != null && t.getStatus() != 3)
                .collect(Collectors.toList());
        if (ids != null && !ids.isEmpty()) {
            List<Long> idList = Arrays.stream(ids.split(",")).map(Long::parseLong).collect(Collectors.toList());
            list = list.stream().filter(t -> idList.contains(t.getId())).collect(Collectors.toList());
        }
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        try {
            String fileName = URLEncoder.encode("库存调拨", "UTF-8").replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
            var excelWriter = EasyExcel.write(response.getOutputStream(), TransferDetailExportVO.class).build();
            for (InventoryTransfer s : list) {
                List<TransferDetailExportVO> sheetData = transferService.getExportDetailList(List.of(s));
                WriteSheet writeSheet = EasyExcel.writerSheet(s.getOrderNo()).build();
                excelWriter.write(sheetData, writeSheet);
            }
            excelWriter.finish();
        } catch (IOException e) {
            throw new RuntimeException("导出失败", e);
        }
        // grouped by sheet above
    }
}
