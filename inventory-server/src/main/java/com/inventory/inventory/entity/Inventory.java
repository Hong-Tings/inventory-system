package com.inventory.inventory.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("inventory")
public class Inventory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long productId;
    private Long warehouseId;
    private Long locationId;
    private String batchNo;
    private Integer quantity;
    private Integer lockedQty;
    /** 加权平均成本价（移动加权平均法：总金额÷总数量） */
    private BigDecimal costPrice;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String productName;

    @TableField(exist = false)
    private String productCode;

    @TableField(exist = false)
    private String warehouseName;

    @TableField(exist = false)
    private String locationName;
}
