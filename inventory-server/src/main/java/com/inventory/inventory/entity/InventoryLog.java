package com.inventory.inventory.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("inventory_log")
public class InventoryLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long productId;
    private Long warehouseId;
    private Long locationId;
    private String batchNo;
    private String changeType;
    private Integer changeQty;
    private Integer beforeQty;
    private Integer afterQty;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    private String refOrderNo;
    private Long operatorId;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(exist = false)
    private String productName;

    @TableField(exist = false)
    private String warehouseName;

    @TableField(exist = false)
    private String operatorName;
}
