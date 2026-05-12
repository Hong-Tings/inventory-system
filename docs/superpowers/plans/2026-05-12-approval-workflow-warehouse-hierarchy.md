# 审批工作流 & 仓库分级 实现计划

> **面向 AI 代理的工作者：** 必需子技能：使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 逐任务实现此计划。步骤使用复选框（`- [ ]`）语法来跟踪进度。

**目标：** 在采购入库、销售出库、库存调拨三个模块中引入管理员单级审批流程；将仓库表改为自关联树结构，支持 1-4 级分层查询。

**架构：**
- 审批流：状态机新增 `PENDING(4)` 状态；`submit()` 改为提交到待审批；`approve()` 处理库存变动逻辑（从原 `submit()` 提取）；`reject()` 驳回回到草稿。
- 仓库分级：自关联树方案（与 `product_category` 一致），`warehouse` 表新增 `level`、`parent_id` 字段；后端新增 `tree`/`children`/`search` 接口；前端级联选择器。

**技术栈：** Java 17, Spring Boot 3.2, MyBatis-Plus, Vue 3 + Element Plus, UniApp

---

### 任务 1：常量 & 数据库变更

**文件：**
- 修改：`inventory-server/src/main/java/com/inventory/common/constant/OrderStatus.java`
- 修改：`sql/01-schema.sql`

- [ ] **步骤 1：OrderStatus 新增 PENDING 常量**

```java
// 在 OrderStatus 中添加（在第 6 行 CONFIRMED = 1 之后）
int PENDING = 4;  // 待审批
```

- [ ] **步骤 2：SQL 新增 warehouse 字段**

```sql
-- 在 warehouse 表定义中新增（在 status 字段之后）
`level`     TINYINT    DEFAULT 4 COMMENT '层级 1=大区 2=区域 3=城市 4=具体仓库',
`parent_id` BIGINT     NULL     COMMENT '上级ID（1级填null）',

-- 在表末尾的 PRIMARY KEY 之后新增索引
KEY `idx_parent` (`parent_id`),
KEY `idx_level` (`level`),
```

- [ ] **步骤 3：提交**

```bash
git add inventory-server/src/main/java/com/inventory/common/constant/OrderStatus.java sql/01-schema.sql
git commit -m "feat: add PENDING status constant and warehouse level/parent_id fields"
```

---

### 任务 2：PurchaseOrderService 审批流改造

**文件：**
- 修改：`inventory-server/src/main/java/com/inventory/purchase/service/PurchaseOrderService.java`

- [ ] **步骤 1：修改 `submit()` 方法 — 只提交到待审批，不操作库存**

修改 `submit()` 方法，将原有库存操作逻辑移到 `approve()`，submit 只做状态校验和状态变更：

```java
@Transactional(rollbackFor = Exception.class)
public synchronized void submit(Long id) {
    PurchaseOrder order = purchaseOrderMapper.selectById(id);
    if (order == null) throw new BusinessException("采购单不存在");
    if (order.getStatus() != OrderStatus.DRAFT) throw new BusinessException("当前状态不可提交");
    order.setStatus(OrderStatus.PENDING);
    purchaseOrderMapper.updateById(order);
}
```

- [ ] **步骤 2：新增 `approve()` 方法**

```java
@Transactional(rollbackFor = Exception.class)
public synchronized void approve(Long id) {
    PurchaseOrder order = purchaseOrderMapper.selectById(id);
    if (order == null) throw new BusinessException("采购单不存在");
    if (order.getStatus() != OrderStatus.PENDING) throw new BusinessException("当前状态不可审核");

    List<PurchaseOrderItem> items = purchaseOrderItemMapper.selectList(
            new LambdaQueryWrapper<PurchaseOrderItem>().eq(PurchaseOrderItem::getOrderId, id));

    for (PurchaseOrderItem item : items) {
        // 移动加权平均法：先计算该商品该仓库当前的库存总金额和总数量
        List<Inventory> existingBatches = inventoryMapper.selectList(
                new LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getProductId, item.getProductId())
                        .eq(Inventory::getWarehouseId, order.getWarehouseId()));
        BigDecimal oldTotalValue = BigDecimal.ZERO;
        int oldTotalQty = 0;
        for (Inventory b : existingBatches) {
            if (b.getCostPrice() != null) {
                oldTotalValue = oldTotalValue.add(
                        b.getCostPrice().multiply(BigDecimal.valueOf(b.getQuantity())));
            }
            oldTotalQty += b.getQuantity();
        }

        LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getProductId, item.getProductId())
                .eq(Inventory::getWarehouseId, order.getWarehouseId())
                .eq(item.getBatchNo() != null && !item.getBatchNo().isEmpty(), Inventory::getBatchNo, item.getBatchNo());
        Inventory inv = inventoryMapper.selectOne(wrapper);
        int beforeQty = inv != null ? inv.getQuantity() : 0;
        int afterQty = beforeQty + item.getQuantity();

        if (inv != null) {
            inv.setQuantity(afterQty);
            inventoryMapper.updateById(inv);
        } else {
            inv = new Inventory();
            inv.setProductId(item.getProductId());
            inv.setWarehouseId(order.getWarehouseId());
            inv.setLocationId(order.getLocationId());
            inv.setBatchNo(item.getBatchNo());
            inv.setQuantity(item.getQuantity());
            inv.setLockedQty(0);
            inventoryMapper.insert(inv);
        }

        // 移动加权平均法
        int newTotalQty = oldTotalQty + item.getQuantity();
        BigDecimal newTotalValue = oldTotalValue.add(
                item.getAmount() != null ? item.getAmount() : BigDecimal.ZERO);
        BigDecimal avgPrice = BigDecimal.ZERO;
        if (newTotalQty > 0) {
            avgPrice = newTotalValue.divide(BigDecimal.valueOf(newTotalQty), 4, RoundingMode.HALF_UP);
        }
        List<Inventory> allBatches = inventoryMapper.selectList(
                new LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getProductId, item.getProductId())
                        .eq(Inventory::getWarehouseId, order.getWarehouseId()));
        for (Inventory b : allBatches) {
            b.setCostPrice(avgPrice);
            inventoryMapper.updateById(b);
        }

        InventoryLog log = new InventoryLog();
        log.setProductId(item.getProductId());
        log.setWarehouseId(order.getWarehouseId());
        log.setLocationId(order.getLocationId());
        log.setBatchNo(item.getBatchNo());
        log.setChangeType(OrderStatus.PURCHASE_IN);
        log.setChangeQty(item.getQuantity());
        log.setBeforeQty(beforeQty);
        log.setAfterQty(afterQty);
        log.setUnitPrice(item.getUnitPrice());
        log.setAmount(item.getAmount());
        log.setRefOrderNo(order.getOrderNo());
        log.setOperatorId(order.getOperatorId());
        log.setRemark("采购入库");
        inventoryLogMapper.insert(log);
    }

    order.setStatus(OrderStatus.CONFIRMED);
    purchaseOrderMapper.updateById(order);
}
```

