# P1/P2 功能实现计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 完成进销存管理系统 P1（功能补齐）+ P2（体验优化）的 6 项开发任务

**前置知识：** 导出功能（4 个 Controller + 4 个前端页面）已全部完成，无需额外工作

**Tech Stack:** Java 17, Spring Boot 3.2.5, MyBatis-Plus, Vue 3.5 + Element Plus, EasyExcel 3.3

---

## 文件变更总览

| 操作 | 文件 |
|------|------|
| 创建 | `FileController.java` — 图片上传接口 |
| 创建 | `SysConfig.java` — 系统配置实体 |
| 创建 | `SysConfigMapper.java` — 系统配置 Mapper |
| 创建 | `SysConfigService.java` — 系统配置 Service |
| 创建 | `SysConfigController.java` — 系统配置 Controller |
| 创建 | `ConfigList.vue` — 系统配置页面 |
| 修改 | `ProductController.java` — 添加 `/import` 端点 |
| 修改 | `WebMvcConfig.java` — 添加静态资源配置 |
| 修改 | `SalesOrderService.java` — FIFO 出库逻辑 |
| 修改 | `ReportService.java` — 真实报表查询 |
| 修改 | `ProductList.vue` — 导入弹窗 + 图片上传 + 缩略图列 |
| 修改 | `SalesForm.vue` — 批次库存信息展示 |
| 修改 | `ReportView.vue` — 真实图表数据 + 周转率 |
| 修改 | `PurchaseDetail.vue` — 打印功能 |
| 修改 | `router/index.ts` — 添加系统配置路由 |
| 修改 | `01-schema.sql` — 添加 sys_config 表 |

---

### Task 1: 商品 Excel 导入（后端）

**Files:**
- Modify: `inventory-server/.../product/controller/ProductController.java` — 添加导入端点

- [ ] **Step 1: ProductController 新增 /import 端点**

在 `ProductController` 中现有 `/export` 同级添加：

```java
@Operation(summary = "导入商品")
@PostMapping("/import")
public R<Integer> importExcel(@RequestParam("file") MultipartFile file) throws IOException {
    List<Product> list = EasyExcel.read(file.getInputStream())
            .head(ProductImportVO.class)
            .sheet()
            .doReadSync();
    int count = 0;
    for (ProductImportVO vo : list) {
        Product p = new Product();
        p.setCode(vo.getCode());
        p.setName(vo.getName());
        p.setSpec(vo.getSpec());
        p.setUnit(vo.getUnit());
        p.setPurchasePrice(vo.getPurchasePrice());
        p.setSalePrice(vo.getSalePrice());
        p.setMinStock(vo.getMinStock() != null ? vo.getMinStock() : 0);
        p.setStatus(1);
        productService.save(p);
        count++;
    }
    return R.ok(count);
}
```

- [ ] **Step 2: 创建 ProductImportVO 类**

创建 `inventory-server/src/main/java/com/inventory/product/entity/ProductImportVO.java`:

```java
package com.inventory.product.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductImportVO {
    @ExcelProperty("编码")
    private String code;

    @ExcelProperty("名称")
    private String name;

    @ExcelProperty("规格")
    private String spec;

    @ExcelProperty("单位")
    private String unit;

    @ExcelProperty("采购价")
    private BigDecimal purchasePrice;

    @ExcelProperty("销售价")
    private BigDecimal salePrice;

    @ExcelProperty("最低库存")
    private Integer minStock;
}
```

- [ ] **Step 3: 验证导入端点编译通过**

Run: `cd inventory-server && mvn compile -q`

---

### Task 2: 商品 Excel 导入（前端）

**Files:**
- Modify: `inventory-admin/src/views/product/ProductList.vue` — 添加导入弹窗

- [ ] **Step 1: ProductList.vue 添加导入相关状态和函数**

在 script 中 `handleExport` 函数下方添加：

```typescript
const importDialogVisible = ref(false)
const importFile = ref<File | null>(null)
const importing = ref(false)

function handleImport() {
  importDialogVisible.value = true
  importFile.value = null
}

async function handleImportSubmit() {
  if (!importFile.value) { ElMessage.warning('请选择文件'); return }
  importing.value = true
  try {
    const formData = new FormData()
    formData.append('file', importFile.value)
    await request.post('/product/import', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
    ElMessage.success('导入成功')
    importDialogVisible.value = false
    fetchData()
  } catch { /* handled */ } finally { importing.value = false }
}
```

