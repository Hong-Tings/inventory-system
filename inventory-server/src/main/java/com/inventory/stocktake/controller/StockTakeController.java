package com.inventory.stocktake.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inventory.common.result.PageResult;
import com.inventory.common.result.R;
import com.inventory.common.util.ExcelUtil;
import com.inventory.stocktake.entity.StockTake;
import com.inventory.stocktake.entity.StockTakeItem;
import com.inventory.stocktake.entity.StockTakeDetailExportVO;
import com.inventory.stocktake.entity.StockTakeExportVO;
import com.inventory.stocktake.service.StockTakeService;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.write.metadata.WriteSheet;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Tag(name = "盘点管理")
@RestController
@RequestMapping("/api/v1/stock-take")
@RequiredArgsConstructor
public class StockTakeController {

    private final StockTakeService stockTakeService;

    @Operation(summary = "分页查询盘点单")
    @GetMapping("/page")
    public R<PageResult<StockTake>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String orderNo,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Integer takeType,
            @RequestParam(required = false) Integer minTotalItems,
            @RequestParam(required = false) Integer maxTotalItems,
            @RequestParam(required = false) Integer minDiffItems,
            @RequestParam(required = false) Integer maxDiffItems,
            @RequestParam(required = false) String operatorName,
            @RequestParam(required = false) String approverName,
            @RequestParam(required = false) Integer status) {
        return R.ok(PageResult.of(stockTakeService.page(new Page<>(page, size), orderNo, warehouseId, takeType, minTotalItems, maxTotalItems, minDiffItems, maxDiffItems, operatorName, approverName, status)));
    }

    @Operation(summary = "获取盘点单详情")
    @GetMapping("/{id}")
    public R<StockTake> getById(@PathVariable Long id) {
        return R.ok(stockTakeService.getDetail(id));
    }

    @Operation(summary = "新增盘点单")
    @PostMapping
    public R<Long> create(@RequestBody StockTake stockTake) {
        stockTake.setOperatorId(StpUtil.getLoginIdAsLong());
        return R.ok(stockTakeService.create(stockTake));
    }

    @Operation(summary = "添加盘点明细")
    @PostMapping("/item")
    public R<Void> addItem(@RequestBody StockTakeItem item) {
        stockTakeService.addItem(item);
        return R.ok();
    }

    @Operation(summary = "删除盘点明细")
    @DeleteMapping("/item/{itemId}")
    public R<Void> deleteItem(@PathVariable Long itemId) {
        stockTakeService.deleteItem(itemId);
        return R.ok();
    }

    @Operation(summary = "更新盘点明细（实盘数/原因）")
    @PutMapping("/item/{itemId}")
    public R<Void> updateItem(@PathVariable Long itemId, @RequestBody StockTakeItem item) {
        item.setId(itemId);
        stockTakeService.updateItem(item);
        return R.ok();
    }

    @SaCheckRole("role_1")
    @Operation(summary = "审核盘点单")
    @PutMapping("/{id}/approve")
    public R<Void> approve(@PathVariable Long id) {
        stockTakeService.approve(id, StpUtil.getLoginIdAsLong());
        return R.ok();
    }

    @Operation(summary = "导出盘点单")
    @GetMapping("/export")
    public void export(HttpServletResponse response,
                       @RequestParam(required = false) String ids) {
        List<StockTake> list = stockTakeService.listAll().stream()
                .filter(s -> s.getStatus() != null && s.getStatus() != 3)
                .collect(Collectors.toList());
        if (ids != null && !ids.isEmpty()) {
            List<Long> idList = Arrays.stream(ids.split(",")).map(Long::parseLong).collect(Collectors.toList());
            list = list.stream().filter(s -> idList.contains(s.getId())).collect(Collectors.toList());
        }
        // 按盘点单分组导出，每个单据一个 sheet
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        try {
            String fileName = URLEncoder.encode("盘点明细", "UTF-8").replaceAll("\\+", "%20");
            response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");
            var excelWriter = EasyExcel.write(response.getOutputStream(), StockTakeDetailExportVO.class).build();
            for (StockTake s : list) {
                List<StockTakeDetailExportVO> sheetData = stockTakeService.getExportDetailList(List.of(s));
                WriteSheet writeSheet = EasyExcel.writerSheet(s.getOrderNo()).build();
                excelWriter.write(sheetData, writeSheet);
            }
            excelWriter.finish();
        } catch (IOException e) {
            throw new RuntimeException("导出失败", e);
        }
    }

    @Operation(summary = "盘点调整")
    @PutMapping("/{id}/adjust")
    public R<Void> adjust(@PathVariable Long id) {
        stockTakeService.adjust(id);
        return R.ok();
    }

    @SaCheckRole("role_1")
    @Operation(summary = "删除盘点单")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        stockTakeService.delete(id);
        return R.ok();
    }

    @SaCheckRole("role_1")
    @Operation(summary = "批量删除盘点单")
    @DeleteMapping("/batch")
    public R<Void> batchDelete(@RequestBody List<Long> ids) {
        stockTakeService.batchDelete(ids);
        return R.ok();
    }

    @SaCheckRole("role_1")
    @Operation(summary = "作废盘点单")
    @PutMapping("/{id}/void")
    public R<Void> voidOrder(@PathVariable Long id, @RequestBody Map<String, String> body) {
        stockTakeService.voidOrder(id, body.getOrDefault("reason", ""));
        return R.ok();
    }

    @SaCheckRole("role_1")
    @Operation(summary = "批量作废盘点单")
    @PutMapping("/batch-void")
    public R<Void> batchVoid(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Integer> ids = (List<Integer>) body.get("ids");
        String reason = (String) body.getOrDefault("reason", "");
        stockTakeService.batchVoid(ids.stream().map(Long::valueOf).collect(Collectors.toList()), reason);
        return R.ok();
    }
}