- [ ] **步骤 3：新增 `reject()` 方法**

```java
@Transactional(rollbackFor = Exception.class)
public void reject(Long id, String reason) {
    PurchaseOrder order = purchaseOrderMapper.selectById(id);
    if (order == null) throw new BusinessException("采购单不存在");
    if (order.getStatus() != OrderStatus.PENDING) throw new BusinessException("当前状态不可驳回");
    order.setStatus(OrderStatus.DRAFT);
    order.setRemark((order.getRemark() != null ? order.getRemark() + " | " : "") + "驳回原因: " + (reason != null ? reason : ""));
    purchaseOrderMapper.updateById(order);
}
```

- [ ] **步骤 4：修改 `cancel()` 方法 — 处理 PENDING 状态**

在 `cancel()` 方法中，在 `if (order.getStatus() == OrderStatus.CANCELED)` 判断之后，`if (order.getStatus() == OrderStatus.CONFIRMED)` 之前，新增 PENDING 处理：

```java
// 在现有代码：if (order.getStatus() == OrderStatus.CANCELED) 之后添加：
if (order.getStatus() == OrderStatus.PENDING) {
    // 待审批状态直接取消，无需回滚库存
    order.setStatus(OrderStatus.CANCELED);
    purchaseOrderMapper.updateById(order);
    return;
}
```

- [ ] **步骤 5：提交**

```bash
git add inventory-server/src/main/java/com/inventory/purchase/service/PurchaseOrderService.java
git commit -m "feat: add approval workflow to purchase order service"
```

---

### 任务 3：SalesOrderService 审批流改造

**文件：**
- 修改：`inventory-server/src/main/java/com/inventory/sales/service/SalesOrderService.java`

- [ ] **步骤 1：修改 `submit()` — 只提交到待审批**

```java
@Transactional(rollbackFor = Exception.class)
public synchronized void submit(Long id) {
    SalesOrder order = salesOrderMapper.selectById(id);
    if (order == null) throw new BusinessException("销售单不存在");
    if (order.getStatus() != OrderStatus.DRAFT) throw new BusinessException("当前状态不可提交");
    order.setStatus(OrderStatus.PENDING);
    salesOrderMapper.updateById(order);
}
```

- [ ] **步骤 2：新增 `approve()` 方法**

将原 `submit()` 中的 FIFO 库存扣减逻辑提取到此处（代码与 Task 2 步骤 2 模式一致，但使用 SalesOrder / SalesOrderItem / salesOrderMapper）：

```java
@Transactional(rollbackFor = Exception.class)
public synchronized void approve(Long id) {
    SalesOrder order = salesOrderMapper.selectById(id);
    if (order == null) throw new BusinessException("销售单不存在");
    if (order.getStatus() != OrderStatus.PENDING) throw new BusinessException("当前状态不可审核");

    List<SalesOrderItem> items = salesOrderItemMapper.selectList(
            new LambdaQueryWrapper<SalesOrderItem>().eq(SalesOrderItem::getOrderId, id));

    for (SalesOrderItem item : items) {
        // 查询该商品在该仓库下的所有有库存的批次，按 ID 正序排列（先进先出）
        List<Inventory> batchList = inventoryMapper.selectList(
                new LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getProductId, item.getProductId())
                        .eq(Inventory::getWarehouseId, order.getWarehouseId())
                        .gt(Inventory::getQuantity, 0)
                        .orderByAsc(Inventory::getId));

        int needQty = item.getQuantity();
        int totalAvailable = batchList.stream().mapToInt(Inventory::getQuantity).sum();
        if (totalAvailable < needQty) {
            throw new BusinessException("商品库存不足");
        }

        for (Inventory batch : batchList) {
            if (needQty <= 0) break;
            int deductQty = Math.min(needQty, batch.getQuantity());
            int beforeQty = batch.getQuantity();
            batch.setQuantity(beforeQty - deductQty);
            inventoryMapper.updateById(batch);

            InventoryLog log = new InventoryLog();
            log.setProductId(item.getProductId());
            log.setWarehouseId(order.getWarehouseId());
            log.setLocationId(batch.getLocationId());
            log.setBatchNo(batch.getBatchNo());
            log.setChangeType(OrderStatus.SALES_OUT);
            log.setChangeQty(-deductQty);
            log.setBeforeQty(beforeQty);
            log.setAfterQty(batch.getQuantity());
            log.setUnitPrice(item.getUnitPrice());
            log.setAmount(item.getUnitPrice() != null
                    ? item.getUnitPrice().multiply(BigDecimal.valueOf(deductQty)) : BigDecimal.ZERO);
            log.setRefOrderNo(order.getOrderNo());
            log.setOperatorId(order.getOperatorId());
            log.setRemark("销售出库(FIFO)");
            inventoryLogMapper.insert(log);
            needQty -= deductQty;
        }
    }

    order.setStatus(OrderStatus.CONFIRMED);
    salesOrderMapper.updateById(order);
}
```

