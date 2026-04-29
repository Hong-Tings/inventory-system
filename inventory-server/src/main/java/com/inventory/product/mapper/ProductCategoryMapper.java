package com.inventory.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.inventory.product.entity.ProductCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProductCategoryMapper extends BaseMapper<ProductCategory> {

    @Select("SELECT * FROM product_category WHERE deleted = 0 ORDER BY sort")
    List<ProductCategory> selectTree();

    @Select("SELECT * FROM product_category WHERE deleted = 1")
    List<ProductCategory> selectDeleted();
}
