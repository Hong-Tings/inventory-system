package com.inventory.transfer.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("inventory_transfer_item")
public class InventoryTransferItem {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long transferId;
    private Long productId;
    private Integer quantity;
    private String batchNo;

    @TableField(exist = false)
    private String productName;

    @TableField(exist = false)
    private String productCode;
}