- [ ] **步骤 3：新增 `reject()` 方法**

```java
@Transactional(rollbackFor = Exception.class)
public void reject(Long id, String reason) {
    SalesOrder order = salesOrderMapper.selectById(id);
    if (order == null) throw new BusinessException("销售单不存在");
    if (order.getStatus() != OrderStatus.PENDING) throw new BusinessException("当前状态不可驳回");
    order.setStatus(OrderStatus.DRAFT);
    order.setRemark((order.getRemark() != null ? order.getRemark() + " | " : "") + "驳回原因: " + (reason != null ? reason : ""));
    salesOrderMapper.updateById(order);
}
```

- [ ] **步骤 4：修改 `cancel()` — 处理 PENDING 状态**

在 `cancel()` 中，`if (order.getStatus() == OrderStatus.CANCELED)` 之后，`if (order.getStatus() == OrderStatus.CONFIRMED)` 之前添加：

```java
if (order.getStatus() == OrderStatus.PENDING) {
    order.setStatus(OrderStatus.CANCELED);
    salesOrderMapper.updateById(order);
    return;
}
```

- [ ] **步骤 5：提交**

```bash
git add inventory-server/src/main/java/com/inventory/sales/service/SalesOrderService.java
git commit -m "feat: add approval workflow to sales order service"
```

---

### 任务 4：TransferService 审批流改造

**文件：**
- 修改：`inventory-server/src/main/java/com/inventory/transfer/service/TransferService.java`

- [ ] **步骤 1：修改 `submit()` — 只提交到待审批**

```java
@Transactional(rollbackFor = Exception.class)
public synchronized void submit(Long id) {
    InventoryTransfer transfer = transferMapper.selectById(id);
    if (transfer == null) throw new BusinessException("调拨单不存在");
    if (transfer.getStatus() != OrderStatus.DRAFT) throw new BusinessException("当前状态不可提交");
    transfer.setStatus(OrderStatus.PENDING);
    transferMapper.updateById(transfer);
}
```

- [ ] **步骤 2：新增 `approve()` 方法**

将原 `submit()` 中的双向库存变动逻辑提取到此处（源仓库扣减 + 目标仓库增加，含成本价计算）：

