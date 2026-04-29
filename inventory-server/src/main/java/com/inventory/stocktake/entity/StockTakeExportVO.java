package com.inventory.stocktake.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class StockTakeExportVO {
    @ExcelProperty("单据编号") private String orderNo;
    @ExcelProperty("仓库") private String warehouseName;
    @ExcelProperty("盘点方式") private String takeType;
    @ExcelProperty("总项数") private Integer totalItems;
    @ExcelProperty("差异数") private Integer diffItems;
    @ExcelProperty("状态") private String status;
    @ColumnWidth(18) @ExcelProperty("单据日期") private LocalDate orderDate;
    @ColumnWidth(26) @ExcelProperty("新增时间") private LocalDateTime createTime;
    @ColumnWidth(26) @ExcelProperty("更新时间") private LocalDateTime updateTime;
}
