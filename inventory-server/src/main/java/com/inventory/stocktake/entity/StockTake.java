package com.inventory.stocktake.entity;

import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("stock_take")
public class StockTake {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long warehouseId;
    @TableField(exist = false)
    private Long locationId;
    private Integer takeType;
    private Integer totalItems;
    private Integer diffItems;
    private Integer status;
    private Long operatorId;
    private Long approverId;
    @ColumnWidth(26)
    private LocalDate orderDate;
    private String remark;

    @ColumnWidth(26)
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @ColumnWidth(26)
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String warehouseName;

    @TableField(exist = false)
    private String locationName;

    @TableField(exist = false)
    private String operatorName;

    @TableField(exist = false)
    private String approverName;

    @TableField(exist = false)
    private List<StockTakeItem> items;
}
