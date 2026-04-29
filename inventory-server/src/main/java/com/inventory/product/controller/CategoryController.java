package com.inventory.product.controller;

import com.inventory.common.result.R;
import com.inventory.product.entity.ProductCategory;
import com.inventory.product.service.ProductCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "分类管理")
@RestController
@RequestMapping("/api/v1/category")
@RequiredArgsConstructor
public class CategoryController {

    private final ProductCategoryService categoryService;

    @Operation(summary = "获取分类树")
    @GetMapping("/tree")
    public R<List<ProductCategory>> tree() {
        return R.ok(categoryService.tree());
    }

    @Operation(summary = "新增分类")
    @PostMapping
    public R<Void> create(@RequestBody ProductCategory category) {
        categoryService.save(category);
        return R.ok();
    }

    @Operation(summary = "更新分类")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody ProductCategory category) {
        category.setId(id);
        categoryService.update(category);
        return R.ok();
    }

    @Operation(summary = "删除分类")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return R.ok();
    }

    @Operation(summary = "恢复已删除分类")
    @PutMapping("/{id}/restore")
    public R<Void> restore(@PathVariable Long id) {
        categoryService.restore(id);
        return R.ok();
    }
}
