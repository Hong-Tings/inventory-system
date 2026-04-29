package com.inventory.supplier.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.inventory.supplier.entity.Supplier;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SupplierMapper extends BaseMapper<Supplier> {

    @Select("SELECT * FROM supplier WHERE deleted = 1")
    List<Supplier> selectDeleted();

    @Select("SELECT COUNT(*) FROM supplier WHERE code = #{code}")
    int countAllByCode(String code);

    @Select("SELECT code FROM supplier WHERE code LIKE CONCAT(#{prefix}, '%') ORDER BY code DESC LIMIT 1")
    String selectMaxCodeByPrefix(String prefix);

    @Select("SELECT * FROM supplier WHERE id = #{id}")
    Supplier selectAnyById(Long id);
}
