package com.inventory.inventory.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class InventoryExportVO {

    @ExcelProperty("商品编码")
    private String productCode;

    @ExcelProperty("商品名称")
    private String productName;

    @ExcelProperty("仓库")
    private String warehouseName;

    @ExcelProperty("库存数量")
    private Integer quantity;
}
