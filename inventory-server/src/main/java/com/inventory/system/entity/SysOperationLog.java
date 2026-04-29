package com.inventory.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_operation_log")
public class SysOperationLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long operatorId;
    private String operator;
    private String module;
    private String action;
    private String targetType;
    private String targetId;
    private String detail;
    private String ip;
    private String requestUrl;
    private String requestMethod;
    private Integer costTime;
    private Integer result;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