- [ ] **Step 2: ProductList.vue 添加导入按钮和弹窗模板**

在 page-header 中导出按钮旁边添加：

```html
<el-button @click="handleImport">导入Excel</el-button>
```

在底部表单弹窗之后添加导入弹窗：

```html
<el-dialog v-model="importDialogVisible" title="导入商品" width="400px">
  <el-upload
    drag
    :auto-upload="false"
    :limit="1"
    accept=".xlsx,.xls"
    @change="(file: any) => importFile = file.raw"
  >
    <el-icon style="font-size:48px;color:#999"><UploadFilled /></el-icon>
    <div style="margin-top:8px">拖拽或点击选择 Excel 文件</div>
    <template #tip><div style="font-size:12px;color:#999;margin-top:4px">仅支持 .xlsx 格式</div></template>
  </el-upload>
  <template #footer>
    <el-button @click="importDialogVisible = false">取消</el-button>
    <el-button type="primary" :loading="importing" @click="handleImportSubmit">导入</el-button>
  </template>
</el-dialog>
```

---

### Task 3: 商品图片上传（后端）

**Files:**
- Create: `inventory-server/.../common/controller/FileController.java`
- Modify: `inventory-server/.../common/config/WebMvcConfig.java`

- [ ] **Step 1: 创建 FileController**

Create `inventory-server/src/main/java/com/inventory/common/controller/FileController.java`:

```java
package com.inventory.common.controller;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.inventory.common.result.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;

@Tag(name = "文件管理")
@RestController
@RequestMapping("/api/v1/file")
public class FileController {

    @Value("${file.upload-path:uploads/images}")
    private String uploadPath;

    @PostConstruct
    public void init() {
        File dir = new File(uploadPath);
        if (!dir.exists()) dir.mkdirs();
    }

    @Operation(summary = "上传图片")
    @PostMapping("/upload")
    public R<String> upload(@RequestParam("file") MultipartFile file) throws IOException {
        String ext = FileUtil.extName(file.getOriginalFilename());
        if (!"jpg,jpeg,png,gif".contains(ext.toLowerCase())) {
            return R.fail("仅支持 jpg/png/gif 格式");
        }
        String fileName = IdUtil.fastSimpleUUID() + "." + ext;
        File dest = new File(uploadPath, fileName);
        file.transferTo(dest);
        return R.ok("/" + uploadPath.replace("\\", "/") + "/" + fileName);
    }
}
```

- [ ] **Step 2: WebMvcConfig 添加静态资源映射**

在 WebMvcConfig 类中添加：

```java
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

public class WebMvcConfig implements WebMvcConfigurer {
    // ... 现有 corsFilter bean ...

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/");
    }
}
```

将类声明从 `public class WebMvcConfig` 改为 `public class WebMvcConfig implements WebMvcConfigurer`

- [ ] **Step 3: application.yml 添加文件大小限制**

在 `spring:` 下添加：

```yaml
  servlet:
    multipart:
      max-file-size: 5MB
      max-request-size: 10MB
```

---

### Task 4: 商品图片上传（前端）

**Files:**
- Modify: `inventory-admin/src/views/product/ProductList.vue`

- [ ] **Step 1: 商品表单增加图片上传**

在商品编辑弹窗中，在"备注"字段之前添加：

```html
<el-form-item label="商品图片">
  <el-upload
    :action="'/api/v1/file/upload'"
    :headers="{ Authorization: 'Bearer ' + (localStorage.getItem('token') || '') }"
    :on-success="(res: any) => { if (res.code === 200) form.imageUrl = res.data }"
    :on-remove="() => form.imageUrl = ''"
    :file-list="form.imageUrl ? [{ name: '商品图片', url: form.imageUrl }] : []"
    list-type="picture"
    limit="1"
  >
    <el-button size="small">上传图片</el-button>
    <template #tip><div style="font-size:12px;color:#999">建议尺寸 200x200，不超过 5MB</div></template>
  </el-upload>
</el-form-item>
```

- [ ] **Step 2: 商品列表增加缩略图列**

在表格 `el-table-column` 的"编码"列之后添加：

```html
<el-table-column label="图片" width="70">
  <template #default="{ row }">
    <el-image v-if="row.imageUrl" :src="row.imageUrl" style="width:40px;height:40px;border-radius:4px" fit="cover" />
    <span v-else style="color:#ccc">-</span>
  </template>
</el-table-column>
```

