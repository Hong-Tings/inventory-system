package com.inventory.product.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.inventory.common.exception.BusinessException;
import com.inventory.product.entity.Product;
import com.inventory.product.entity.ProductCategory;
import com.inventory.product.mapper.ProductCategoryMapper;
import com.inventory.product.mapper.ProductMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductCategoryService {

    private final ProductCategoryMapper productCategoryMapper;
    private final ProductMapper productMapper;

    public ProductCategoryService(ProductCategoryMapper productCategoryMapper, ProductMapper productMapper) {
        this.productCategoryMapper = productCategoryMapper;
        this.productMapper = productMapper;
    }

    public List<ProductCategory> tree() {
        List<ProductCategory> all = productCategoryMapper.selectList(
                new LambdaQueryWrapper<ProductCategory>().orderByAsc(ProductCategory::getSort));
        return buildTree(all, null);
    }

    private List<ProductCategory> buildTree(List<ProductCategory> all, Long parentId) {
        List<ProductCategory> children = all.stream()
                .filter(c -> {
                    if (parentId == null) return c.getParentId() == null;
                    return parentId.equals(c.getParentId());
                })
                .collect(Collectors.toList());
        for (ProductCategory category : children) {
            List<ProductCategory> subs = buildTree(all, category.getId());
            category.setChildren(subs.isEmpty() ? new ArrayList<>() : subs);
        }
        return children;
    }

    @Transactional(rollbackFor = Exception.class)
    public void save(ProductCategory category) {
        productCategoryMapper.insert(category);
    }

    @Transactional(rollbackFor = Exception.class)
    public void update(ProductCategory category) {
        productCategoryMapper.updateById(category);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        long childCount = productCategoryMapper.selectCount(
                new LambdaQueryWrapper<ProductCategory>().eq(ProductCategory::getParentId, id));
        if (childCount > 0) {
            throw new RuntimeException("存在子分类，无法删除");
        }
        long productCount = productMapper.selectCount(
                new LambdaQueryWrapper<Product>().eq(Product::getCategoryId, id));
        if (productCount > 0) {
            throw new BusinessException("该分类下有商品，无法删除");
        }
        productCategoryMapper.deleteById(id);
    }

    @Transactional(rollbackFor = Exception.class)
    public void restore(Long id) {
        ProductCategory c = productCategoryMapper.selectById(id);
        if (c == null) throw new BusinessException("分类不存在");
        c.setDeleted(0);
        productCategoryMapper.updateById(c);
    }
}
