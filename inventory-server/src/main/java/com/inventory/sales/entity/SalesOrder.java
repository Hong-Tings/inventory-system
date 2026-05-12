package com.inventory.sales.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("sales_order")
public class SalesOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long customerId;
    private Long warehouseId;
    private BigDecimal totalAmount;
    private Integer totalQuantity;
    private String salesman;
    private String externalOrderNo;
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
    private String customerName;

    @TableField(exist = false)
    private String warehouseName;

    @TableField(exist = false)
    private String operatorName;

    @TableField(exist = false)
    private String approverName;

    @TableField(exist = false)
    private List<SalesOrderItem> items;
}