---

### Task 5: FIFO 出库策略（后端）

**Files:**
- Modify: `inventory-server/.../sales/service/SalesOrderService.java` — 修改 submit() 方法

- [ ] **Step 1: 替换 submit() 方法中的库存扣减逻辑**

将 submit() 方法中 `for (SalesOrderItem item : items)` 的循环体替换为 FIFO 逻辑：

```java
for (SalesOrderItem item : items) {
    // 查询该商品在该仓库下的所有批次库存，按入库时间正序排列
    List<Inventory> batchList = inventoryMapper.selectList(
            new LambdaQueryWrapper<Inventory>()
                    .eq(Inventory::getProductId, item.getProductId())
                    .eq(Inventory::getWarehouseId, order.getWarehouseId())
                    .gt(Inventory::getQuantity, 0)
                    .orderByAsc(Inventory::getId));

    int needQty = item.getQuantity();
    if (batchList.stream().mapToInt(Inventory::getQuantity).sum() < needQty) {
        throw new BusinessException("商品「" + item.getProductName() + "」库存不足");
    }

    for (Inventory batch : batchList) {
        if (needQty <= 0) break;

        int deductQty = Math.min(needQty, batch.getQuantity());
        int beforeQty = batch.getQuantity();

        batch.setQuantity(beforeQty - deductQty);
        inventoryMapper.updateById(batch);

        // 记录流水——按批次逐条记录
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
```

需要在文件头部导入 `java.util.List`（如果尚未导入）。

---

### Task 6: FIFO 出库（前端 — 批次库存展示）

**Files:**
- Modify: `inventory-admin/src/views/sales/SalesForm.vue`

- [ ] **Step 1: 添加批次库存查询和展示**

在 script 中添加：

```typescript
const batchInventory = ref<Record<number, Array<{batchNo: string; quantity: number}>>>({})

async function fetchBatchInventory(productId: number, warehouseId: number) {
  if (!productId || !warehouseId) return
  try {
    const res = await request.get('/inventory/page', {
      params: { productId, warehouseId, page: 1, size: 100 }
    })
    batchInventory.value[productId] = res.data.data.records
      .filter((r: any) => r.batchNo && r.quantity > 0)
      .map((r: any) => ({ batchNo: r.batchNo, quantity: r.quantity }))
  } catch { /* ignore */ }
}

// 在商品选择时查询批次库存
function onProductSelect(index: number) {
  const item = form.items[index]
  if (item.productId && form.warehouseId) {
    fetchBatchInventory(item.productId, form.warehouseId)
  }
}
```

在商品选择列的 `<el-select>` 上添加 `@change="() => onProductSelect($index)"`。

在表头"批次号"列中，添加批次库存下拉提示：

```html
<el-table-column label="批次号/库存" width="200">
  <template #default="{ $index }">
    <el-select v-model="form.items[$index].batchNo" filterable allow-create clearable placeholder="自动FIFO" style="width:100%">
      <el-option
        v-for="b in (batchInventory[form.items[$index].productId || ''] || [])"
        :key="b.batchNo"
        :label="`${b.batchNo} (余${b.quantity})`"
        :value="b.batchNo"
      />
    </el-select>
  </template>
</el-table-column>
```

---

### Task 7: 库存周转报表（后端）

**Files:**
- Modify: `inventory-server/.../report/service/ReportService.java`

- [ ] **Step 1: 替换 ReportService 中的空实现**

将 `purchaseSummary()` 和 `salesSummary()` 方法替换为真实查询：

