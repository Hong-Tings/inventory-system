-- =====================================================
-- 进销存管理系统 — 数据库建表脚本
-- 版本: v2.0
-- 数据库: MySQL 8.0
-- =====================================================

CREATE DATABASE IF NOT EXISTS `inventory` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE `inventory`;

-- =====================================================
-- 1. 系统管理
-- =====================================================

-- 1.1 用户表（角色使用 role 字段，1=管理员 2=员工）
CREATE TABLE `sys_user` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `username`    VARCHAR(64)  NOT NULL COMMENT '用户名',
  `password`    VARCHAR(256) NOT NULL COMMENT '密码（BCrypt）',
  `real_name`   VARCHAR(100) NULL     COMMENT '真实姓名',
  `phone`       VARCHAR(20)  NULL     COMMENT '手机号',
  `email`       VARCHAR(100) NULL     COMMENT '邮箱',
  `avatar`      VARCHAR(500) NULL     COMMENT '头像',
  `position`    VARCHAR(50)  NULL     COMMENT '职位（如销售员、仓管员、养护工）',
  `role`        TINYINT      DEFAULT 2 COMMENT '角色 1=管理员 2=员工',
  `status`      TINYINT      DEFAULT 1 COMMENT '状态 0=禁用 1=启用',
  `deleted`     TINYINT      DEFAULT 0 COMMENT '逻辑删除',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_status` (`status`),
  KEY `idx_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 1.2 操作日志表
