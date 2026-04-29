package com.inventory.report.controller;

import com.inventory.common.result.R;
import com.inventory.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Tag(name = "报表统计")
@RestController
@RequestMapping("/api/v1/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "采购汇总")
    @GetMapping("/purchase-summary")
    public R<Map<String, Object>> purchaseSummary(@RequestParam(defaultValue = "30") int days) {
        return R.ok(reportService.purchaseSummary(days));
    }

    @Operation(summary = "销售汇总")
    @GetMapping("/sales-summary")
    public R<Map<String, Object>> salesSummary(@RequestParam(defaultValue = "30") int days) {
        return R.ok(reportService.salesSummary(days));
    }

    @Operation(summary = "库存周转率")
    @GetMapping("/turnover-rate")
    public R<List<Map<String, Object>>> turnoverRate(@RequestParam(defaultValue = "30") int days) {
        return R.ok(reportService.turnoverRate(days));
    }

    @Operation(summary = "库存预警列表")
    @GetMapping("/inventory-alert")
    public R<List<Map<String, Object>>> inventoryAlert() {
        return R.ok(reportService.inventoryAlert());
    }
}
