package com.inventory.inventory.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
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
