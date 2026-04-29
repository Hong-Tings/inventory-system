package com.inventory.sales.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class SalesOrderExportVO {

    @ExcelProperty("订单编号")
    private String orderNo;

    @ExcelProperty("客户")
    private String customerName;

    @ExcelProperty("仓库")
    private String warehouseName;

    @ExcelProperty("总数量")
    private Integer totalQuantity;

    @ExcelProperty("总金额")
    private BigDecimal totalAmount;

    @ExcelProperty("业务员")
    private String salesman;

    @ExcelProperty("操作人")
    private String operatorName;

    @ColumnWidth(15)
    @ExcelProperty("订单日期")
    private LocalDate orderDate;

    @ColumnWidth(26)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @ExcelProperty("状态")
    private String status;
}
