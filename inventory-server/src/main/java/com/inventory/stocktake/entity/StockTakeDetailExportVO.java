package com.inventory.stocktake.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

@Data
public class StockTakeDetailExportVO {
    @ExcelProperty("盘点单号") private String orderNo;
    @ExcelProperty("仓库") private String warehouseName;
    @ExcelProperty("盘点方式") private String takeType;
    @ExcelProperty("状态") private String status;
    @ExcelProperty("商品名称") private String productName;
    @ExcelProperty("商品编码") private String productCode;
    @ExcelProperty("批次号") private String batchNo;
    @ExcelProperty("账面数量") private Integer bookQty;
    @ExcelProperty("实际数量") private Integer actualQty;
    @ExcelProperty("差异数量") private Integer diffQty;
    @ExcelProperty("差异原因") private String diffReason;
}
