package com.inventory.customer.entity;

import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("customer")
public class Customer {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
    private String contact;
    private String phone;
    private String address;
    private Integer status;
    private String remark;

    @TableLogic
    private Integer deleted;

    @ColumnWidth(26)
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @ColumnWidth(26)
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
