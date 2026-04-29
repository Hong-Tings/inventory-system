package com.inventory.stocktake.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.inventory.stocktake.entity.StockTake;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface StockTakeMapper extends BaseMapper<StockTake> {
}
