package com.inventory.sales.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.inventory.sales.entity.SalesOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SalesOrderMapper extends BaseMapper<SalesOrder> {
}
