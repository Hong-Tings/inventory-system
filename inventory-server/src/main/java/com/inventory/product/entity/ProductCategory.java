package com.inventory.product.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("product_category")
public class ProductCategory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Long parentId;
    private Integer sort;
    private Integer status;

    @TableLogic
    private Integer deleted;

    @TableField(exist = false)
    private List<ProductCategory> children;
}