CREATE TABLE `sys_operation_log` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT,
  `operator_id` BIGINT        NULL     COMMENT '操作人ID',
  `operator`    VARCHAR(64)   NULL     COMMENT '操作人用户名',
  `module`      VARCHAR(64)   NOT NULL COMMENT '操作模块',
  `action`      VARCHAR(64)   NOT NULL COMMENT '操作动作',
  `target_type` VARCHAR(64)   NULL     COMMENT '目标类型',
  `target_id`   VARCHAR(64)   NULL     COMMENT '目标ID',
  `detail`      VARCHAR(1000) NULL     COMMENT '操作详情',
  `ip`          VARCHAR(64)   NULL     COMMENT '请求IP',
  `request_url` VARCHAR(500)  NULL     COMMENT '请求URL',
  `request_method` VARCHAR(10) NULL    COMMENT '请求方法',
  `cost_time`   INT           DEFAULT 0 COMMENT '耗时(ms)',
  `result`      TINYINT       DEFAULT 1 COMMENT '结果 1=成功 0=失败',
  `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_module` (`module`),
  KEY `idx_operator` (`operator`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作日志表';

-- 1.3 系统配置表
CREATE TABLE IF NOT EXISTS `sys_config` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_key`  VARCHAR(100) NOT NULL COMMENT '配置键',
  `config_value` VARCHAR(500) NOT NULL COMMENT '配置值',
  `remark`      VARCHAR(200) NULL     COMMENT '说明',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- =====================================================
-- 2. 基础数据
-- =====================================================

-- 2.1 商品分类表
CREATE TABLE `product_category` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `name`        VARCHAR(100) NOT NULL COMMENT '分类名称',
  `parent_id`   BIGINT       NULL     COMMENT '父分类ID',
  `sort`        INT          DEFAULT 0 COMMENT '排序',
  `status`      TINYINT      DEFAULT 1 COMMENT '状态',
  `deleted`     TINYINT      DEFAULT 0,
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品分类表';

-- 2.2 商品表
CREATE TABLE `product` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code`           VARCHAR(64)   NOT NULL COMMENT '商品编码',
  `name`           VARCHAR(200)  NOT NULL COMMENT '商品名称',
  `category_id`    BIGINT        NULL     COMMENT '分类ID',
  `spec`           VARCHAR(200)  NULL     COMMENT '规格型号',
  `unit`           VARCHAR(20)   NOT NULL COMMENT '单位（个/箱/KG等）',
  `purchase_price` DECIMAL(10,2) NULL     COMMENT '采购参考价',
  `sale_price`     DECIMAL(10,2) NULL     COMMENT '销售参考价',
  `image_url`      VARCHAR(500)  NULL     COMMENT '商品图片',
  `min_stock`      INT           DEFAULT 0 COMMENT '最低库存（安全库存）',
  `max_stock`      INT           DEFAULT 0 COMMENT '最高库存',
  `status`         TINYINT       DEFAULT 1 COMMENT '状态 0=停用 1=启用',
  `remark`         VARCHAR(500)  NULL     COMMENT '备注',
  `deleted`        TINYINT       DEFAULT 0 COMMENT '逻辑删除',
  `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_category` (`category_id`),
  KEY `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='商品表';

-- 2.3 仓库表
CREATE TABLE `warehouse` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `code`        VARCHAR(64)  NULL     COMMENT '仓库编码（1-3级虚拟节点为null）',
  `name`        VARCHAR(200) NOT NULL COMMENT '仓库名称',
  `contact`     VARCHAR(50)  NULL     COMMENT '联系人',
  `phone`       VARCHAR(20)  NULL     COMMENT '联系电话',
  `address`     VARCHAR(500) NULL     COMMENT '仓库地址',
  `status`      TINYINT      DEFAULT 1 COMMENT '状态 0=停用 1=启用',
  `level`       TINYINT      DEFAULT 4 COMMENT '层级 1-4（1为最高级）',
  `parent_id`   BIGINT       NULL     COMMENT '上级ID（1级填null）',
  `remark`      VARCHAR(500) NULL     COMMENT '备注',
  `deleted`     TINYINT      DEFAULT 0,
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`),
  KEY `idx_parent` (`parent_id`),
  KEY `idx_level` (`level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仓库表';

-- 2.4 库位表
CREATE TABLE `warehouse_location` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `warehouse_id` BIGINT       NOT NULL COMMENT '所属仓库ID',
  `code`         VARCHAR(64)  NOT NULL COMMENT '库位编码（如 A-01-01）',
  `name`         VARCHAR(200) NULL     COMMENT '库位名称',
  `capacity`     INT          NULL     COMMENT '容量',
  `status`       TINYINT      DEFAULT 1 COMMENT '状态 0=禁用 1=启用',
  `remark`       VARCHAR(500) NULL,
  `deleted`      TINYINT      DEFAULT 0,
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_warehouse_code` (`warehouse_id`, `code`),
  KEY `idx_warehouse` (`warehouse_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库位表';

-- 2.5 供应商表
CREATE TABLE `supplier` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `code`        VARCHAR(64)  NOT NULL COMMENT '供应商编码',
  `name`        VARCHAR(200) NOT NULL COMMENT '供应商名称',
  `contact`     VARCHAR(50)  NULL     COMMENT '联系人',
  `phone`       VARCHAR(20)  NULL     COMMENT '联系电话',
  `address`     VARCHAR(500) NULL     COMMENT '地址',
  `status`      TINYINT      DEFAULT 1 COMMENT '状态',
  `remark`      VARCHAR(500) NULL,
  `deleted`     TINYINT      DEFAULT 0,
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应商表';

-- 2.6 客户表
CREATE TABLE `customer` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT,
  `code`        VARCHAR(64)  NOT NULL COMMENT '客户编码',
  `name`        VARCHAR(200) NOT NULL COMMENT '客户名称',
  `contact`     VARCHAR(50)  NULL     COMMENT '联系人',
  `phone`       VARCHAR(20)  NULL     COMMENT '联系电话',
  `address`     VARCHAR(500) NULL     COMMENT '地址',
  `status`      TINYINT      DEFAULT 1,
  `remark`      VARCHAR(500) NULL,
  `deleted`     TINYINT      DEFAULT 0,
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户表';

-- =====================================================
-- 3. 采购入库
-- =====================================================

-- 3.1 采购入库单主表
CREATE TABLE `purchase_order` (
  `id`             BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `order_no`       VARCHAR(64)   NOT NULL COMMENT '入库单号',
  `supplier_id`    BIGINT        NULL     COMMENT '供应商ID',
  `warehouse_id`   BIGINT        NOT NULL COMMENT '仓库ID',
  `location_id`    BIGINT        NULL     COMMENT '库位ID',
  `total_amount`   DECIMAL(12,2) DEFAULT 0 COMMENT '入库总金额',
  `total_quantity` INT           DEFAULT 0 COMMENT '入库总数量',
  `status`         TINYINT       DEFAULT 0 COMMENT '状态 0=草稿 1=已入库 2=已取消 3=已作废 4=待审批',
  `operator_id`    BIGINT        NULL     COMMENT '操作人ID',
  `approver_id`    BIGINT        NULL     COMMENT '审核人ID',
  `approve_time`   DATETIME      NULL     COMMENT '审核时间',
  `order_date`     DATE          NOT NULL COMMENT '入库日期',
  `remark`         VARCHAR(500)  NULL     COMMENT '备注',
  `deleted`        TINYINT       DEFAULT 0,
  `create_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_supplier` (`supplier_id`),
  KEY `idx_warehouse` (`warehouse_id`),
  KEY `idx_operator` (`operator_id`),
  KEY `idx_order_date` (`order_date`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购入库单';

-- 3.2 采购入库明细表
CREATE TABLE `purchase_order_item` (
  `id`              BIGINT        NOT NULL AUTO_INCREMENT,
  `order_id`        BIGINT        NOT NULL COMMENT '入库单ID',
  `product_id`      BIGINT        NOT NULL COMMENT '商品ID',
  `quantity`        INT           NOT NULL COMMENT '入库数量',
  `unit_price`      DECIMAL(10,2) NULL     COMMENT '单价',
  `amount`          DECIMAL(12,2) NULL     COMMENT '金额',
  `batch_no`        VARCHAR(100)  NULL     COMMENT '批次号',
  `production_date` DATE          NULL     COMMENT '生产日期',
  `expiry_date`     DATE          NULL     COMMENT '有效期',
  `remark`          VARCHAR(500)  NULL,
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`),
  KEY `idx_product` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购入库明细';

-- =====================================================
-- 4. 销售出库
-- =====================================================

-- 4.1 销售出库单主表
CREATE TABLE `sales_order` (
  `id`               BIGINT        NOT NULL AUTO_INCREMENT,
  `order_no`         VARCHAR(64)   NOT NULL COMMENT '出库单号',
  `customer_id`      BIGINT        NULL     COMMENT '客户ID',
  `warehouse_id`     BIGINT        NOT NULL COMMENT '仓库ID',
  `total_amount`     DECIMAL(12,2) DEFAULT 0 COMMENT '出库总金额',
  `total_quantity`   INT           DEFAULT 0 COMMENT '出库总数量',
  `salesman`         VARCHAR(50)   NULL     COMMENT '销售员',
  `external_order_no` VARCHAR(100) NULL     COMMENT '外部订单号',
  `status`           TINYINT       DEFAULT 0 COMMENT '0=草稿 1=已出库 2=已取消 3=已作废 4=待审批',
  `operator_id`      BIGINT        NULL     COMMENT '操作人ID',
  `approver_id`      BIGINT        NULL     COMMENT '审核人ID',
  `approve_time`     DATETIME      NULL     COMMENT '审核时间',
  `order_date`       DATE          NOT NULL COMMENT '出库日期',
  `remark`           VARCHAR(500)  NULL,
  `deleted`          TINYINT       DEFAULT 0,
  `create_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_customer` (`customer_id`),
  KEY `idx_warehouse` (`warehouse_id`),
  KEY `idx_operator` (`operator_id`),
  KEY `idx_order_date` (`order_date`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='销售出库单';

-- 4.2 销售出库明细表
CREATE TABLE `sales_order_item` (
  `id`         BIGINT        NOT NULL AUTO_INCREMENT,
  `order_id`   BIGINT        NOT NULL COMMENT '出库单ID',
  `product_id` BIGINT        NOT NULL COMMENT '商品ID',
  `quantity`   INT           NOT NULL COMMENT '出库数量',
  `unit_price` DECIMAL(10,2) NULL     COMMENT '售价',
  `amount`     DECIMAL(12,2) NULL     COMMENT '金额',
  `batch_no`   VARCHAR(100)  NULL     COMMENT '批次号',
  `remark`     VARCHAR(500)  NULL,
  PRIMARY KEY (`id`),
  KEY `idx_order` (`order_id`),
  KEY `idx_product` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='销售出库明细';

-- =====================================================
-- 5. 库存
-- =====================================================

-- 5.1 实时库存表
CREATE TABLE `inventory` (
  `id`           BIGINT        NOT NULL AUTO_INCREMENT,
  `product_id`   BIGINT        NOT NULL COMMENT '商品ID',
  `warehouse_id` BIGINT        NOT NULL COMMENT '仓库ID',
  `location_id`  BIGINT        NULL     COMMENT '库位ID',
  `batch_no`     VARCHAR(100)  NULL     COMMENT '批次号',
  `quantity`     INT           NOT NULL DEFAULT 0 COMMENT '可用库存',
  `locked_qty`   INT           NOT NULL DEFAULT 0 COMMENT '锁定库存',
  `cost_price`   DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT '加权平均成本价（移动加权平均法）',
  `remark`       VARCHAR(500)  NULL,
  `create_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_prod_warehouse_loc_batch` (`product_id`, `warehouse_id`, `location_id`, `batch_no`),
  KEY `idx_warehouse` (`warehouse_id`),
  KEY `idx_product` (`product_id`),
  KEY `idx_location` (`location_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实时库存表';

-- 5.2 库存流水表
CREATE TABLE `inventory_log` (
  `id`           BIGINT        NOT NULL AUTO_INCREMENT,
  `product_id`   BIGINT        NOT NULL COMMENT '商品ID',
  `warehouse_id` BIGINT        NOT NULL COMMENT '仓库ID',
  `location_id`  BIGINT        NULL     COMMENT '库位ID',
  `batch_no`     VARCHAR(100)  NULL     COMMENT '批次号',
  `change_type`  VARCHAR(32)   NOT NULL COMMENT '变动类型：PURCHASE_IN/ SALES_OUT/ TRANSFER_IN/ TRANSFER_OUT/ STOCKTAKE/ CANCEL_PURCHASE/ CANCEL_SALES/ CANCEL_TRANSFER/ ADJUSTMENT_IN/ ADJUSTMENT_OUT',
  `change_qty`   INT           NOT NULL COMMENT '变动数量（正=增加，负=减少）',
  `before_qty`   INT           NOT NULL COMMENT '变动前数量',
  `after_qty`    INT           NOT NULL COMMENT '变动后数量',
  `unit_price`   DECIMAL(10,2) NULL     COMMENT '单价',
  `amount`       DECIMAL(12,2) NULL     COMMENT '金额',
  `ref_order_no` VARCHAR(64)   NULL     COMMENT '关联单号',
  `operator_id`  BIGINT        NULL     COMMENT '操作人ID',
  `remark`       VARCHAR(500)  NULL,
  `create_time`  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_product` (`product_id`),
  KEY `idx_warehouse` (`warehouse_id`),
  KEY `idx_change_type` (`change_type`),
  KEY `idx_ref_order` (`ref_order_no`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存流水表';

-- =====================================================
-- 6. 盘点
-- =====================================================

-- 6.1 盘点单主表
CREATE TABLE `stock_take` (
  `id`           BIGINT       NOT NULL AUTO_INCREMENT,
  `order_no`     VARCHAR(64)  NOT NULL COMMENT '盘点单号',
  `warehouse_id` BIGINT       NOT NULL COMMENT '仓库ID',
  `take_type`    TINYINT      DEFAULT 0 COMMENT '0=全盘 1=抽盘',
  `total_items`  INT          DEFAULT 0 COMMENT '盘点商品数',
  `diff_items`   INT          DEFAULT 0 COMMENT '差异商品数',
  `status`       TINYINT      DEFAULT 0 COMMENT '0=盘点中 1=已审核 2=已调整 3=已作废',
  `operator_id`  BIGINT       NULL     COMMENT '盘点人ID',
  `approver_id`  BIGINT       NULL     COMMENT '审核人ID',
  `order_date`   DATE         NULL     COMMENT '盘点日期',
  `remark`       VARCHAR(500) NULL,
  `deleted`      TINYINT      DEFAULT 0,
  `create_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_warehouse` (`warehouse_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='盘点单';

-- 6.2 盘点明细表
CREATE TABLE `stock_take_item` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT,
  `stock_take_id` BIGINT        NOT NULL COMMENT '盘点单ID',
  `product_id`    BIGINT        NOT NULL COMMENT '商品ID',
  `location_id`   BIGINT        NULL     COMMENT '库位ID',
  `batch_no`      VARCHAR(100)  NULL     COMMENT '批次号',
  `book_qty`      INT           NOT NULL DEFAULT 0 COMMENT '账面数量',
  `actual_qty`    INT           NOT NULL DEFAULT 0 COMMENT '实盘数量',
  `diff_qty`      INT           NOT NULL DEFAULT 0 COMMENT '差异数量',
  `diff_reason`   VARCHAR(500)  NULL     COMMENT '差异原因',
  `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_stock_take` (`stock_take_id`),
  KEY `idx_product` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='盘点明细';

-- 6.3 库存调整单表（预留，当前盘点调整直接更新 inventory 表）
CREATE TABLE `stock_adjustment` (
  `id`            BIGINT        NOT NULL AUTO_INCREMENT,
  `order_no`      VARCHAR(64)   NOT NULL COMMENT '调整单号',
  `stock_take_id` BIGINT        NULL     COMMENT '关联盘点单ID',
  `warehouse_id`  BIGINT        NOT NULL COMMENT '仓库ID',
  `adjust_type`   TINYINT       DEFAULT 0 COMMENT '0=盘盈 1=盘亏',
  `total_amount`  DECIMAL(12,2) DEFAULT 0 COMMENT '调整金额',
  `status`        TINYINT       DEFAULT 0 COMMENT '0=草稿 1=已调整',
  `operator_id`   BIGINT        NULL     COMMENT '操作人ID',
  `remark`        VARCHAR(500)  NULL,
  `deleted`       TINYINT       DEFAULT 0,
  `create_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_stock_take` (`stock_take_id`),
  KEY `idx_warehouse` (`warehouse_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存调整单';

-- =====================================================
-- 7. 库存调拨
-- =====================================================

CREATE TABLE `inventory_transfer` (
  `id`               BIGINT       NOT NULL AUTO_INCREMENT,
  `order_no`         VARCHAR(64)  NOT NULL COMMENT '调拨单号',
  `from_warehouse_id` BIGINT      NOT NULL COMMENT '调出仓库ID',
  `to_warehouse_id`   BIGINT      NOT NULL COMMENT '调入仓库ID',
  `total_quantity`   INT          DEFAULT 0 COMMENT '调拨总数量',
  `status`           TINYINT      DEFAULT 0 COMMENT '0=草稿 1=已完成 2=已取消 3=已作废 4=待审批',
  `operator_id`      BIGINT       NULL     COMMENT '操作人ID',
  `approver_id`      BIGINT       NULL     COMMENT '审核人ID',
  `approve_time`     DATETIME     NULL     COMMENT '审核时间',
  `order_date`       DATE         NULL     COMMENT '调拨日期',
  `remark`           VARCHAR(500) NULL,
  `deleted`          TINYINT      DEFAULT 0,
  `create_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_from_wh` (`from_warehouse_id`),
  KEY `idx_to_wh` (`to_warehouse_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存调拨单';

CREATE TABLE `inventory_transfer_item` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT,
  `transfer_id` BIGINT        NOT NULL COMMENT '调拨单ID',
  `product_id`  BIGINT        NOT NULL COMMENT '商品ID',
  `quantity`    INT           NOT NULL COMMENT '调拨数量',
  `batch_no`    VARCHAR(100)  NULL     COMMENT '批次号',
  PRIMARY KEY (`id`),
  KEY `idx_transfer` (`transfer_id`),
  KEY `idx_product` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存调拨明细';

-- =====================================================
-- 默认数据
-- =====================================================

-- 默认账号（密码均为 123456，BCrypt 哈希）
INSERT IGNORE INTO `sys_user` (`id`, `username`, `password`, `real_name`, `position`, `role`, `status`) VALUES
(1, 'admin',     '$2b$10$fcRVnc5Ku/EIsEnzWU2tA.2V9BevXAK1HzlaoslAufd.f.Vy3LeJu', '系统管理员', NULL,      1, 1),
(2, 'warehouse', '$2b$10$fcRVnc5Ku/EIsEnzWU2tA.2V9BevXAK1HzlaoslAufd.f.Vy3LeJu', '仓库管理员', '仓管员', 2, 1),
(3, 'sales',     '$2b$10$fcRVnc5Ku/EIsEnzWU2tA.2V9BevXAK1HzlaoslAufd.f.Vy3LeJu', '销售员',    '销售员', 2, 1);

-- 系统配置
INSERT IGNORE INTO `sys_config` (`config_key`, `config_value`, `remark`) VALUES
('purchase_order_prefix', 'PO', '入库单前缀'),
('sales_order_prefix', 'SO', '出库单前缀'),
('stocktake_order_prefix', 'CK', '盘点单前缀'),
('alert_enabled', 'true', '安全库存预警开关');