```java
@Transactional(rollbackFor = Exception.class)
public synchronized void approve(Long id) {
    InventoryTransfer transfer = transferMapper.selectById(id);
    if (transfer == null) throw new BusinessException("调拨单不存在");
    if (transfer.getStatus() != OrderStatus.PENDING) throw new BusinessException("当前状态不可审核");

    List<InventoryTransferItem> items = transferItemMapper.selectList(
            new LambdaQueryWrapper<InventoryTransferItem>().eq(InventoryTransferItem::getTransferId, id));

    // 扣减源仓库库存
    for (InventoryTransferItem item : items) {
        LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getProductId, item.getProductId())
                .eq(Inventory::getWarehouseId, transfer.getFromWarehouseId())
                .eq(item.getBatchNo() != null && !item.getBatchNo().isEmpty(), Inventory::getBatchNo, item.getBatchNo());
        Inventory inv = inventoryMapper.selectOne(wrapper);
        int available = (inv != null ? inv.getQuantity() : 0) - (inv != null ? inv.getLockedQty() : 0);
        if (available < item.getQuantity()) {
            throw new BusinessException("商品库存不足");
        }
        int beforeQty = inv.getQuantity();
        int afterQty = beforeQty - item.getQuantity();
        inv.setQuantity(afterQty);
        inventoryMapper.updateById(inv);

        InventoryLog outLog = new InventoryLog();
        outLog.setProductId(item.getProductId());
        outLog.setWarehouseId(transfer.getFromWarehouseId());
        outLog.setBatchNo(item.getBatchNo());
        outLog.setChangeType("TRANSFER_OUT");
        outLog.setChangeQty(-item.getQuantity());
        outLog.setBeforeQty(beforeQty);
        outLog.setAfterQty(afterQty);
        outLog.setRefOrderNo(transfer.getOrderNo());
        outLog.setOperatorId(transfer.getOperatorId());
        outLog.setRemark("调拨出库");
        inventoryLogMapper.insert(outLog);
    }

    // 增加目标仓库库存（含移动加权平均成本价计算，代码与原 submit 中的逻辑一致）
    for (InventoryTransferItem item : items) {
        Inventory srcInv = inventoryMapper.selectOne(
                new LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getProductId, item.getProductId())
                        .eq(Inventory::getWarehouseId, transfer.getFromWarehouseId())
                        .last("LIMIT 1"));
        BigDecimal srcCost = srcInv != null && srcInv.getCostPrice() != null
                ? srcInv.getCostPrice() : BigDecimal.ZERO;

        List<Inventory> destBatches = inventoryMapper.selectList(
                new LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getProductId, item.getProductId())
                        .eq(Inventory::getWarehouseId, transfer.getToWarehouseId()));
        BigDecimal destOldValue = BigDecimal.ZERO;
        int destOldQty = 0;
        for (Inventory b : destBatches) {
            if (b.getCostPrice() != null) {
                destOldValue = destOldValue.add(b.getCostPrice().multiply(BigDecimal.valueOf(b.getQuantity())));
            }
            destOldQty += b.getQuantity();
        }

        LambdaQueryWrapper<Inventory> wrapper = new LambdaQueryWrapper<Inventory>()
                .eq(Inventory::getProductId, item.getProductId())
                .eq(Inventory::getWarehouseId, transfer.getToWarehouseId())
                .eq(item.getBatchNo() != null && !item.getBatchNo().isEmpty(), Inventory::getBatchNo, item.getBatchNo());
        Inventory inv = inventoryMapper.selectOne(wrapper);
        int beforeQty = inv != null ? inv.getQuantity() : 0;
        int afterQty = beforeQty + item.getQuantity();

        if (inv != null) {
            inv.setQuantity(afterQty);
            inventoryMapper.updateById(inv);
        } else {
            inv = new Inventory();
            inv.setProductId(item.getProductId());
            inv.setWarehouseId(transfer.getToWarehouseId());
            inv.setBatchNo(item.getBatchNo());
            inv.setQuantity(item.getQuantity());
            inv.setLockedQty(0);
            inventoryMapper.insert(inv);
        }

        int destNewQty = destOldQty + item.getQuantity();
        BigDecimal transferValue = srcCost.multiply(BigDecimal.valueOf(item.getQuantity()));
        BigDecimal destNewValue = destOldValue.add(transferValue);
        BigDecimal destAvg = BigDecimal.ZERO;
        if (destNewQty > 0) {
            destAvg = destNewValue.divide(BigDecimal.valueOf(destNewQty), 4, RoundingMode.HALF_UP);
        }
        List<Inventory> allDestBatches = inventoryMapper.selectList(
                new LambdaQueryWrapper<Inventory>()
                        .eq(Inventory::getProductId, item.getProductId())
                        .eq(Inventory::getWarehouseId, transfer.getToWarehouseId()));
        for (Inventory b : allDestBatches) {
            b.setCostPrice(destAvg);
            inventoryMapper.updateById(b);
        }

        InventoryLog inLog = new InventoryLog();
        inLog.setProductId(item.getProductId());
        inLog.setWarehouseId(transfer.getToWarehouseId());
        inLog.setBatchNo(item.getBatchNo());
        inLog.setChangeType("TRANSFER_IN");
        inLog.setChangeQty(item.getQuantity());
        inLog.setBeforeQty(beforeQty);
        inLog.setAfterQty(afterQty);
        inLog.setRefOrderNo(transfer.getOrderNo());
        inLog.setOperatorId(transfer.getOperatorId());
        inLog.setRemark("调拨入库");
        inventoryLogMapper.insert(inLog);
    }

    transfer.setStatus(OrderStatus.CONFIRMED);
    transferMapper.updateById(transfer);
}
```

- [ ] **步骤 3：新增 `reject()` 方法**

```java
@Transactional(rollbackFor = Exception.class)
public void reject(Long id, String reason) {
    InventoryTransfer transfer = transferMapper.selectById(id);
    if (transfer == null) throw new BusinessException("调拨单不存在");
    if (transfer.getStatus() != OrderStatus.PENDING) throw new BusinessException("当前状态不可驳回");
    transfer.setStatus(OrderStatus.DRAFT);
    transfer.setRemark((transfer.getRemark() != null ? transfer.getRemark() + " | " : "") + "驳回原因: " + (reason != null ? reason : ""));
    transferMapper.updateById(transfer);
}
```

- [ ] **步骤 4：修改 `cancel()` — 处理 PENDING 状态**

在 `cancel()` 中，`if (order.getStatus() == OrderStatus.CANCELED)` 之后，`if (order.getStatus() == OrderStatus.CONFIRMED)` 之前添加：

```java
if (transfer.getStatus() == OrderStatus.PENDING) {
    transfer.setStatus(OrderStatus.CANCELED);
    transferMapper.updateById(transfer);
    return;
}
```

- [ ] **步骤 5：提交**

```bash
git add inventory-server/src/main/java/com/inventory/transfer/service/TransferService.java
git commit -m "feat: add approval workflow to transfer service"
```

---

### 任务 5：Controller 新增审批接口 + 导出状态映射

**文件：**
- 修改：`inventory-server/src/main/java/com/inventory/purchase/controller/PurchaseOrderController.java`
- 修改：`inventory-server/src/main/java/com/inventory/sales/controller/SalesOrderController.java`
- 修改：`inventory-server/src/main/java/com/inventory/transfer/controller/TransferController.java`

- [ ] **步骤 1：PurchaseOrderController 新增 approve/reject 接口并更新导出**

在 `submit()` 方法之后新增：

```java
@SaCheckRole("role_1")
@Operation(summary = "审核通过采购订单")
@PutMapping("/{id}/approve")
public R<Void> approve(@PathVariable Long id) {
    purchaseOrderService.approve(id);
    return R.ok();
}

@SaCheckRole("role_1")
@Operation(summary = "驳回采购订单")
@PutMapping("/{id}/reject")
public R<Void> reject(@PathVariable Long id, @RequestBody Map<String, String> body) {
    purchaseOrderService.reject(id, body.getOrDefault("reason", ""));
    return R.ok();
}
```

