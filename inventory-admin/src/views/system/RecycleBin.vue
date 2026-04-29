<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '../../api/request'
import { ElMessage } from 'element-plus'

const data = ref<Record<string, any[]>>({})
const loading = ref(false)
const activeTab = ref('products')

const tabs = [
  { key: 'products', label: '商品' },
  { key: 'categories', label: '商品分类' },
  { key: 'suppliers', label: '供应商' },
  { key: 'customers', label: '客户' },
  { key: 'warehouses', label: '仓库' },
  { key: 'purchaseOrders', label: '采购入库单' },
  { key: 'salesOrders', label: '销售出库单' },
  { key: 'transfers', label: '库存调拨单' },
  { key: 'stockTakes', label: '盘点单' },
]

async function fetchData() {
  loading.value = true
  try {
    const res = await request.get('/recycle/list')
    data.value = res.data.data || {}
  } finally { loading.value = false }
}

function getColumns(type: string) {
  const cols: { prop: string; label: string; width?: string }[] = []
  switch (type) {
    case 'products':
      cols.push({ prop: 'code', label: '编码', width: '120' })
      cols.push({ prop: 'name', label: '商品名称', width: '160' })
      cols.push({ prop: 'spec', label: '规格', width: '100' })
      cols.push({ prop: 'unit', label: '单位', width: '60' })
      cols.push({ prop: 'updateTime', label: '作废时间', width: '160' })
      break
    case 'categories':
      cols.push({ prop: 'name', label: '分类名称', width: '160' })
      cols.push({ prop: 'updateTime', label: '作废时间', width: '160' })
      break
    case 'suppliers':
    case 'customers':
      cols.push({ prop: 'code', label: '编码', width: '120' })
      cols.push({ prop: 'name', label: '名称', width: '160' })
      cols.push({ prop: 'contact', label: '联系人', width: '100' })
      cols.push({ prop: 'phone', label: '电话', width: '130' })
      cols.push({ prop: 'updateTime', label: '作废时间', width: '160' })
      break
    case 'warehouses':
      cols.push({ prop: 'code', label: '编码', width: '120' })
      cols.push({ prop: 'name', label: '仓库名称', width: '160' })
      cols.push({ prop: 'contact', label: '联系人', width: '100' })
      cols.push({ prop: 'updateTime', label: '作废时间', width: '160' })
      break
    case 'purchaseOrders':
      cols.push({ prop: 'orderNo', label: '入库单号', width: '160' })
      cols.push({ prop: 'supplierName', label: '供应商', width: '130' })
      cols.push({ prop: 'totalAmount', label: '总金额', width: '100' })
      cols.push({ prop: 'orderDate', label: '日期', width: '100' })
      cols.push({ prop: 'remark', label: '作废原因', width: '180' })
      cols.push({ prop: 'updateTime', label: '作废时间', width: '160' })
      break
    case 'salesOrders':
      cols.push({ prop: 'orderNo', label: '出库单号', width: '160' })
      cols.push({ prop: 'customerName', label: '客户', width: '130' })
      cols.push({ prop: 'totalAmount', label: '总金额', width: '100' })
      cols.push({ prop: 'orderDate', label: '日期', width: '100' })
      cols.push({ prop: 'remark', label: '作废原因', width: '180' })
      cols.push({ prop: 'updateTime', label: '作废时间', width: '160' })
      break
    case 'transfers':
      cols.push({ prop: 'orderNo', label: '调拨单号', width: '160' })
      cols.push({ prop: 'fromWarehouseName', label: '调出仓库', width: '120' })
      cols.push({ prop: 'toWarehouseName', label: '调入仓库', width: '120' })
      cols.push({ prop: 'remark', label: '作废原因', width: '180' })
      cols.push({ prop: 'updateTime', label: '作废时间', width: '160' })
      break
    case 'stockTakes':
      cols.push({ prop: 'orderNo', label: '盘点单号', width: '160' })
      cols.push({ prop: 'warehouseName', label: '仓库', width: '120' })
      cols.push({ prop: 'totalItems', label: '商品数', width: '70' })
      cols.push({ prop: 'diffItems', label: '差异数', width: '70' })
      cols.push({ prop: 'remark', label: '作废原因', width: '180' })
      cols.push({ prop: 'updateTime', label: '作废时间', width: '160' })
      break
  }
  return cols
}

onMounted(fetchData)
</script>

<template>
  <div>
    <div class="page-header"><h2>已作废列表</h2></div>

    <el-tabs v-model="activeTab" type="border-card">
      <el-tab-pane v-for="tab in tabs" :key="tab.key" :label="`${tab.label} (${(data[tab.key] || []).length})`" :name="tab.key">
        <div style="min-height:200px;">
          <div v-if="loading" style="text-align:center;padding:40px;color:#999;">加载中...</div>
          <el-empty v-else-if="!(data[tab.key]?.length)" description="暂无数据" />
          <el-table v-else :data="data[tab.key]" stripe border size="small" max-height="400">
            <el-table-column v-for="col in getColumns(tab.key)" :key="col.prop" :prop="col.prop" :label="col.label" :width="col.width" show-overflow-tooltip />
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>
