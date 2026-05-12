-- =====================================================
-- 升级脚本：增加审批人审计字段 + 更新状态注释
-- 适用版本：添加审批工作流后的数据库升级
-- =====================================================

-- 1. 采购入库单
ALTER TABLE `purchase_order`
  ADD COLUMN `approver_id`  BIGINT    NULL COMMENT '审核人ID' AFTER `operator_id`,
  ADD COLUMN `approve_time` DATETIME  NULL COMMENT '审核时间' AFTER `approver_id`;
ALTER TABLE `purchase_order` MODIFY COLUMN `status` TINYINT DEFAULT 0 COMMENT '状态 0=草稿 1=已入库 2=已取消 3=已作废 4=待审批';

-- 2. 销售出库单
ALTER TABLE `sales_order`
  ADD COLUMN `approver_id`  BIGINT    NULL COMMENT '审核人ID' AFTER `operator_id`,
  ADD COLUMN `approve_time` DATETIME  NULL COMMENT '审核时间' AFTER `approver_id`;
ALTER TABLE `sales_order` MODIFY COLUMN `status` TINYINT DEFAULT 0 COMMENT '0=草稿 1=已出库 2=已取消 3=已作废 4=待审批';

-- 3. 库存调拨单
ALTER TABLE `inventory_transfer`
  ADD COLUMN `approver_id`  BIGINT    NULL COMMENT '审核人ID' AFTER `operator_id`,
  ADD COLUMN `approve_time` DATETIME  NULL COMMENT '审核时间' AFTER `approver_id`;
ALTER TABLE `inventory_transfer` MODIFY COLUMN `status` TINYINT DEFAULT 0 COMMENT '0=草稿 1=已完成 2=已取消 3=已作废 4=待审批';