更新 `export()` 方法中的状态映射，在 `else if (order.getStatus() == OrderStatus.CANCELED)` 之后添加：

```java
} else if (order.getStatus() == OrderStatus.PENDING) {
    statusText = "待审批";
}
```

- [ ] **步骤 2：SalesOrderController 新增 approve/reject 接口并更新导出**

同样的模式，在 `submit()` 后添加 approve/reject，在 export 状态映射中添加 PENDING。

- [ ] **步骤 3：TransferController 新增 approve/reject 接口并更新导出**

同样的模式，在 `submit()` 后添加 approve/reject，在 export 状态映射中添加 PENDING。

- [ ] **步骤 4：提交**

```bash
git add inventory-server/src/main/java/com/inventory/purchase/controller/PurchaseOrderController.java inventory-server/src/main/java/com/inventory/sales/controller/SalesOrderController.java inventory-server/src/main/java/com/inventory/transfer/controller/TransferController.java
git commit -m "feat: add approve/reject controller endpoints and update export status mapping"
```

---

### 任务 6：仓库分级 — 后端 Entity + Service + Controller

**文件：**
- 修改：`inventory-server/src/main/java/com/inventory/warehouse/entity/Warehouse.java`
- 修改：`inventory-server/src/main/java/com/inventory/warehouse/service/WarehouseService.java`
- 修改：`inventory-server/src/main/java/com/inventory/warehouse/controller/WarehouseController.java`

- [ ] **步骤 1：Warehouse entity 新增 level 和 parentId 字段**

```java
// 在现有 status 字段之后添加：
private Integer level;
private Long parentId;

// 在现有 @TableField(exist = false) 块中添加（用于树形结构前端展示）：
@TableField(exist = false)
private String parentName;

@TableField(exist = false)
private List<Warehouse> children;
```

- [ ] **步骤 2：WarehouseService 新增 tree、children、search 方法**

在 `listAll()` 方法之前新增：

```java
public List<Warehouse> tree() {
    List<Warehouse> all = warehouseMapper.selectList(new LambdaQueryWrapper<Warehouse>()
            .eq(Warehouse::getStatus, 1).orderByAsc(Warehouse::getId));
    // 按 parent_id 构建树
    Map<Long, Warehouse> map = all.stream().collect(Collectors.toMap(Warehouse::getId, w -> w));
    List<Warehouse> roots = new java.util.ArrayList<>();
    for (Warehouse w : all) {
        if (w.getParentId() == null) {
            roots.add(w);
        } else {
            Warehouse parent = map.get(w.getParentId());
            if (parent != null) {
                if (parent.getChildren() == null) parent.setChildren(new java.util.ArrayList<>());
                parent.getChildren().add(w);
            }
        }
    }
    return roots;
}

public List<Warehouse> children(Long parentId) {
    return warehouseMapper.selectList(new LambdaQueryWrapper<Warehouse>()
            .eq(Warehouse::getParentId, parentId)
            .eq(Warehouse::getStatus, 1)
            .orderByAsc(Warehouse::getId));
}

public List<Warehouse> search(String keyword) {
    return warehouseMapper.selectList(new LambdaQueryWrapper<Warehouse>()
            .eq(Warehouse::getStatus, 1)
            .and(w -> w.like(Warehouse::getName, keyword).or().like(Warehouse::getCode, keyword))
            .orderByAsc(Warehouse::getId));
}
```

修改 `listAll()` 方法，改为只返回 level=4 的具体仓库：

```java
public List<Warehouse> listAll() {
    List<Warehouse> list = warehouseMapper.selectList(new LambdaQueryWrapper<Warehouse>()
            .eq(Warehouse::getStatus, 1).eq(Warehouse::getLevel, 4).orderByAsc(Warehouse::getId));
    for (Warehouse w : list) enrichStats(w);
    return list;
}
```

更新 `page()` 方法签名，新增 level 和 parentId 筛选参数：

```java
public Page<Warehouse> page(Page<Warehouse> page, String name, String contact, String phone,
                            String address, Integer status, Integer level, Long parentId) {
    LambdaQueryWrapper<Warehouse> wrapper = new LambdaQueryWrapper<Warehouse>()
            .like(name != null, Warehouse::getName, name)
            .like(contact != null, Warehouse::getContact, contact)
            .like(phone != null, Warehouse::getPhone, phone)
            .like(address != null, Warehouse::getAddress, address)
            .eq(status != null, Warehouse::getStatus, status)
            .eq(level != null, Warehouse::getLevel, level)
            .eq(parentId != null, Warehouse::getParentId, parentId)
            .orderByDesc(Warehouse::getId);
    Page<Warehouse> result = warehouseMapper.selectPage(page, wrapper);
    for (Warehouse w : result.getRecords()) {
        enrichStats(w);
        if (w.getParentId() != null) {
            Warehouse parent = warehouseMapper.selectById(w.getParentId());
            if (parent != null) w.setParentName(parent.getName());
        }
    }
    return result;
}
```

更新 `save()` 方法，仅在新建仓库时生成编码（非仓库节点使用自定义编码）：

```java
@Transactional(rollbackFor = Exception.class)
public void save(Warehouse warehouse) {
    if (warehouse.getLevel() == null || warehouse.getLevel() == 4) {
        warehouse.setCode(generateWarehouseCode());
    }
    warehouseMapper.insert(warehouse);
}
```

- [ ] **步骤 3：WarehouseController 新增接口并更新现有接口**

新增 tree、children、search 接口：

