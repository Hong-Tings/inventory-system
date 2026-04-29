package com.inventory.transfer.entity;

import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("inventory_transfer")
public class InventoryTransfer {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String orderNo;
    private Long fromWarehouseId;
    private Long toWarehouseId;
    private Integer totalQuantity;
    private Integer status;
    private Long operatorId;
    @ColumnWidth(26)
    private LocalDate orderDate;
    private String remark;

    @ColumnWidth(26)
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @ColumnWidth(26)
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String fromWarehouseName;

    @TableField(exist = false)
    private String toWarehouseName;

    @TableField(exist = false)
    private String operatorName;

    @TableField(exist = false)
    private Long outWarehouseId;

    @TableField(exist = false)
    private Long inWarehouseId;

    @TableField(exist = false)
    private List<InventoryTransferItem> items;
}
