package com.inventory.purchase.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@TableName("purchase_order_item")
public class PurchaseOrderItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long productId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    private String batchNo;
    private LocalDate productionDate;
    private LocalDate expiryDate;
    private String remark;

    @TableField(exist = false)
    private String productName;

    @TableField(exist = false)
    private String productCode;
}
