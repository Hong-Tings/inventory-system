package com.inventory.product.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ProductExportVO {

    @ExcelProperty("编码")
    private String code;

    @ExcelProperty("名称")
    private String name;

    @ExcelProperty("规格")
    private String spec;

    @ExcelProperty("单位")
    private String unit;

    @ExcelProperty("采购价")
    private BigDecimal purchasePrice;

    @ExcelProperty("销售价")
    private BigDecimal salePrice;

    @ExcelProperty("最低库存")
    private Integer minStock;

    @ExcelProperty("当前库存")
    private Integer inventoryQuantity;

    @ExcelProperty("库存状态")
    private String alertStatus;

    @ExcelProperty("分类名称")
    private String categoryName;

    @ColumnWidth(20)
    @ExcelProperty("新增时间")
    private LocalDateTime createTime;
}
