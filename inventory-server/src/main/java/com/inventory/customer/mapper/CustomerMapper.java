package com.inventory.customer.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.inventory.customer.entity.Customer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CustomerMapper extends BaseMapper<Customer> {

    @Select("SELECT * FROM customer WHERE deleted = 1")
    List<Customer> selectDeleted();

    @Select("SELECT COUNT(*) FROM customer WHERE code = #{code}")
    int countAllByCode(String code);

    @Select("SELECT code FROM customer WHERE code LIKE CONCAT(#{prefix}, '%') ORDER BY code DESC LIMIT 1")
    String selectMaxCodeByPrefix(String prefix);

    @Select("SELECT * FROM customer WHERE id = #{id}")
    Customer selectAnyById(Long id);
}
