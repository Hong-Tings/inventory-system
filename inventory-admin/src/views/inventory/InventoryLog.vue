<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import request, { downloadFile } from '../../api/request'
import type { InventoryLog as InventoryLogType, PageResult, PageParams, Warehouse } from '../../types/api'

const loading = ref(false)
const list = ref<InventoryLogType[]>([])
const total = ref(0)
const warehouses = ref<Warehouse[]>([])
const query = reactive<PageParams & {
  productName?: string
  warehouseId?: number
  changeType?: string
  refOrderNo?: string
  operatorName?: string
  startDate?: string
  endDate?: string
}>({
  page: 1, size: 10,
  productName: '', warehouseId: undefined, changeType: '',
  refOrderNo: '', operatorName: '',
  startDate: '', endDate: '',
})

const changeTypeMap: Record<string, string> = {
  PURCHASE_IN: '采购入库',
  SALES_OUT: '销售出库',
  PURCHASE_CANCEL: '取消入库',
  SALES_CANCEL: '取消出库',
  ADJUSTMENT_IN: '盘盈入库',
  ADJUSTMENT_OUT: '盘亏出库',
  STOCKTAKE: '盘点',
  TRANSFER_IN: '调拨入库',
  TRANSFER_OUT: '调拨出库',
}

async function fetchData() {
  loading.value = true
  try {
    const res = await request.get('/inventory/log/page', { params: query })
    list.value = res.data.data.records
    total.value = res.data.data.total
  } finally { loading.value = false }
}

async function fetchWarehouses() {
  try {
    const res = await request.get('/warehouse/list')
    warehouses.value = res.data.data
  } catch { /* ignore */ }
}

function handleSearch() {
  query.page = 1
  fetchData()
}

function handleExport() {
  downloadFile('/inventory/log/export', '库存流水.xlsx')
}

onMounted(() => {
  fetchWarehouses()
  fetchData()
})
</script>

<template>
  <div>
    <div class="page-header"><h2>库存流水</h2><el-button @click="handleExport">导出Excel</el-button></div>
    <div class="search-bar">
      <el-input v-model="query.productName" placeholder="商品名称" clearable style="width:160px"
        @keyup.enter="handleSearch" @clear="handleSearch" />
      <el-select v-model="query.warehouseId" placeholder="仓库" clearable style="width:140px"
        @change="handleSearch">
        <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
      </el-select>
      <el-select v-model="query.changeType" placeholder="操作类型" clearable style="width:140px"
        @change="handleSearch">
        <el-option v-for="(label, key) in changeTypeMap" :key="key" :label="label" :value="key" />
      </el-select>
      <el-input v-model="query.refOrderNo" placeholder="关联单号" clearable style="width:160px"
        @keyup.enter="handleSearch" @clear="handleSearch" />
      <el-input v-model="query.operatorName" placeholder="操作人" clearable style="width:140px"
        @keyup.enter="handleSearch" @clear="handleSearch" />
      <el-date-picker v-model="query.startDate" type="date" placeholder="开始日期" value-format="YYYY-MM-DD" style="width:140px"
        @change="handleSearch" />
      <el-date-picker v-model="query.endDate" type="date" placeholder="结束日期" value-format="YYYY-MM-DD" style="width:140px"
        @change="handleSearch" />
      <el-button type="primary" @click="handleSearch">查询</el-button>
    </div>
    <div class="table-container">
      <el-table :data="list" v-loading="loading" stripe border>
        <el-table-column prop="createTime" label="时间" width="180" sortable />
        <el-table-column prop="productName" label="商品" min-width="160" />
        <el-table-column prop="warehouseName" label="仓库" width="120" />
        <el-table-column prop="changeType" label="类型" width="120">
          <template #default="{ row }">{{ changeTypeMap[row.changeType] || row.changeType }}</template>
        </el-table-column>
        <el-table-column prop="changeQty" label="变动数量" width="100">
          <template #default="{ row }">
            <span :style="{ color: row.changeQty > 0 ? '#67c23a' : '#f56c6c' }">{{ row.changeQty > 0 ? '+' : '' }}{{ row.changeQty }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="beforeQty" label="变动前" width="80" />
        <el-table-column prop="afterQty" label="变动后" width="80" />
        <el-table-column prop="refOrderNo" label="关联单号" width="180" />
        <el-table-column prop="operatorName" label="操作人" width="100" />
      </el-table>
      <el-pagination v-model:page-size="query.size" :total="total" layout="total, sizes, prev, pager, next" style="margin-top:16px;justify-content:flex-end"
        @current-change="query.page = $event; fetchData()" @size-change="query.page = 1; fetchData()" />
    </div>
  </div>
</template>
