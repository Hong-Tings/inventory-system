package com.inventory.stocktake.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.inventory.stocktake.entity.StockTakeItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StockTakeItemMapper extends BaseMapper<StockTakeItem> {
}
