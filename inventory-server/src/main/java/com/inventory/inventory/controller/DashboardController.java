package com.inventory.inventory.controller;

import com.inventory.common.result.R;
import com.inventory.inventory.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "仪表盘")
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "获取仪表盘统计数据")
    @GetMapping("/stats")
    public R<Map<String, Object>> stats() {
        return R.ok(dashboardService.stats());
    }
}
