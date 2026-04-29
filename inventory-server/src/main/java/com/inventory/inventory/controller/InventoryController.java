package com.inventory.inventory.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inventory.common.result.PageResult;
import com.inventory.common.result.R;
import com.inventory.common.util.ExcelUtil;
import com.inventory.inventory.entity.Inventory;
import com.inventory.inventory.entity.InventoryExportVO;
import com.inventory.inventory.entity.InventoryLog;
import com.inventory.inventory.entity.InventoryLogExportVO;
import com.inventory.inventory.service.InventoryLogService;
import com.inventory.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "库存管理")
@RestController
@RequestMapping("/api/v1/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;
    private final InventoryLogService inventoryLogService;

    @Operation(summary = "分页查询库存")
    @GetMapping("/page")
    public R<PageResult<Inventory>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) Long warehouseId) {
        return R.ok(PageResult.of(inventoryService.page(new Page<>(page, size), productId, productName, warehouseId)));
    }

    @Operation(summary = "分页查询库存流水")
    @GetMapping("/log/page")
    public R<PageResult<InventoryLog>> logPage(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) String productName,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) String changeType,
            @RequestParam(required = false) String refOrderNo,
            @RequestParam(required = false) String operatorName,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        LocalDateTime start = startDate != null && !startDate.isEmpty() ? LocalDateTime.parse(startDate + "T00:00:00") : null;
        LocalDateTime end = endDate != null && !endDate.isEmpty() ? LocalDateTime.parse(endDate + "T23:59:59") : null;
        return R.ok(PageResult.of(inventoryLogService.page(new Page<>(page, size), productId, productName, warehouseId, changeType, refOrderNo, operatorName, start, end)));
    }

    @Operation(summary = "导出库存")
    @GetMapping("/export")
    public void export(HttpServletResponse response) {
        List<Inventory> list = inventoryService.listAll();
        List<InventoryExportVO> voList = list.stream().map(inv -> {
            InventoryExportVO vo = new InventoryExportVO();
            vo.setProductCode(inv.getProductCode());
            vo.setProductName(inv.getProductName());
            vo.setWarehouseName(inv.getWarehouseName());
            vo.setQuantity(inv.getQuantity());
            return vo;
        }).collect(Collectors.toList());
        ExcelUtil.export(response, voList, "库存列表", InventoryExportVO.class);
    }

    @Operation(summary = "导出库存流水")
    @GetMapping("/log/export")
    public void exportLog(HttpServletResponse response) {
        List<InventoryLog> list = inventoryLogService.listAll();
        List<InventoryLogExportVO> voList = list.stream().map(log -> {
            InventoryLogExportVO vo = new InventoryLogExportVO();
            vo.setProductName(log.getProductName());
            vo.setWarehouseName(log.getWarehouseName());
            // 操作类型转中文
            String type = log.getChangeType();
            if (type != null) {
                switch (type) {
                    case "PURCHASE_IN": type = "采购入库"; break;
                    case "SALES_OUT": type = "销售出库"; break;
                    case "TRANSFER_IN": type = "调拨入库"; break;
                    case "TRANSFER_OUT": type = "调拨出库"; break;
                    case "PURCHASE_CANCEL": type = "取消入库"; break;
                    case "SALES_CANCEL": type = "取消出库"; break;
                    case "STOCKTAKE": type = "盘点调整"; break;
                }
            }
            vo.setChangeType(type);
            vo.setChangeQty(log.getChangeQty());
            vo.setBeforeQty(log.getBeforeQty());
            vo.setAfterQty(log.getAfterQty());
            vo.setRefOrderNo(log.getRefOrderNo());
            vo.setOperatorName(log.getOperatorName());
            if (log.getCreateTime() != null) vo.setCreateTime(log.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
        ExcelUtil.export(response, voList, "库存流水", InventoryLogExportVO.class);
    }
}
