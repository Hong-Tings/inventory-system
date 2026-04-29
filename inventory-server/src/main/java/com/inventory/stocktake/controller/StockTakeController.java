package com.inventory.stocktake.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inventory.common.result.PageResult;
import com.inventory.common.result.R;
import com.inventory.common.util.ExcelUtil;
import com.inventory.stocktake.entity.StockTake;
import com.inventory.stocktake.entity.StockTakeItem;
import com.inventory.stocktake.entity.StockTakeExportVO;
import com.inventory.stocktake.service.StockTakeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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

    @Operation(summary = "审核盘点单")
    @PutMapping("/{id}/approve")
    public R<Void> approve(@PathVariable Long id) {
        stockTakeService.approve(id, StpUtil.getLoginIdAsLong());
        return R.ok();
    }

    @Operation(summary = "导出盘点单")
    @GetMapping("/export")
    public void export(HttpServletResponse response) {
        List<StockTake> list = stockTakeService.listAll().stream()
                .filter(s -> s.getStatus() != null && s.getStatus() != 3)
                .collect(Collectors.toList());
        List<StockTakeExportVO> voList = list.stream().map(s -> {
            StockTakeExportVO vo = new StockTakeExportVO();
            vo.setOrderNo(s.getOrderNo());
            vo.setWarehouseName(s.getWarehouseName());
            vo.setTakeType(s.getTakeType() != null && s.getTakeType() == 0 ? "全盘" : "抽盘");
            vo.setTotalItems(s.getTotalItems());
            vo.setDiffItems(s.getDiffItems());
            if (s.getStatus() != null) {
                switch (s.getStatus()) {
                    case 0: vo.setStatus("盘点中"); break;
                    case 1: vo.setStatus("已审核"); break;
                    case 2: vo.setStatus("已调整"); break;
                    default: vo.setStatus("未知"); break;
                }
            }
            if (s.getOrderDate() != null) vo.setOrderDate(s.getOrderDate());
            if (s.getCreateTime() != null) vo.setCreateTime(s.getCreateTime());
            if (s.getUpdateTime() != null) vo.setUpdateTime(s.getUpdateTime());
            return vo;
        }).collect(Collectors.toList());
        ExcelUtil.export(response, voList, "盘点单列表", StockTakeExportVO.class);
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
