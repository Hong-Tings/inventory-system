package com.inventory.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("product")
public class Product {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
    private Long categoryId;
    private String spec;
    private String unit;
    private BigDecimal purchasePrice;
    private BigDecimal salePrice;
    private String imageUrl;
    private Integer minStock;
    private Integer maxStock;
    private Integer status;
    private String remark;

    @TableLogic
    private Integer deleted;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String categoryName;

    @TableField(exist = false)
    private Integer inventoryQuantity;

    @TableField(exist = false)
    private String alertStatus;
}