```java
@Operation(summary = "获取仓库树形结构")
@GetMapping("/tree")
public R<List<Warehouse>> tree() {
    return R.ok(warehouseService.tree());
}

@Operation(summary = "获取子级仓库列表")
@GetMapping("/children/{parentId}")
public R<List<Warehouse>> children(@PathVariable Long parentId) {
    return R.ok(warehouseService.children(parentId));
}

@Operation(summary = "搜索仓库")
@GetMapping("/search")
public R<List<Warehouse>> search(@RequestParam String keyword) {
    return R.ok(warehouseService.search(keyword));
}
```

更新 `page()` 接口参数，新增 level 和 parentId：

```java
@Operation(summary = "分页查询仓库")
@GetMapping("/page")
public R<PageResult<Warehouse>> page(
        @RequestParam(defaultValue = "1") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String name,
        @RequestParam(required = false) String contact,
        @RequestParam(required = false) String phone,
        @RequestParam(required = false) String address,
        @RequestParam(required = false) Integer status,
        @RequestParam(required = false) Integer level,
        @RequestParam(required = false) Long parentId) {
    return R.ok(PageResult.of(warehouseService.page(new Page<>(page, size), name, contact, phone, address, status, level, parentId)));
}
```

- [ ] **步骤 4：提交**

```bash
git add inventory-server/src/main/java/com/inventory/warehouse/entity/Warehouse.java inventory-server/src/main/java/com/inventory/warehouse/service/WarehouseService.java inventory-server/src/main/java/com/inventory/warehouse/controller/WarehouseController.java
git commit -m "feat: add warehouse hierarchy (level/parent_id) and tree/search APIs"
```

---

### 任务 7：前端审批流 — 列表页状态展示

**文件：**
- 修改：`inventory-admin/src/views/purchase/PurchaseList.vue`
- 修改：`inventory-admin/src/views/sales/SalesList.vue`
- 修改：`inventory-admin/src/views/transfer/TransferList.vue`

- [ ] **步骤 1：PurchaseList.vue — statusMap 添加 PENDING 状态**

```typescript
const statusMap: Record<number, { label: string; type: string }> = {
  0: { label: '草稿', type: 'info' },
  1: { label: '已入库', type: 'success' },
  2: { label: '已取消', type: 'danger' },
  4: { label: '待审批', type: 'warning' },
}
```

筛选下拉添加"待审批"选项（搜索框中 status select）：

```html
<el-option label="待审批" :value="4" />
```

- [ ] **步骤 2：SalesList.vue — 同样修改 statusMap 和筛选**

```typescript
const statusMap: Record<number, { label: string; type: string }> = {
  0: { label: '草稿', type: 'info' },
  1: { label: '已出库', type: 'success' },
  2: { label: '已取消', type: 'danger' },
  4: { label: '待审批', type: 'warning' },
}
```

- [ ] **步骤 3：TransferList.vue — 同样修改 statusMap 和筛选**

```typescript
const statusMap: Record<number, { label: string; type: string }> = {
  0: { label: '草稿', type: 'info' },
  1: { label: '已完成', type: 'success' },
  2: { label: '已取消', type: 'danger' },
  4: { label: '待审批', type: 'warning' },
}
```

- [ ] **步骤 4：提交**

```bash
git add inventory-admin/src/views/purchase/PurchaseList.vue inventory-admin/src/views/sales/SalesList.vue inventory-admin/src/views/transfer/TransferList.vue
git commit -m "feat: add pending approval status display in list pages"
```

---

### 任务 8：前端审批流 — 详情页审批按钮

**文件：**
- 修改：`inventory-admin/src/views/purchase/PurchaseDetail.vue`
- 修改：`inventory-admin/src/views/sales/SalesDetail.vue`
- 修改：`inventory-admin/src/views/transfer/TransferDetail.vue`

- [ ] **步骤 1：PurchaseDetail.vue — statusMap + 审批按钮**

```typescript
import { useUserStore } from '../../store/user'
const userStore = useUserStore()

const statusMap: Record<number, { label: string; type: string }> = {
  0: { label: '草稿', type: 'info' },
  1: { label: '已入库', type: 'success' },
  2: { label: '已取消', type: 'danger' },
  4: { label: '待审批', type: 'warning' },
}

async function handleApprove() {
  try {
    await ElMessageBox.confirm('确认审核通过此入库单？通过后将更新库存。', '审核确认', { type: 'info' })
  } catch { return }
  await request.put(`/purchase-order/${route.params.id}/approve`)
  ElMessage.success('已审核通过'); fetchDetail()
}

async function handleReject() {
  try {
    const { value } = await ElMessageBox.prompt('确定驳回此入库单？', '驳回', {
      inputPlaceholder: '请填写驳回原因（必填）',
      inputValidator: (val: string) => !!val.trim(),
      inputErrorMessage: '驳回原因不能为空',
      type: 'warning',
    })
    await request.put(`/purchase-order/${route.params.id}/reject`, { reason: value })
    ElMessage.success('已驳回'); fetchDetail()
  } catch {}
}
```

模板中操作按钮区域添加：

```html
<el-button v-if="userStore.isAdmin && order?.status === 4" type="success" @click="handleApprove">审核通过</el-button>
<el-button v-if="userStore.isAdmin && order?.status === 4" type="warning" @click="handleReject">驳回</el-button>
```

将"取消入库"按钮改为在所有非终态可用：
```html
<el-button v-if="order?.status === 0" type="danger" @click="handleCancel">取消入库</el-button>
<el-button v-if="order?.status === 4" type="danger" @click="handleCancel">取消入库</el-button>
```

