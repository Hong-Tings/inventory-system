package com.inventory.supplier.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inventory.common.result.PageResult;
import com.inventory.common.result.R;
import com.inventory.common.util.ExcelUtil;
import com.inventory.supplier.entity.Supplier;
import com.inventory.supplier.entity.SupplierExportVO;
import com.inventory.supplier.service.SupplierService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "供应商管理")
@RestController
@RequestMapping("/api/v1/supplier")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;

    @Operation(summary = "分页查询供应商")
    @GetMapping("/page")
    public R<PageResult<Supplier>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String contact,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String address) {
        return R.ok(PageResult.of(supplierService.page(new Page<>(page, size), name, contact, phone, address)));
    }

    @Operation(summary = "导出供应商")
    @GetMapping("/export")
    public void export(HttpServletResponse response) {
        List<Supplier> list = supplierService.listAll();
        List<SupplierExportVO> voList = list.stream().map(s -> {
            SupplierExportVO vo = new SupplierExportVO();
            vo.setCode(s.getCode()); vo.setName(s.getName()); vo.setContact(s.getContact());
            vo.setPhone(s.getPhone()); vo.setAddress(s.getAddress());
            vo.setStatus(s.getStatus() != null && s.getStatus() == 1 ? "启用" : "停用");
            if (s.getCreateTime() != null) vo.setCreateTime(s.getCreateTime());
            if (s.getUpdateTime() != null) vo.setUpdateTime(s.getUpdateTime());
            return vo;
        }).collect(Collectors.toList());
        ExcelUtil.export(response, voList, "供应商列表", SupplierExportVO.class);
    }

    @Operation(summary = "获取供应商详情")
    @GetMapping("/{id}")
    public R<Supplier> getById(@PathVariable Long id) {
        return R.ok(supplierService.getById(id));
    }

    @Operation(summary = "查询所有启用的供应商")
    @GetMapping("/list")
    public R<List<Supplier>> list() {
        return R.ok(supplierService.listAll());
    }

    @SaCheckRole("role_1")
    @Operation(summary = "新增供应商")
    @PostMapping
    public R<Void> create(@RequestBody Supplier supplier) {
        supplierService.save(supplier);
        return R.ok();
    }

    @SaCheckRole("role_1")
    @Operation(summary = "更新供应商")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody Supplier supplier) {
        supplier.setId(id);
        supplierService.update(supplier);
        return R.ok();
    }

    @SaCheckRole("role_1")
    @Operation(summary = "删除供应商")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        supplierService.delete(id);
        return R.ok();
    }

    @SaCheckRole("role_1")
    @Operation(summary = "恢复已删除供应商")
    @PutMapping("/{id}/restore")
    public R<Void> restore(@PathVariable Long id) {
        supplierService.restore(id);
        return R.ok();
    }
}
