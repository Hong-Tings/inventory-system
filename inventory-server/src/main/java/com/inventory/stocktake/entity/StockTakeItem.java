package com.inventory.stocktake.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("stock_take_item")
public class StockTakeItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long stockTakeId;
    private Long productId;
    private Long locationId;
    private String batchNo;
    private Integer bookQty;
    private Integer actualQty;
    private Integer diffQty;
    private String diffReason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(exist = false)
    private String productName;

    @TableField(exist = false)
    private String productCode;

    @TableField(exist = false)
    private String locationName;
}
