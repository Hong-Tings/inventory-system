package com.inventory.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.inventory.product.entity.Product;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ProductMapper extends BaseMapper<Product> {

    @Select("SELECT * FROM product WHERE deleted = 1")
    List<Product> selectDeleted();

    @Select("SELECT COUNT(*) FROM product WHERE code = #{code}")
    int countAllByCode(String code);

    @Select("SELECT code FROM product WHERE code LIKE CONCAT(#{prefix}, '%') ORDER BY code DESC LIMIT 1")
    String selectMaxCodeByPrefix(String prefix);
}
