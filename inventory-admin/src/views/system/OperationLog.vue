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
        <el-table-column prop="createTime" label="时间" width="180" />
        <el-table-column prop="operator" label="操作人" width="120" />
        <el-table-column prop="module" label="模块" width="100" />
        <el-table-column prop="action" label="操作" width="120" />
        <el-table-column prop="targetId" label="目标" width="180" />
        <el-table-column prop="detail" label="详情" min-width="200" />
        <el-table-column prop="ip" label="IP" width="140" />
      </el-table>
      <el-pagination v-model:page-size="query.size" :total="total" layout="total, sizes, prev, pager, next" style="margin-top:16px;justify-content:flex-end" @current-change="query.page = $event; fetchData()" @size-change="fetchData" />
    </div>
  </div>
</template>
