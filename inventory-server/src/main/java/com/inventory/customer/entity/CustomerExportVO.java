package com.inventory.customer.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CustomerExportVO {
    @ExcelProperty("编码") private String code;
    @ExcelProperty("名称") private String name;
    @ExcelProperty("联系人") private String contact;
    @ExcelProperty("电话") private String phone;
    @ExcelProperty("地址") private String address;
    @ExcelProperty("状态") private String status;
    @ColumnWidth(26) @ExcelProperty("新增时间") private LocalDateTime createTime;
    @ColumnWidth(26) @ExcelProperty("更新时间") private LocalDateTime updateTime;
}
