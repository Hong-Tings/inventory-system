package com.inventory.product.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.inventory.common.result.PageResult;
import com.inventory.common.result.R;
import com.inventory.common.util.ExcelUtil;
import com.inventory.product.entity.Product;
import com.inventory.product.entity.ProductExportVO;
import com.inventory.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "商品管理")
@RestController
@RequestMapping("/api/v1/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "分页查询商品")
    @GetMapping("/page")
    public R<PageResult<Product>> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Boolean alertOnly,
            @RequestParam(required = false) Long warehouseId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) BigDecimal minSalePrice,
            @RequestParam(required = false) BigDecimal maxSalePrice,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return R.ok(PageResult.of(productService.page(new Page<>(page, size), name, code, status, alertOnly, warehouseId, categoryId, minPrice, maxPrice, minSalePrice, maxSalePrice, startDate, endDate)));
    }

    @Operation(summary = "查询所有启用的商品")
    @GetMapping("/list")
    public R<List<Product>> list() {
        return R.ok(productService.list());
    }

    @Operation(summary = "根据ID获取商品")
    @GetMapping("/{id}")
    public R<Product> getById(@PathVariable Long id) {
        Product product = productService.getById(id);
        return R.ok(product);
    }

    @Operation(summary = "新增商品")
    @PostMapping
    public R<Void> create(@RequestBody Product product) {
        productService.save(product);
        return R.ok();
    }

    @Operation(summary = "更新商品")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody Product product) {
        product.setId(id);
        productService.update(product);
        return R.ok();
    }

    @Operation(summary = "删除商品")
    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        productService.delete(id);
        return R.ok();
    }

    @Operation(summary = "恢复已删除商品")
    @PutMapping("/{id}/restore")
    public R<Void> restore(@PathVariable Long id) {
        productService.restore(id);
        return R.ok();
    }

    @Operation(summary = "导出商品")
    @GetMapping("/export")
    public void export(HttpServletResponse response) {
        List<Product> list = productService.listAll();
        List<ProductExportVO> voList = list.stream().map(p -> {
            ProductExportVO vo = new ProductExportVO();
            vo.setCode(p.getCode());
            vo.setName(p.getName());
            vo.setSpec(p.getSpec());
            vo.setUnit(p.getUnit());
            vo.setPurchasePrice(p.getPurchasePrice());
            vo.setSalePrice(p.getSalePrice());
            vo.setMinStock(p.getMinStock());
            vo.setInventoryQuantity(p.getInventoryQuantity());
            vo.setAlertStatus(p.getAlertStatus());
            vo.setCategoryName(p.getCategoryName());
            vo.setCreateTime(p.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
        ExcelUtil.export(response, voList, "商品列表", ProductExportVO.class);
    }

}
