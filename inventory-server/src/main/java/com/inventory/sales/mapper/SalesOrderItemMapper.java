package com.inventory.sales.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.inventory.sales.entity.SalesOrderItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SalesOrderItemMapper extends BaseMapper<SalesOrderItem> {
}
