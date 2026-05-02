<script setup lang="ts">
import { ref, onMounted, reactive, computed } from 'vue'
import request, { downloadFile } from '../../api/request'
import type { Inventory, PageParams, Warehouse } from '../../types/api'
import { useRouter } from 'vue-router'

const router = useRouter()
const loading = ref(false)
const allList = ref<Inventory[]>([])
const warehouses = ref<Warehouse[]>([])

const query = reactive<PageParams & { productName?: string; warehouseId?: number }>({
  page: 1, size: 999, productName: '', warehouseId: undefined,
})

async function fetchData() {
  loading.value = true
  try {
    const res = await request.get('/inventory/page', { params: query })
    allList.value = res.data.data.records || []
  } finally { loading.value = false }
}

async function fetchWarehouses() {
  const res = await request.get('/warehouse/list')
  warehouses.value = res.data.data
}

function handleSearch() { query.page = 1; fetchData() }
function handleReset() { query.productName = ''; query.warehouseId = undefined; handleSearch() }
function handleExport() { downloadFile('/inventory/export', '库存查询.xlsx') }

// 按仓库分组
const grouped = computed(() => {
  const map = new Map<number, { wId: number; wName: string; items: Inventory[] }>()
  for (const item of allList.value) {
    const wid = item.warehouseId
    if (!map.has(wid)) {
      const w = warehouses.value.find(wh => wh.id === wid)
      map.set(wid, { wId: wid, wName: w?.name || '未知仓库', items: [] })
    }
    map.get(wid)!.items.push(item)
  }
  return Array.from(map.values())
})

// 每个仓库的小计
function warehouseSubtotal(items: Inventory[]) {
  const qty = items.reduce((s, i) => s + (i.quantity || 0), 0)
  const amount = items.reduce((s, i) => s + ((i.costPrice || 0) * (i.quantity || 0)), 0)
  return { qty, amount }
}

// 全部总计
const grandTotal = computed(() => {
  const qty = allList.value.reduce((s, i) => s + (i.quantity || 0), 0)
  const amount = allList.value.reduce((s, i) => s + ((i.costPrice || 0) * (i.quantity || 0)), 0)
  return { qty, amount }
})

onMounted(() => { fetchWarehouses(); fetchData() })
</script>

<template>
  <div>
    <div class="page-header">
      <h2>库存查询</h2>
      <div>
        <el-button @click="router.push('/inventory/log')">库存流水</el-button>
        <el-button @click="handleExport">导出Excel</el-button>
      </div>
    </div>

    <!-- 搜索 -->
    <div class="search-bar">
      <el-input v-model="query.productName" placeholder="商品名称" clearable style="width:200px" @keyup.enter="handleSearch" @clear="handleSearch" />
      <el-select v-model="query.warehouseId" placeholder="全部仓库" clearable style="width:160px" @change="handleSearch">
        <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
      </el-select>
      <el-button type="primary" @click="handleSearch">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <div v-loading="loading">
      <!-- 按仓库分组展示 -->
      <div v-if="grouped.length === 0 && !loading" style="text-align:center;padding:60px 0;color:#999;">
        暂无库存数据
      </div>

      <div v-for="group in grouped" :key="group.wId" class="warehouse-card">
        <div class="warehouse-header">
          <div class="warehouse-title">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="#409eff"><path d="M19 8h-1V3H6v5H5c-1.66 0-3 1.34-3 3v6h4v4h12v-4h4v-6c0-1.66-1.34-3-3-3zM8 5h8v3H8V5zm8 14H8v-4h8v4zm2-4v-2H6v2H4v-4c0-.55.45-1 1-1h14c.55 0 1 .45 1 1v4h-2z"/></svg>
            <span>{{ group.wName }}</span>
            <span class="warehouse-stats">
              {{ group.items.length }} 种商品 &nbsp;|&nbsp; 小计：¥{{ warehouseSubtotal(group.items).amount.toFixed(2) }}
            </span>
          </div>
        </div>

        <el-table :data="group.items" stripe border>
          <el-table-column prop="productCode" label="编码" width="130" />
          <el-table-column prop="productName" label="商品名称" min-width="160" />
          <el-table-column prop="batchNo" label="批次" width="100" />
          <el-table-column prop="quantity" label="库存数量" sortable width="100" align="center">
            <template #default="{ row }">
              <span :style="{ color: row.quantity <= 0 ? '#f56c6c' : '#303133', fontWeight: 600 }">{{ row.quantity }}</span>
            </template>
          </el-table-column>
          <el-table-column label="成本均价" width="140" align="right">
            <template #header>
              <span>
                成本均价
                <el-tooltip content="移动加权平均法估算，仅供参考" placement="top">
                  <span style="cursor:help;color:#999;font-size:12px;">ⓘ</span>
                </el-tooltip>
              </span>
            </template>
            <template #default="{ row }">¥{{ (row.costPrice || 0).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column label="库存金额" sortable width="130" align="right">
            <template #default="{ row }">¥{{ ((row.costPrice || 0) * (row.quantity || 0)).toFixed(2) }}</template>
          </el-table-column>
          <el-table-column prop="updateTime" label="最后变动" width="170">
            <template #default="{ row }">{{ row.updateTime ? row.updateTime.substring(0, 16) : '-' }}</template>
          </el-table-column>
        </el-table>

        <div class="warehouse-footer">
          <span>小计：{{ warehouseSubtotal(group.items).qty }} 件 &nbsp;·&nbsp; 金额 ¥{{ warehouseSubtotal(group.items).amount.toFixed(2) }}</span>
        </div>
      </div>

      <!-- 总计 -->
      <div class="grand-total">
        <span>总计：{{ grandTotal.qty }} 件 &nbsp;·&nbsp; 金额 ¥{{ grandTotal.amount.toFixed(2) }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.warehouse-card {
  margin-bottom: 20px;
  border: 1px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}
.warehouse-header {
  background: #f5f7fa;
  padding: 12px 20px;
  border-bottom: 1px solid #e4e7ed;
}
.warehouse-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}
.warehouse-stats {
  font-size: 13px;
  font-weight: 400;
  color: #909399;
  margin-left: 12px;
}
.warehouse-footer {
  background: #fafafa;
  padding: 10px 20px;
  text-align: right;
  font-size: 14px;
  color: #606266;
  border-top: 1px solid #e4e7ed;
}
.grand-total {
  text-align: right;
  padding: 16px 20px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  background: #f5f7fa;
  border-radius: 8px;
}
</style>
