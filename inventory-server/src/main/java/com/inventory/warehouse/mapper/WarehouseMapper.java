package com.inventory.warehouse.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.inventory.warehouse.entity.Warehouse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WarehouseMapper extends BaseMapper<Warehouse> {

    @Select("SELECT * FROM warehouse WHERE deleted = 1")
    List<Warehouse> selectDeleted();

    @Select("SELECT COUNT(*) FROM warehouse WHERE code = #{code}")
    int countAllByCode(String code);

    @Select("SELECT code FROM warehouse WHERE code LIKE CONCAT(#{prefix}, '%') ORDER BY code DESC LIMIT 1")
    String selectMaxCodeByPrefix(String prefix);

    @Select("SELECT * FROM warehouse WHERE id = #{id}")
    Warehouse selectAnyById(Long id);
}