```java
public Map<String, Object> purchaseSummary(int days) {
    Map<String, Object> result = new HashMap<>();
    LocalDate since = LocalDate.now().minusDays(days);

    List<PurchaseOrder> orders = purchaseOrderMapper.selectList(
            new LambdaQueryWrapper<PurchaseOrder>()
                    .eq(PurchaseOrder::getStatus, 1)
                    .ge(PurchaseOrder::getOrderDate, since)
                    .orderByAsc(PurchaseOrder::getOrderDate));

    List<String> labels = new ArrayList<>();
    List<Integer> inQty = new ArrayList<>();
    List<BigDecimal> inAmt = new ArrayList<>();

    Map<LocalDate, List<PurchaseOrder>> grouped = orders.stream()
            .collect(Collectors.groupingBy(PurchaseOrder::getOrderDate));
    List<LocalDate> sortedDates = new ArrayList<>(grouped.keySet());
    Collections.sort(sortedDates);

    for (LocalDate date : sortedDates) {
        List<PurchaseOrder> dayOrders = grouped.get(date);
        labels.add(date.toString());
        inQty.add(dayOrders.stream().mapToInt(PurchaseOrder::getTotalQuantity).sum());
        inAmt.add(dayOrders.stream().map(PurchaseOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    result.put("labels", labels);
    result.put("inQty", inQty);
    result.put("inAmt", inAmt);
    result.put("totalOrders", orders.size());
    return result;
}

public Map<String, Object> salesSummary(int days) {
    Map<String, Object> result = new HashMap<>();
    LocalDate since = LocalDate.now().minusDays(days);

    List<SalesOrder> orders = salesOrderMapper.selectList(
            new LambdaQueryWrapper<SalesOrder>()
                    .eq(SalesOrder::getStatus, 1)
                    .ge(SalesOrder::getOrderDate, since)
                    .orderByAsc(SalesOrder::getOrderDate));

    List<String> labels = new ArrayList<>();
    List<Integer> outQty = new ArrayList<>();
    List<BigDecimal> outAmt = new ArrayList<>();

    Map<LocalDate, List<SalesOrder>> grouped = orders.stream()
            .collect(Collectors.groupingBy(SalesOrder::getOrderDate));
    List<LocalDate> sortedDates = new ArrayList<>(grouped.keySet());
    Collections.sort(sortedDates);

    for (LocalDate date : sortedDates) {
        List<SalesOrder> dayOrders = grouped.get(date);
        labels.add(date.toString());
        outQty.add(dayOrders.stream().mapToInt(SalesOrder::getTotalQuantity).sum());
        outAmt.add(dayOrders.stream().map(SalesOrder::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    result.put("labels", labels);
    result.put("outQty", outQty);
    result.put("outAmt", outAmt);
    result.put("totalOrders", orders.size());
    return result;
}
```

需要添加 import：
```java
import java.util.ArrayList;
import java.util.Collections;
import java.util.stream.Collectors;
```

---

### Task 8: 库存周转报表（前端）

**Files:**
- Modify: `inventory-admin/src/views/report/ReportView.vue`

- [ ] **Step 1: 替换 mock 数据为真实数据**

移除 `mockDays` 和 `mockData` 常量。修改 `renderChart()` 方法：

```typescript
function renderChart() {
  if (!chartContainer.value) return
  if (chart) chart.dispose()
  chart = echarts.init(chartContainer.value)

  const data = tab.value === 'purchase' ? purchaseData.value : salesData.value
  const labels = data.labels || []
  const seriesData = tab.value === 'purchase' ? (data.inQty || []) : (data.outQty || [])
  const amountData = tab.value === 'purchase' ? (data.inAmt || []) : (data.outAmt || [])

  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['数量', '金额'] },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      data: labels,
      axisLabel: { fontSize: 11, color: '#999' },
    },
    yAxis: [
      { type: 'value', name: '数量', minInterval: 1 },
      { type: 'value', name: '金额(元)', minInterval: 1 },
    ],
    series: [
      {
        name: '数量',
        type: 'bar',
        data: seriesData,
        itemStyle: { color: '#2e7d32', borderRadius: [4, 4, 0, 0] },
        barWidth: '60%',
      },
    ],
  })
}
```

- [ ] **Step 2: 添加库存周转率 Tabs**

在 `<el-tabs>` 中添加"周转率"tab：

```html
<el-tab-pane label="周转率" name="turnover">
  <div v-if="turnoverData.length">
    <el-table :data="turnoverData" stripe border>
      <el-table-column prop="productName" label="商品名称" min-width="180" />
      <el-table-column prop="totalOut" label="出库总量" width="100" />
      <el-table-column prop="avgInventory" label="平均库存" width="100" />
      <el-table-column prop="turnoverRate" label="周转率" width="100">
        <template #default="{ row }">{{ row.turnoverRate?.toFixed(2) }}</template>
      </el-table-column>
    </el-table>
  </div>
  <el-empty v-else description="暂无数据" />
</el-tab-pane>
```

在 script 中添加：
```typescript
const turnoverData = ref<any[]>([])

async function fetchTurnover() {
  try {
    const res = await request.get('/report/turnover-rate', { params: { days: 30 } })
    turnoverData.value = res.data.data || []
  } catch { /* ignore */ }
}

watch(tab, (val) => {
  if (val === 'turnover') { nextTick(() => fetchTurnover()) }
  else { nextTick(() => renderChart()) }
})

onMounted(async () => {
  await fetchReports()
  nextTick(() => renderChart())
})
```

