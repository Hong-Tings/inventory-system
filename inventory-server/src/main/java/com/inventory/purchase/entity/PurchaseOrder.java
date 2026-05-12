package com.inventory.purchase.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("purchase_order")
public class PurchaseOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long supplierId;
    private Long warehouseId;
    private Long locationId;
    private BigDecimal totalAmount;
    private Integer totalQuantity;
    private Integer status;
    private Long operatorId;
    private Long approverId;
    private LocalDateTime approveTime;
    private LocalDate orderDate;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String supplierName;

    @TableField(exist = false)
    private String warehouseName;

    @TableField(exist = false)
    private String locationName;

    @TableField(exist = false)
    private String operatorName;

    @TableField(exist = false)
    private String approverName;

    @TableField(exist = false)
    private List<PurchaseOrderItem> items;
}
