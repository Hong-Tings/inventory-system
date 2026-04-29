package com.inventory.common.constant;

public interface OrderStatus {
    // 入库单/出库单状态
    int DRAFT = 0;      // 草稿
    int CONFIRMED = 1;  // 已入库/已出库
    int CANCELED = 2;   // 已取消

    // 盘点单状态
    int STOCKTAKE_IN_PROGRESS = 0;  // 盘点中
    int STOCKTAKE_APPROVED = 1;     // 已审核
    int STOCKTAKE_ADJUSTED = 2;     // 已调整

    // 通用：已作废（订单类统一使用）
    int VOIDED = 3;

    // 库存变动类型
    String PURCHASE_IN = "PURCHASE_IN";       // 采购入库
    String SALES_OUT = "SALES_OUT";           // 销售出库
    String ADJUSTMENT_IN = "ADJUSTMENT_IN";   // 盘盈入库
    String ADJUSTMENT_OUT = "ADJUSTMENT_OUT"; // 盘亏出库
    String STOCKTAKE_ADJUST = "STOCKTAKE";    // 盘点调整
}