- [ ] **步骤 2：SalesDetail.vue — 同样的修改**

- [ ] **步骤 3：TransferDetail.vue — 同样的修改**

- [ ] **步骤 4：提交**

```bash
git add inventory-admin/src/views/purchase/PurchaseDetail.vue inventory-admin/src/views/sales/SalesDetail.vue inventory-admin/src/views/transfer/TransferDetail.vue
git commit -m "feat: add approve/reject buttons in detail pages"
```

---

### 任务 9：前端仓库分级 — WarehouseList.vue

**文件：**
- 修改：`inventory-admin/src/views/warehouse/WarehouseList.vue`

- [ ] **步骤 1：搜索区新增级联搜索和关键词搜索**

在 template 搜索区中，原有搜索框之前添加级联筛选：

```html
<el-select v-model="query.level1Id" placeholder="1级大区" clearable style="width:140px" @change="onLevel1Change">
  <el-option v-for="w in level1List" :key="w.id" :label="w.name" :value="w.id" />
</el-select>
<el-select v-model="query.level2Id" placeholder="2级区域" clearable style="width:140px" :disabled="!query.level1Id" @change="onLevel2Change">
  <el-option v-for="w in level2List" :key="w.id" :label="w.name" :value="w.id" />
</el-select>
<el-select v-model="query.level3Id" placeholder="3级城市" clearable style="width:140px" :disabled="!query.level2Id" @change="onLevel3Change">
  <el-option v-for="w in level3List" :key="w.id" :label="w.name" :value="w.id" />
</el-select>
<el-select v-model="query.level4Id" placeholder="4级仓库" clearable style="width:140px" :disabled="!query.level3Id" @change="handleSearch">
  <el-option v-for="w in level4List" :key="w.id" :label="w.name" :value="w.id" />
</el-select>
```

新增仓库名称/编码直接搜索框：
```html
<el-input v-model="query.keyword" placeholder="搜索仓库名称/编码" clearable style="width:180px" @keyup.enter="handleSearch" @clear="handleSearch" />
```

- [ ] **步骤 2：表格新增层级和上级列**

在编码列之后，仓库名称列之后添加：
```html
<el-table-column prop="level" label="层级" width="80">
  <template #default="{ row }">{{ ['','大区','区域','城市','仓库'][row.level] || '-' }}</template>
</el-table-column>
```

在仓库名称之后、地址之前添加：
```html
<el-table-column prop="parentName" label="上级" width="120" />
```

- [ ] **步骤 3：编辑弹窗新增层级和上级选择**

在弹窗表单中添加：
```html
<el-form-item label="层级" required>
  <el-select v-model="form.level" placeholder="选择层级" @change="onLevelChange">
    <el-option label="1级-大区" :value="1" />
    <el-option label="2级-区域" :value="2" />
    <el-option label="3级-城市" :value="3" />
    <el-option label="4级-仓库" :value="4" />
  </el-select>
</el-form-item>
<el-form-item label="上级仓库" v-if="form.level && form.level > 1">
  <el-select v-model="form.parentId" placeholder="选择上级" filterable>
    <el-option v-for="w in parentCandidates" :key="w.id" :label="w.name" :value="w.id" />
  </el-select>
</el-form-item>
```

- [ ] **步骤 4：script 新增对应逻辑**

```typescript
// 新增响应式数据
const level1List = ref<Warehouse[]>([])
const level2List = ref<Warehouse[]>([])
const level3List = ref<Warehouse[]>([])
const level4List = ref<Warehouse[]>([])
const parentCandidates = ref<Warehouse[]>([])

// 查询条件新增
query: {
  ...原有的,
  level1Id: undefined,
  level2Id: undefined,
  level3Id: undefined,
  level4Id: undefined,
  keyword: '',
}

// 级联方法
async function onLevel1Change(val: number | undefined) {
  query.level2Id = undefined; query.level3Id = undefined; query.level4Id = undefined;
  level2List.value = []; level3List.value = []; level4List.value = [];
  if (val) { const res = await request.get('/warehouse/children/' + val); level2List.value = res.data.data }
  handleSearch()
}
async function onLevel2Change(val: number | undefined) {
  query.level3Id = undefined; query.level4Id = undefined;
  level3List.value = []; level4List.value = [];
  if (val) { const res = await request.get('/warehouse/children/' + val); level3List.value = res.data.data }
  handleSearch()
}
async function onLevel3Change(val: number | undefined) {
  query.level4Id = undefined; level4List.value = [];
  if (val) { const res = await request.get('/warehouse/children/' + val); level4List.value = res.data.data }
  handleSearch()
}
async function onLevelChange(level: number) {
  form.parentId = undefined
  if (level > 1) {
    const levelMap = { 2: 1, 3: 2, 4: 3 }
    const res = await request.get('/warehouse/page', { params: { page: 1, size: 999, level: levelMap[level as keyof typeof levelMap] } })
    parentCandidates.value = res.data.data.records
  }
}

// 初始化加载1级
onMounted(async () => {
  const res = await request.get('/warehouse/page', { params: { page: 1, size: 999, level: 1 } })
  level1List.value = res.data.data.records
  fetchData()
})
```

- [ ] **步骤 5：提交**

```bash
git add inventory-admin/src/views/warehouse/WarehouseList.vue
git commit -m "feat: add warehouse hierarchy cascade search and edit"
```

---

### 任务 10：前端仓库分级 — 单据页仓库选择器