- [ ] **Step 3: ReportController 新增周转率接口**

在 `ReportController` 中添加：

```java
@Operation(summary = "库存周转率")
@GetMapping("/turnover-rate")
public R<List<Map<String, Object>>> turnoverRate(@RequestParam(defaultValue = "30") int days) {
    return R.ok(reportService.turnoverRate(days));
}
```

在 `ReportService` 中添加：

```java
public List<Map<String, Object>> turnoverRate(int days) {
    LocalDate since = LocalDate.now().minusDays(days);
    List<Map<String, Object>> result = new ArrayList<>();

    // 查询所有出库明细
    List<SalesOrder> orders = salesOrderMapper.selectList(
            new LambdaQueryWrapper<SalesOrder>()
                    .eq(SalesOrder::getStatus, 1)
                    .ge(SalesOrder::getOrderDate, since));
    Map<Long, Integer> productOutQty = new HashMap<>();
    for (SalesOrder order : orders) {
        List<SalesOrderItem> items = salesOrderItemMapper.selectList(
                new LambdaQueryWrapper<SalesOrderItem>().eq(SalesOrderItem::getOrderId, order.getId()));
        for (SalesOrderItem item : items) {
            productOutQty.merge(item.getProductId(), item.getQuantity(), Integer::sum);
        }
    }

    // 查询当前库存
    List<Inventory> inventoryList = inventoryMapper.selectList(null);
    Map<Long, Integer> productInventory = new HashMap<>();
    for (Inventory inv : inventoryList) {
        productInventory.merge(inv.getProductId(), inv.getQuantity(), Integer::sum);
    }

    // 计算周转率
    for (Map.Entry<Long, Integer> entry : productOutQty.entrySet()) {
        Long pid = entry.getKey();
        int outQty = entry.getValue();
        int avgInv = productInventory.getOrDefault(pid, 0);
        if (avgInv == 0) continue;

        Map<String, Object> item = new HashMap<>();
        Product p = productMapper.selectById(pid);
        item.put("productName", p != null ? p.getName() : "未知");
        item.put("totalOut", outQty);
        item.put("avgInventory", avgInv);
        item.put("turnoverRate", (double) outQty / avgInv);
        result.add(item);
    }
    result.sort((a, b) -> Double.compare(
            ((Number) b.get("turnoverRate")).doubleValue(),
            ((Number) a.get("turnoverRate")).doubleValue()));
    return result;
}
```

在 `ReportService` 中需要注入 `SalesOrderItemMapper`：

```java
private final SalesOrderItemMapper salesOrderItemMapper;
private final InventoryMapper inventoryMapper;
```

并在构造函数或 `@RequiredArgsConstructor` 中注入（如果已使用 Lombok，直接添加字段即可）。

---

### Task 9: 入库单打印（前端）

**Files:**
- Modify: `inventory-admin/src/views/purchase/PurchaseDetail.vue`

- [ ] **Step 1: 添加打印按钮和样式**

在页面头部"返回列表"按钮旁添加：

```html
<el-button @click="handlePrint">打印</el-button>
```

在 script 中添加：

```typescript
function handlePrint() {
  window.print()
}
```

在模板末尾（或单独 style 块）添加打印样式：

```html
<style>
@media print {
  /* 隐藏侧边栏、顶部导航、按钮 */
  .sidebar, .navbar, .page-header div, .el-button, .el-pagination, .footer { display: none !important; }
  /* 只显示内容区 */
  .detail-card { box-shadow: none !important; border: 1px solid #ddd !important; }
  body { background: white !important; }
  @page { margin: 1.5cm; }
}
</style>
```

---

### Task 10: 系统配置（后端）

**Files:**
- Modify: `sql/01-schema.sql` — 新增系统配置表
- Create: `SysConfig.java`
- Create: `SysConfigMapper.java`
- Create: `SysConfigService.java`
- Create: `SysConfigController.java`

- [ ] **Step 1: 在 01-schema.sql 末尾添加 sys_config 表**

