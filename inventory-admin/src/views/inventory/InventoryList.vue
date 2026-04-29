<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import request from '../../api/request'
import type { Inventory, PageResult, PageParams, Warehouse } from '../../types/api'
import { useRouter } from 'vue-router'

const router = useRouter()
const loading = ref(false)
const list = ref<Inventory[]>([])
const total = ref(0)
const warehouses = ref<Warehouse[]>([])

const query = reactive<PageParams & { productName?: string; warehouseId?: number }>({
  page: 1, size: 10, productName: '', warehouseId: undefined,
})

async function fetchData() {
  loading.value = true
  try {
    const res = await request.get('/inventory/page', { params: query })
    list.value = res.data.data.records; total.value = res.data.data.total
  } finally { loading.value = false }
}

async function fetchWarehouses() {
  const res = await request.get('/warehouse/list')
  warehouses.value = res.data.data
}

function handleSearch() { query.page = 1; fetchData() }
function handleReset() { query.productName = ''; query.warehouseId = undefined; handleSearch() }
function handleExport() { window.open('/api/v1/inventory/export', '_blank') }

function getSummaries(param: { columns: any[]; data: any[] }) {
  const { columns, data } = param
  const sums: string[] = []
  columns.forEach((column, index) => {
    if (index === 4) {
      sums[index] = '总计: ' + data.reduce((sum: number, row: any) => sum + (row.quantity || 0), 0)
    } else if (index === 5) {
      sums[index] = '总计: ' + data.reduce((sum: number, row: any) => sum + (row.lockedQty || 0), 0)
    } else if (index === 6) {
      sums[index] = '总计: ' + data.reduce((sum: number, row: any) => sum + ((row.quantity || 0) - (row.lockedQty || 0)), 0)
    } else if (index === 0) {
      sums[index] = '合计'
    } else {
      sums[index] = ''
    }
  })
  return sums
}

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
    <div class="search-bar">
      <el-input v-model="query.productName" placeholder="商品名称" clearable style="width:200px" @keyup.enter="handleSearch" @clear="handleSearch" />
      <el-select v-model="query.warehouseId" placeholder="全部仓库" clearable style="width:160px" @change="handleSearch">
        <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
      </el-select>
      <el-button type="primary" @click="handleSearch">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>
    <div class="table-container">
      <el-table :data="list" v-loading="loading" stripe border show-summary :summary-method="getSummaries">
        <el-table-column prop="productCode" label="商品编码" width="140" />
        <el-table-column prop="productName" label="商品名称" min-width="180" />
        <el-table-column prop="warehouseName" label="仓库" width="120" />
        <el-table-column prop="batchNo" label="批次号" width="140" />
        <el-table-column prop="quantity" label="可用库存" sortable width="100">
          <template #default="{ row }">
            <span :style="{ color: row.quantity <= 0 ? '#f56c6c' : '#67c23a', fontWeight: 'bold' }">{{ row.quantity }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="lockedQty" label="锁定库存" sortable width="100" />
        <el-table-column label="可发库存" width="100">
          <template #default="{ row }">{{ (row.quantity || 0) - (row.lockedQty || 0) }}</template>
        </el-table-column>
        <el-table-column prop="updateTime" label="变动时间" sortable width="170">
          <template #default="{ row }">{{ row.updateTime ? row.updateTime.substring(0, 16) : '-' }}</template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:page-size="query.size" :total="total" layout="total, sizes, prev, pager, next" style="margin-top:16px;justify-content:flex-end" @current-change="query.page = $event; fetchData()" @size-change="fetchData" />
    </div>
  </div>
</template>
