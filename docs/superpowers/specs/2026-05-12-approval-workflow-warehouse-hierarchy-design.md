# 进销存管理系统 — 审批工作流 & 仓库分级设计

> 日期：2026-05-12 | 版本：v2.2

---

## 一、审批工作流

### 1.1 背景

当前采购入库、销售出库、库存调拨的状态流转为：草稿(0) → 提交 → 已确认(1)，提交即直接触发生成库存变动，无审核环节。现需加入单级管理员审批流程。

### 1.2 状态变更

```
草稿(0) → 提交 → 待审批(4) → 审核通过 → 已确认(1)
                            → 驳回 → 草稿(0)
       → 取消 → 已取消(2)
       → 作废 → 已作废(3)
```

### 1.3 涉及模块

三个模块统一改造：采购入库（PurchaseOrder）、销售出库（SalesOrder）、库存调拨（InventoryTransfer）

### 1.4 后端改动

#### OrderStatus 常量类
- 新增 `PENDING = 4`（待审批）

#### Service 层逻辑变更

| 方法 | 原行为 | 新行为 |
|------|--------|--------|
| `submit()` | 草稿→已确认, 更新库存 | 草稿→待审批, **不更新库存** |
| `approve()`(新增) | — | 待审批→已确认, **更新库存**；`@SaCheckRole("role_1")` |
| `reject(reason)`(新增) | — | 待审批→草稿, **不操作库存**；记录驳回原因 |
| `cancel()` | 已确认→已取消, 回滚库存 | 草稿：直接取消，不操作库存（原行为不变） |
| | | 待审批：管理员可取消，不操作库存（库存未变动过） |
| | | 已确认：回滚库存后取消（原行为不变） |
| `voidOrder(reason)` | 已确认→已作废 | **不变** |

> 注意：草稿和待审批状态下取消/驳回，不需要触发库存回滚（因为库存未变动过）。

#### Controller 接口新增

| 模块 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 采购 | PUT | `/api/v1/purchase-order/{id}/approve` | 审核通过 |
| 采购 | PUT | `/api/v1/purchase-order/{id}/approve` | 审核通过 |
| 采购 | PUT | `/api/v1/purchase-order/{id}/reject` | 驳回（body: `{"reason":"xxx"}`） |
| 销售 | PUT | `/api/v1/sales-order/{id}/approve` | 审核通过 |
| 销售 | PUT | `/api/v1/sales-order/{id}/reject` | 驳回（body: `{"reason":"xxx"}`） |
| 调拨 | PUT | `/api/v1/transfer/{id}/approve` | 审核通过 |
| 调拨 | PUT | `/api/v1/transfer/{id}/reject` | 驳回（body: `{"reason":"xxx"}`） |

### 1.5 前端改动

#### 详情页（PurchaseDetail / SalesDetail / TransferDetail）
- 单据状态新增 **"待审批"** 标签显示
- 管理员角色可见 **"审核通过"** 和 **"驳回"** 按钮
- 点击驳回弹出对话框，填写驳回原因（必填）
- 提交按钮文案从"确认入库/出库"改为 **"提交审批"**

#### 列表页
- 状态筛选项新增 **"待审批"**
- 列表状态列新增待审批样式

---

## 二、仓库分级

### 2.1 背景

当前仓库为扁平结构，无法按组织层级查询。现需支持 1-4 级仓库层级（如：华南大区 → 广东省 → 深圳市 → 龙华仓库），并支持逐级下钻查询和直接搜索仓库号。

### 2.2 设计方案

采用自关联树方案（与商品分类 `product_category` 模式一致）。

### 2.3 数据库变更

```sql
-- warehouse 表新增字段
ALTER TABLE `warehouse`
  ADD COLUMN `level`     TINYINT    DEFAULT 4 COMMENT '层级 1=大区 2=区域 3=城市 4=具体仓库',
  ADD COLUMN `parent_id` BIGINT     NULL     COMMENT '上级ID（1级填null）',
  ADD INDEX `idx_parent` (`parent_id`),
  ADD INDEX `idx_level` (`level`);
```