```sql
CREATE TABLE `sys_config` (
  `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `config_key`  VARCHAR(100) NOT NULL COMMENT '配置键',
  `config_value` VARCHAR(500) NOT NULL COMMENT '配置值',
  `remark`      VARCHAR(200) NULL     COMMENT '说明',
  `create_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- 默认配置
INSERT INTO `sys_config` (`config_key`, `config_value`, `remark`) VALUES
('purchase_order_prefix', 'PO', '入库单前缀'),
('sales_order_prefix', 'SO', '出库单前缀'),
('stocktake_order_prefix', 'CK', '盘点单前缀'),
('alert_enabled', 'true', '安全库存预警开关');
```

- [ ] **Step 2: 创建 SysConfig 实体**

`inventory-server/src/main/java/com/inventory/system/entity/SysConfig.java`:

```java
package com.inventory.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_config")
public class SysConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String configKey;
    private String configValue;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
```

- [ ] **Step 3: 创建 SysConfigMapper**

`inventory-server/src/main/java/com/inventory/system/mapper/SysConfigMapper.java`:

```java
package com.inventory.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.inventory.system.entity.SysConfig;

public interface SysConfigMapper extends BaseMapper<SysConfig> {
}
```

- [ ] **Step 4: 创建 SysConfigService**

`inventory-server/src/main/java/com/inventory/system/service/SysConfigService.java`:

```java
package com.inventory.system.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.inventory.system.entity.SysConfig;
import com.inventory.system.mapper.SysConfigMapper;
import org.springframework.stereotype.Service;

@Service
public class SysConfigService extends ServiceImpl<SysConfigMapper, SysConfig> {
}
```

- [ ] **Step 5: 创建 SysConfigController**

`inventory-server/src/main/java/com/inventory/system/controller/SysConfigController.java`:

```java
package com.inventory.system.controller;

import com.inventory.common.result.R;
import com.inventory.system.entity.SysConfig;
import com.inventory.system.service.SysConfigService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "系统配置")
@RestController
@RequestMapping("/api/v1/system/config")
@RequiredArgsConstructor
public class SysConfigController {

    private final SysConfigService sysConfigService;

    @Operation(summary = "获取全部配置")
    @GetMapping
    public R<List<SysConfig>> list() {
        return R.ok(sysConfigService.list());
    }

    @Operation(summary = "更新配置")
    @PutMapping("/{id}")
    public R<Void> update(@PathVariable Long id, @RequestBody SysConfig config) {
        config.setId(id);
        sysConfigService.updateById(config);
        return R.ok();
    }
}
```

---

### Task 11: 系统配置（前端）

**Files:**
- Create: `inventory-admin/src/views/system/ConfigList.vue`
- Modify: `inventory-admin/src/router/index.ts`

- [ ] **Step 1: 创建 ConfigList.vue**

`inventory-admin/src/views/system/ConfigList.vue`:

```vue
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '../../api/request'
import { ElMessage } from 'element-plus'

interface SysConfig { id: number; configKey: string; configValue: string; remark: string; createTime: string; updateTime: string }

const loading = ref(false)
const list = ref<SysConfig[]>([])

async function fetchData() {
  loading.value = true
  try { const res = await request.get('/system/config'); list.value = res.data.data } finally { loading.value = false }
}

async function handleSave(row: SysConfig) {
  await request.put(`/system/config/${row.id}`, { configValue: row.configValue })
  ElMessage.success('已更新')
}

onMounted(fetchData)
</script>

<template>
  <div>
    <div class="page-header"><h2>系统配置</h2></div>
    <div class="table-container">
      <el-table :data="list" v-loading="loading" stripe border>
        <el-table-column prop="configKey" label="配置项" width="200">
          <template #default="{ row }">
            <el-tag>{{ row.configKey }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="configValue" label="配置值" min-width="200">
          <template #default="{ row }">
            <el-input v-model="row.configValue" size="small" style="width:200px" />
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="说明" min-width="200" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="handleSave(row)">保存</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>
```

- [ ] **Step 2: router/index.ts 添加系统配置路由**

在 `/system/log` 路由同级添加：

```typescript
{
  path: 'system/config',
  name: 'SysConfig',
  component: () => import('../views/system/ConfigList.vue'),
  meta: { title: '系统配置', requireAdmin: true },
},
```

---

## 自检清单

- ✅ **所有导出功能已完成**（后端 4 端点 + 前端 4 按钮）
- ✅ 每个任务的改动独立，互不阻塞，可按任意顺序执行
- ✅ 所有代码片段包含完整实现，无 TBD/占位符
- ✅ 前后端类型和方法签名一致
- ✅ 无多余功能，严格限定在 P1/P2 范围内
