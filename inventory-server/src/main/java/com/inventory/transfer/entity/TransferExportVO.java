package com.inventory.transfer.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class TransferExportVO {
    @ExcelProperty("单据编号") private String orderNo;
    @ExcelProperty("调出仓库") private String fromWarehouseName;
    @ExcelProperty("调入仓库") private String toWarehouseName;
    @ExcelProperty("调拨数量") private Integer totalQuantity;
    @ExcelProperty("状态") private String status;
    @ColumnWidth(18) @ExcelProperty("单据日期") private LocalDate orderDate;
    @ColumnWidth(26) @ExcelProperty("新增时间") private LocalDateTime createTime;
    @ColumnWidth(26) @ExcelProperty("更新时间") private LocalDateTime updateTime;
}
