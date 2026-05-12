package com.inventory.warehouse.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inventory.common.result.PageResult;
import com.inventory.common.result.R;
import com.inventory.common.util.ExcelUtil;
import com.inventory.warehouse.entity.Warehouse;
import com.inventory.warehouse.entity.WarehouseExportVO;
import com.inventory.warehouse.service.WarehouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "仓库管理")
@RestController
@RequestMapping("/api/v1/warehouse")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @Operation(summary = "获取仓库树形结构")
    @GetMapping("/tree")
    public R<List<Warehouse>> tree() {
        return R.ok(warehouseService.tree());
    }

    @Operation(summary = "获取子级仓库列表")
    @GetMapping("/children/{parentId}")
    public R<List<Warehouse>> children(@PathVariable Long parentId) {
        return R.ok(warehouseService.children(parentId));
    }

    @Operation(summary = "搜索仓库")
    @GetMapping("/search")
    public R<List<Warehouse>> search(@RequestParam String keyword) {
        return R.ok(warehouseService.search(keyword));
    }

    @Operation(summary = "查询所有启用的仓库")
    @GetMapping("/list")
    public R<List<Warehouse>> list() {
        return R.ok(warehouseService.listAll());
    }

    @Operation(summary = "分页查询仓库")
    @GetMapping("/page")
    public R<PageResult<Warehouse>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String contact,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer level,
            @RequestParam(required = false) Long parentId) {
        return R.ok(PageResult.of(warehouseService.page(new Page<>(page, size), name, contact, phone, address, status, level, parentId)));
    }

    @Operation(summary = "获取仓库详情")
    @GetMapping("/{id}")
    public R<Warehouse> getById(@PathVariable Long id) {
        return R.ok(warehouseService.getById(id));
    }

    @Operation(summary = "新增仓库")
    @PostMapping
    public R<Void> create(@RequestBody Warehouse warehouse) {
        warehouseService.save(warehouse);
        return R.ok();
    }

    @Operation(summary = "更新仓库")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody Warehouse warehouse) {
        warehouse.setId(id);
        warehouseService.update(warehouse);
        return R.ok();
    }

    @SaCheckRole("role_1")
    @Operation(summary = "删除仓库")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        warehouseService.delete(id);
        return R.ok();
    }

    @Operation(summary = "导出仓库")
    @GetMapping("/export")
    public void export(HttpServletResponse response) {
        List<Warehouse> list = warehouseService.listAll();
        List<WarehouseExportVO> voList = list.stream().map(s -> {
            WarehouseExportVO vo = new WarehouseExportVO();
            vo.setCode(s.getCode()); vo.setName(s.getName()); vo.setContact(s.getContact());
            vo.setPhone(s.getPhone()); vo.setAddress(s.getAddress());
            vo.setStatus(s.getStatus() != null && s.getStatus() == 1 ? "启用" : "停用");
            if (s.getProductCount() != null) vo.setProductCount(s.getProductCount());
            if (s.getTotalAmount() != null) vo.setTotalAmount(s.getTotalAmount());
            if (s.getCreateTime() != null) vo.setCreateTime(s.getCreateTime());
            if (s.getUpdateTime() != null) vo.setUpdateTime(s.getUpdateTime());
            return vo;
        }).collect(Collectors.toList());
        ExcelUtil.export(response, voList, "仓库列表", WarehouseExportVO.class);
    }

    @SaCheckRole("role_1")
    @Operation(summary = "恢复已删除仓库")
    @PutMapping("/{id}/restore")
    public R<Void> restore(@PathVariable Long id) {
        warehouseService.restore(id);
        return R.ok();
    }
}
