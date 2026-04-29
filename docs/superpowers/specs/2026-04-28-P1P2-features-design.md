# 进销存管理系统 — P1/P2 功能设计文档

> 版本：v1.0 | 日期：2026-04-28
> 对应：04-后续开发任务书.md 中 P1（功能补齐）+ P2（体验优化）

---

## 概述

本文档覆盖 7 个子任务，按实现依赖关系排序。每个任务独立可交付，互不阻塞。

---

## 一、Excel 导入/导出验证（P1-04 ~ P1-06）

### 现状
- ExcelUtil 工具类已完成，使用 EasyExcel 3.3.4
- 导出 VO 类已存在：ProductExportVO、InventoryExportVO、PurchaseOrderExportVO、SalesOrderExportVO
- ProductController 已有 `/api/v1/product/export` 端点
- InventoryController 已有 `/api/v1/inventory/export` 端点
- PurchaseOrderController 和 SalesOrderController **缺少**导出端点
- **缺少**商品导入端点
- 前端页面**缺少**导出按钮

### 改动

**后端：**
1. PurchaseOrderController + SalesOrderController 各新增 `/export` 端点，复用已有 ExportVO
2. ProductController 新增 `/import` 端点（POST，接收 MultipartFile），解析 Excel 后批量插入商品
3. 为 InventoryController 补充 `/import` 不需要（库存不通过 Excel 导入）

**前端：**
1. 商品列表页（ProductList.vue）：导出按钮（已有） + 导入弹窗（新增，选择文件上传）
2. 入库单列表页（PurchaseList.vue）：导出按钮
3. 出库单列表页（SalesList.vue）：导出按钮
4. 库存查询页（InventoryList.vue）：导出按钮（已有）

### 说明
"导出"就是把你当前看到的列表数据下载成一个 Excel 文件；"导入"就是从 Excel 文件批量创建商品，省去一个一个手动录入。

---

## 二、商品图片上传（P1-01 ~ P1-03）

### 现状
- Product 实体和数据库表都有 `image_url` 字段
- 没有任何文件上传相关的后端代码
- 商品表单没有图片上传组件

### 改动

**后端：**
1. 新增 `FileController`：
   - `POST /api/v1/file/upload` — 接收 multipart 文件，保存到 `uploads/images/` 目录
   - 返回文件访问 URL（如 `/uploads/images/xxx.jpg`）
2. 配置静态资源映射（WebMvcConfig），使 `uploads/` 目录可通过 HTTP 访问
3. 限制文件类型为图片（jpg/png/gif），限制大小 5MB

**前端：**
1. 商品表单（ProductList.vue 中的编辑对话框）：增加图片上传组件（el-upload）
2. 商品列表：增加缩略图列

### 说明
商品图片不是必须的。上传的图片保存在服务器本地 `uploads/images/` 目录下，生产环境需要 Nginx 托管静态文件。

---

## 三、出库 FIFO 策略（P1-07 ~ P1-08）

### 什么是 FIFO
FIFO = First In First Out（先进先出）。即：先入库的商品，出库时优先出掉。防止商品在仓库放太久过期/变质。

### 现状
- 当前出库 `submit()` 只是按 `product_id + warehouse_id + batch_no` 匹配库存，扣减数量
- 没有批次优先逻辑
- 数据库 `inventory` 表有 `batch_no` 字段和 `create_time`，可用于判断批次先后

### 改动

**后端（SalesOrderService.submit() 改造）：**
1. 出库时，先查该商品在该仓库下的所有库存记录，按 `create_time ASC` 排序（最早的批次在前）
2. 遍历批次，逐个扣减，直到扣完所需数量
3. 如果一个批次不够，自动扣下一个批次
4. 如果所有批次总和不够，抛异常"库存不足"

例如：需要出库 10 件商品 A：
- 批次 20240101 有 3 件 → 扣 3，剩 0，还需 7
- 批次 20240201 有 5 件 → 扣 5，剩 0，还需 2
- 批次 20240301 有 8 件 → 扣 2，剩 6，完成

**前端（P1-08）：**
- 出库创建页（SalesForm.vue）：选择商品时，显示该商品各批次的库存数量
- 默认按 FIFO 自动分配，但允许用户手动指定从哪个批次出库

### 工作量说明
FIFO 改动集中在 `SalesOrderService.java` 的 `submit()` 方法，约 40 行代码改动。前端改动在 SalesForm.vue。

---

## 四、库存周转报表（P1-09 ~ P1-10）

### 什么是库存周转
库存周转率 = 一段时间内的出库数量 / 平均库存。这个数字越大，说明商品卖得越快、库存管理效率越高。

### 现状
- ReportController 和 ReportService 已存在
- 但 Service 中 `purchaseSummary()` 和 `salesSummary()` 返回空数组
- 前端 ReportView.vue 使用的是硬编码 mock 数据

### 改动

**后端（ReportService）：**
1. `purchaseSummary(days)`：查询近 N 天的入库单，按日期分组统计数量和金额
2. `salesSummary(days)`：同上，按出库统计
3. 新增 `turnoverRate(days)`：计算库存周转率（出库总量 / 平均库存量）

**前端（ReportView.vue）：**
1. 去掉 mockDays/mockData 硬编码
2. 接入后端真实数据，用 ECharts 展示柱状图
3. 新增"库存周转率"tab

---

## 五、库存导出按钮（P2-01）

### 现状
- 后端 `/api/v1/inventory/export` 端点**已有**
- 前端库存页面**没有**导出按钮

### 改动
- InventoryList.vue 增加"导出"按钮，调用 `/inventory/export`
- < 0.5 天工时

---

## 六、入库单打印（P2-02）

### 现状
- PurchaseDetail.vue 已有完整的入库单详情展示

### 改动
- PurchaseDetail.vue 增加"打印"按钮
- 调用 `window.print()` 浏览器打印
- 添加打印样式（@media print），隐藏无关 UI（侧边栏、按钮等），只打印单据内容

---

## 七、系统配置（P2-03 ~ P2-04）

### 现状
- 没有配置表和配置页面
- 单号生成规则硬编码在 Service 中（PO/SO 前缀）

### 改动

**后端：**
1. 新增 `sys_config` 表（id, config_key, config_value, remark, create_time, update_time）
2. 新增 SysConfig 实体、Mapper、Service、Controller
3. 预置配置项：入库单前缀（默认 PO）、出库单前缀（默认 SO）、安全库存告警开关（默认开启）

**前端：**
1. 系统管理下新增"系统配置"页面
2. 表格展示所有配置项，支持编辑值

---

## 八、执行顺序

```
第 1 天  Excel 导入/导出（已有工具类，加端点+前端按钮，最快见效）
第 2 天  商品图片上传（FileController + 前端组件）
第 3 天  FIFO 出库策略（核心逻辑改动）
第 4 天  库存周转报表（修复空数据 + 前端图表）
第 5 天  P2 收尾：库存导出按钮 + 打印 + 系统配置
```

---

## 九、不做/延后的功能

- 图片上传不做 OSS/云存储（仅本地存储，生产用 Nginx 托管）
- 不增加审批流程、不做多级审核
- 不接入微信云开发
- 不做多单位换算
