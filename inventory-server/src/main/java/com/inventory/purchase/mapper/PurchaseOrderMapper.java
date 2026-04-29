package com.inventory.purchase.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.inventory.purchase.entity.PurchaseOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PurchaseOrderMapper extends BaseMapper<PurchaseOrder> {
}
