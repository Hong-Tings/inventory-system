<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import request, { downloadFile } from '../../api/request'
import type { StockTake, PageResult, PageParams } from '../../types/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../store/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const list = ref<StockTake[]>([])
const total = ref(0)
const warehouses = ref<any[]>([])
const selectedIds = ref<number[]>([])
const query = reactive<PageParams & {
  orderNo?: string; status?: number; warehouseId?: number; takeType?: number;
  minTotalItems?: number; maxTotalItems?: number; minDiffItems?: number; maxDiffItems?: number;
  operatorName?: string; approverName?: string;
}>({ page: 1, size: 10, orderNo: '', status: undefined, warehouseId: undefined, takeType: undefined,
  minTotalItems: undefined, maxTotalItems: undefined, minDiffItems: undefined, maxDiffItems: undefined,
  operatorName: '', approverName: '' })
const statusMap: Record<number, { label: string; type: string }> = {
  0: { label: '盘点中', type: 'warning' }, 1: { label: '已审核', type: 'success' }, 2: { label: '已调整', type: 'info' },
}
async function fetchData() {
  loading.value = true
  try {
    const res = await request.get('/stock-take/page', { params: query })
    list.value = res.data.data.records; total.value = res.data.data.total
  } finally { loading.value = false }
}
function handleSearch() { query.page = 1; fetchData() }
function handleReset() {
  query.orderNo = ''; query.status = undefined; query.warehouseId = undefined; query.takeType = undefined;
  query.minTotalItems = undefined; query.maxTotalItems = undefined; query.minDiffItems = undefined; query.maxDiffItems = undefined;
  query.operatorName = ''; query.approverName = ''; handleSearch()
}
async function handleDelete(row: StockTake) {
  try {
    const { value } = await ElMessageBox.prompt(`确定作废盘点单「${row.orderNo}」？`, '作废确认', {
      confirmButtonText: '确定作废',
      cancelButtonText: '取消',
      inputPlaceholder: '请填写作废原因（必填）',
      inputValidator: (val: string) => !!val.trim(),
      inputErrorMessage: '作废原因不能为空',
      type: 'danger',
    })
    await request.put(`/stock-take/${row.id}/void`, { reason: value })
    ElMessage.success('已作废'); fetchData()
  } catch {}
}
async function handleBatchDelete() {
  if (selectedIds.value.length === 0) { ElMessage.warning('请选择要删除的盘点单'); return }
  try {
    const { value } = await ElMessageBox.prompt(`确定作废选中的 ${selectedIds.value.length} 条盘点单？`, '批量作废确认', {
      confirmButtonText: '确定作废',
      cancelButtonText: '取消',
      inputPlaceholder: '请填写作废原因（必填）',
      inputValidator: (val: string) => !!val.trim(),
      inputErrorMessage: '作废原因不能为空',
      type: 'danger',
    })
    await request.put('/stock-take/batch-void', { ids: selectedIds.value, reason: value })
    ElMessage.success('已作废'); selectedIds.value = []; fetchData()
  } catch {}
}
function handleExport() {
  downloadFile('/stock-take/export', '库存盘点.xlsx')
}
onMounted(async () => {
  const wRes = await request.get('/warehouse/list')
  warehouses.value = wRes.data.data
  fetchData()
})
</script>

<template>
  <div>
    <div class="page-header"><h2>库存盘点</h2><el-button type="primary" @click="router.push('/stocktake/create')">新建盘点单</el-button><el-button @click="handleExport">导出Excel</el-button></div>
    <div class="search-bar">
      <el-input v-model="query.orderNo" placeholder="盘点单号" clearable style="width:160px" @keyup.enter="handleSearch" @clear="handleSearch" />
      <el-select v-model="query.warehouseId" placeholder="仓库" clearable style="width:120px" @change="handleSearch">
        <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
      </el-select>
      <el-select v-model="query.takeType" placeholder="盘点方式" clearable style="width:100px" @change="handleSearch">
        <el-option label="全盘" :value="0" /><el-option label="抽盘" :value="1" />
      </el-select>
      <el-select v-model="query.status" placeholder="状态" clearable style="width:100px" @change="handleSearch">
        <el-option label="盘点中" :value="0" /><el-option label="已审核" :value="1" /><el-option label="已调整" :value="2" />
      </el-select>
      <el-input v-model="query.minTotalItems" placeholder="最小商品数" clearable style="width:110px" @keyup.enter="handleSearch" @clear="handleSearch" />
      <el-input v-model="query.maxTotalItems" placeholder="最大商品数" clearable style="width:110px" @keyup.enter="handleSearch" @clear="handleSearch" />
      <el-input v-model="query.minDiffItems" placeholder="最小差异数" clearable style="width:110px" @keyup.enter="handleSearch" @clear="handleSearch" />
      <el-input v-model="query.maxDiffItems" placeholder="最大差异数" clearable style="width:110px" @keyup.enter="handleSearch" @clear="handleSearch" />
      <el-input v-model="query.operatorName" placeholder="盘点人" clearable style="width:100px" @keyup.enter="handleSearch" @clear="handleSearch" />
      <el-input v-model="query.approverName" placeholder="审核人" clearable style="width:100px" @keyup.enter="handleSearch" @clear="handleSearch" />
      <el-button type="primary" @click="handleSearch">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>
    <div class="table-container">
      <div style="margin-bottom:10px;display:flex;align-items:center;gap:8px;">
        <span style="font-size:13px;color:#666;">{{ selectedIds.length ? '已选 ' + selectedIds.length + ' 项' : '批量操作' }}</span>
        <el-button v-if="userStore.isAdmin" size="small" type="danger" :disabled="!selectedIds.length" @click="handleBatchDelete">批量作废</el-button>
      </div>
      <el-table :data="list" v-loading="loading" stripe border @selection-change="(rows: any[]) => selectedIds = rows.map((r: any) => r.id)">
        <el-table-column type="selection" width="45" />
        <el-table-column prop="orderNo" label="盘点单号" width="180" />
        <el-table-column prop="warehouseName" label="仓库" width="140" />
        <el-table-column prop="orderDate" label="盘点日期" width="110" />
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" sortable width="160">
          <template #default="{ row }">{{ row.createTime ? row.createTime.substring(0, 16) : '-' }}</template>
        </el-table-column>
        <el-table-column prop="takeType" label="盘点方式" width="100">
          <template #default="{ row }">{{ row.takeType === 0 ? '全盘' : '抽盘' }}</template>
        </el-table-column>
        <el-table-column prop="totalItems" label="商品数" sortable width="80" />
        <el-table-column prop="diffItems" label="差异数" sortable width="80" />
        <el-table-column prop="operatorName" label="盘点人" width="100" />
        <el-table-column prop="approverName" label="审核人" width="100" />
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }"><el-tag :type="statusMap[row.status]?.type as any">{{ statusMap[row.status]?.label }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="router.push(`/stocktake/${row.id}`)">详情</el-button>
            <el-button v-if="userStore.isAdmin && row.status !== 2" size="small" type="danger" @click="handleDelete(row)">作废</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:page-size="query.size" :total="total" layout="total, sizes, prev, pager, next" style="margin-top:16px;justify-content:flex-end" @current-change="query.page = $event; fetchData()" @size-change="query.page = 1; fetchData()" />
    </div>
  </div>
</template>
