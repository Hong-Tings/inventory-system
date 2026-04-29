<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import request from '../../api/request'
import type { PageParams, PageResult } from '../../types/api'

interface OpLog { id: number; operator: string; module: string; action: string; targetId: string; detail: string; ip: string; createTime: string }

const loading = ref(false)
const list = ref<OpLog[]>([])
const total = ref(0)
const query = reactive<PageParams & { module?: string; startDate?: string; endDate?: string }>({ page: 1, size: 10, module: '', startDate: '', endDate: '' })
function handleSearch() { query.page = 1; fetchData() }
async function fetchData() {
  loading.value = true
  try { const res = await request.get('/system/log/page', { params: query }); list.value = res.data.data.records; total.value = res.data.data.total } finally { loading.value = false }
}

const moduleMap: Record<string, string> = {
  product: '商品', purchase: '采购入库', sales: '销售出库',
  stocktake: '库存盘点', transfer: '库存调拨', inventory: '库存管理',
  supplier: '供应商', customer: '客户', warehouse: '仓库管理',
  system: '系统管理', auth: '登录认证',
}

const actionMap: Record<string, string> = {
  create: '新增', update: '修改', delete: '作废', cancel: '取消',
  submit: '提交', import: '导入', export: '导出', login: '登录',
  logout: '退出', adjust: '调整', approve: '审核', save: '保存',
}

onMounted(fetchData)
</script>

<template>
  <div>
    <div class="page-header"><h2>操作日志</h2></div>
    <div class="search-bar">
      <el-select v-model="query.module" placeholder="模块" clearable style="width:140px" @change="handleSearch">
        <el-option label="商品" value="product" /><el-option label="入库" value="purchase" /><el-option label="出库" value="sales" /><el-option label="盘点" value="stocktake" /><el-option label="系统" value="system" />
      </el-select>
      <el-date-picker v-model="query.startDate" type="date" placeholder="开始日期" value-format="YYYY-MM-DD" style="width:140px" @change="handleSearch" />
      <el-date-picker v-model="query.endDate" type="date" placeholder="结束日期" value-format="YYYY-MM-DD" style="width:140px" @change="handleSearch" />
    </div>
    <div class="table-container">
      <el-table :data="list" v-loading="loading" stripe border>
        <el-table-column prop="createTime" label="时间" width="170" />
        <el-table-column prop="operator" label="操作人" width="100" />
        <el-table-column label="模块" width="90">
          <template #default="{ row }">{{ moduleMap[row.module] || row.module }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90">
          <template #default="{ row }">{{ actionMap[row.action] || row.action }}</template>
        </el-table-column>
        <el-table-column prop="targetId" label="目标" width="170" />
        <el-table-column prop="detail" label="详情" min-width="220" />
        <el-table-column prop="ip" label="IP地址" width="140" />
      </el-table>
      <el-pagination v-model:page-size="query.size" :total="total" layout="total, sizes, prev, pager, next" style="margin-top:16px;justify-content:flex-end" @current-change="query.page = $event; fetchData()" @size-change="fetchData" />
    </div>
  </div>
</template>
