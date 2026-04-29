package com.inventory.sales.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@TableName("sales_order_item")
public class SalesOrderItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long productId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    private String batchNo;
    private String remark;

    @TableField(exist = false)
    private String productName;

    @TableField(exist = false)
    private String productCode;
}
