package com.inventory.warehouse.entity;

import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("warehouse")
public class Warehouse {
    @TableId(type = IdType.AUTO) private Long id;
    private String code; private String name; private String contact;
    private String phone; private String address;
    private Integer status; private String remark;
    @TableLogic private Integer deleted;
    @ColumnWidth(26) @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
    @ColumnWidth(26) @TableField(fill = FieldFill.INSERT_UPDATE) private LocalDateTime updateTime;

    @TableField(exist = false)
    private Integer productCount;

    @TableField(exist = false)
    private java.math.BigDecimal totalAmount;
}