**文件：**
- 修改：`inventory-admin/src/views/purchase/PurchaseForm.vue`
- 修改：`inventory-admin/src/views/sales/SalesForm.vue`
- 修改：`inventory-admin/src/views/transfer/TransferForm.vue`
- 修改：`inventory-admin/src/views/stocktake/StockTakeForm.vue`

- [ ] **步骤 1：PurchaseForm.vue — 仓库选择改为级联 + 搜索**

将现有的仓库下拉选择器替换为级联选择器：

```html
<!-- 替换原有的仓库 select -->
<el-cascader
  v-model="form.warehousePath"
  :options="warehouseTree"
  :props="{ value: 'id', label: 'name', children: 'children', emitPath: false, checkStrictly: false }"
  placeholder="选择仓库"
  filterable
  clearable
  style="width:100%"
  @change="onWarehouseChange"
/>
```

在 script 中：

```typescript
const warehouseTree = ref<any[]>([])
// 移除 warehouses 的原有赋值方式

async function fetchWarehouseTree() {
  const res = await request.get('/warehouse/tree')
  warehouseTree.value = res.data.data
}

function onWarehouseChange(val: number) {
  form.warehouseId = val
}

// 在 fetchBaseData 中加入 fetchWarehouseTree
```

- [ ] **步骤 2：SalesForm.vue、TransferForm.vue、StockTakeForm.vue — 同样的改造**

TransferForm 需要两个级联选择器（调出仓库、调入仓库），分别绑定 `form.outWarehouseId` / `form.inWarehouseId`。

- [ ] **步骤 3：提交**

```bash
git add inventory-admin/src/views/purchase/PurchaseForm.vue inventory-admin/src/views/sales/SalesForm.vue inventory-admin/src/views/transfer/TransferForm.vue inventory-admin/src/views/stocktake/StockTakeForm.vue
git commit -m "feat: replace warehouse select with cascade tree in order forms"
```

---

### 任务 11：单元测试更新

**文件：**
- 修改：`inventory-server/src/test/java/com/inventory/purchase/PurchaseOrderServiceTest.java`
- 修改：`inventory-server/src/test/java/com/inventory/sales/SalesOrderServiceTest.java`
- 修改：`inventory-server/src/test/java/com/inventory/stocktake/StockTakeServiceTest.java`
- 修改：`inventory-server/src/test/java/com/inventory/transfer/TransferServiceTest.java`

- [ ] **步骤 1：更新 PurchaseOrderServiceTest**

将原有测试 submit 后直接检查 CONFIRMED 改为两步：submit 后检查 PENDING，approve 后检查 CONFIRMED：

```java
// 修改测试方法
@Test
void testSubmitThenApprove() {
    // 创建入库单
    PurchaseOrder order = new PurchaseOrder();
    order.setWarehouseId(1L);
    order.setItems(List.of(createItem(1L, 10, new BigDecimal("10.00"))));
    Long id = purchaseOrderService.create(order);

    // 提交后应为待审批
    purchaseOrderService.submit(id);
    PurchaseOrder submitted = purchaseOrderMapper.selectById(id);
    assertEquals(4, submitted.getStatus());  // PENDING

    // 审核通过后应为已入库
    purchaseOrderService.approve(id);
    PurchaseOrder approved = purchaseOrderMapper.selectById(id);
    assertEquals(1, approved.getStatus());  // CONFIRMED

    // 验证库存已更新
    Inventory inv = inventoryMapper.selectOne(...);
    assertNotNull(inv);
    assertEquals(10, inv.getQuantity());
}

@Test
void testRejectReturnsToDraft() {
    // 创建并提交
    Long id = createAndSubmitOrder();
    
    // 驳回
    purchaseOrderService.reject(id, "价格不合理");
    PurchaseOrder rejected = purchaseOrderMapper.selectById(id);
    assertEquals(0, rejected.getStatus());  // DRAFT
    assertTrue(rejected.getRemark().contains("驳回原因"));
}
```

- [ ] **步骤 2：更新 SalesOrderServiceTest 和 TransferServiceTest**

同样的两步测试模式：submit → PENDING，approve → CONFIRMED，并验证库存。

- [ ] **步骤 3：运行测试验证全部通过**

```bash
cd inventory-server && mvn test -Dtest=PurchaseOrderServiceTest,SalesOrderServiceTest,StockTakeServiceTest,TransferServiceTest
```
预期：全部通过（16+ 个测试）

- [ ] **步骤 4：提交**

```bash
git add inventory-server/src/test/java/com/inventory/purchase/PurchaseOrderServiceTest.java inventory-server/src/test/java/com/inventory/sales/SalesOrderServiceTest.java inventory-server/src/test/java/com/inventory/stocktake/StockTakeServiceTest.java inventory-server/src/test/java/com/inventory/transfer/TransferServiceTest.java
git commit -m "test: update tests for approval workflow (submit→PENDING, approve→CONFIRMED)"
```

---

### 任务 12：小程序仓库选择器改造（如果存在）

**文件：**
- 修改：`inventory-miniapp/src/pages/purchase/create.vue`
- 修改：`inventory-miniapp/src/pages/purchase/detail.vue`
- 修改：`inventory-miniapp/src/pages/sales/list.vue`（等涉及仓库选择的页面）

- [ ] **步骤 1：检查小程序是否存在仓库选择**

运行：`grep -rn "warehouse\|仓库" inventory-miniapp/src/pages/ --include="*.vue" -l`

如果存在，替换为级联选择器模式。

- [ ] **步骤 2：提交**

```bash
git add inventory-miniapp/src/pages/...  # 实际改动的文件
git commit -m "feat: update miniapp warehouse selector to cascade tree"
```