### 2.4 后端接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/v1/warehouse/tree` | 获取完整仓库树形结构 |
| GET | `/api/v1/warehouse/children/{parentId}` | 获取某节点下的直接子级列表 |
| GET | `/api/v1/warehouse/page` | 分页查询（新增 `level`、`parentId` 筛选参数） |
| GET | `/api/v1/warehouse/list` | 原接口，改为只返回 level=4 的具体仓库（供单据选择用） |
| GET | `/api/v1/warehouse/search?keyword=` | 新增，按仓库编码/名称模糊搜索直达具体仓库 |

### 2.5 前端改动

#### WarehouseList.vue（仓库管理页）

**搜索区：**
```
[1级下拉] ─选择后→ [2级下拉] ─选择后→ [3级下拉] ─选择后→ [4级下拉]
同时保留原有的：名称/联系人/电话/地址 搜索框
新增：仓库编码/名称直接搜索框（输入关键词，不依赖级联）
```

**表格列：**
- 新增"层级"列（显示 1级/2级/3级/4级）
- 新增"上级"列（显示上级名称）

**编辑弹窗：**
- 新增"层级"下拉（1-4级）
- 新增"上级仓库"选择器
  - 1级：不显示上级选择
  - 2级：上级可选所有1级
  - 3级：上级可选所有2级
  - 4级：上级可选所有3级

#### 单据页面（PurchaseForm / SalesForm / TransferForm / StockTakeForm）

仓库选择器改为**级联选择器（Cascader）**：

```
层级面包屑：华南大区 / 广东省 / 深圳市 / [选择具体仓库]
同时选项列表底部保留"搜索仓库号"输入框，直接搜索 level=4 的仓库
```

#### 小程序
- 仓库选择区域同样改为级联 + 搜索模式

---

## 三、涉及文件清单

### 后端

| 文件路径 | 改动类型 |
|---------|---------|
| `common/constant/OrderStatus.java` | 新增常量 |
| `purchase/service/PurchaseOrderService.java` | submit 逻辑变更，新增 approve/reject |
| `purchase/controller/PurchaseOrderController.java` | 新增 approve/reject 接口 |
| `sales/service/SalesOrderService.java` | 同上 |
| `sales/controller/SalesOrderController.java` | 同上 |
| `transfer/service/TransferService.java` | 同上 |
| `transfer/controller/TransferController.java` | 同上 |
| `warehouse/entity/Warehouse.java` | 新增 level, parentId 字段 |
| `warehouse/service/WarehouseService.java` | 新增 tree/children/search 方法，修改 page/list |
| `warehouse/controller/WarehouseController.java` | 新增 tree/children/search 接口 |

### 数据库

| 文件 |
|------|
| `sql/01-schema.sql` | warehouse 表新增字段 |

### 前端

| 文件路径 | 改动类型 |
|---------|---------|
| `views/warehouse/WarehouseList.vue` | 级联搜索 + 表格列 + 编辑弹窗 |
| `views/purchase/PurchaseForm.vue` | 仓库级联选择器 |
| `views/sales/SalesForm.vue` | 同上 |
| `views/transfer/TransferForm.vue` | 同上 |
| `views/stocktake/StockTakeForm.vue` | 同上 |
| `views/purchase/PurchaseDetail.vue` | 审批按钮 + 驳回弹窗 |
| `views/sales/SalesDetail.vue` | 同上 |
| `views/transfer/TransferDetail.vue` | 同上 |
| `views/purchase/PurchaseList.vue` | 状态列新增"待审批" |
| `views/sales/SalesList.vue` | 同上 |
| `views/transfer/TransferList.vue` | 同上 |

### 小程序

| 文件路径 | 改动类型 |
|---------|---------|
| `pages/purchase/create.vue` 等 | 仓库选择改为级联 |
