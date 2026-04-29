package com.inventory.inventory.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InventoryLogExportVO {

    @ExcelProperty("商品名称")
    private String productName;

    @ExcelProperty("仓库")
    private String warehouseName;

    @ExcelProperty("操作类型")
    private String changeType;

    @ExcelProperty("变动数量")
    private Integer changeQty;

    @ExcelProperty("变动前")
    private Integer beforeQty;

    @ExcelProperty("变动后")
    private Integer afterQty;

    @ExcelProperty("关联单号")
    private String refOrderNo;

    @ExcelProperty("操作人")
    private String operatorName;

    @ColumnWidth(26)
    @ExcelProperty("时间")
    private LocalDateTime createTime;
}
