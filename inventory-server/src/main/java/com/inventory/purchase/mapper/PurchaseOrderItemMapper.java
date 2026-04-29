package com.inventory.purchase.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.inventory.purchase.entity.PurchaseOrderItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PurchaseOrderItemMapper extends BaseMapper<PurchaseOrderItem> {
}
