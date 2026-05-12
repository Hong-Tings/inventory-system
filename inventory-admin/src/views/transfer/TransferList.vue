<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import request, { downloadFile } from '../../api/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../store/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const warehouses = ref<any[]>([])
const selectedIds = ref<number[]>([])
const query = reactive<{
  page: number; size: number; orderNo?: string; outWarehouseId?: number; inWarehouseId?: number;
  minQuantity?: number; maxQuantity?: number; operatorName?: string; status?: number;
  startDate?: string; endDate?: string;
}>({
  page: 1, size: 10, orderNo: '', outWarehouseId: undefined, inWarehouseId: undefined,
  minQuantity: undefined, maxQuantity: undefined, operatorName: '', status: undefined,
  startDate: '', endDate: '',
})

const statusMap: Record<number, { label: string; type: string }> = {
  0: { label: '草稿', type: 'info' },
  1: { label: '已完成', type: 'success' },
  2: { label: '已取消', type: 'danger' },
  4: { label: '待审批', type: 'warning' },
}

async function fetchData() {
  loading.value = true
  try {
    const params: Record<string, any> = { page: query.page, size: query.size }
    if (query.orderNo) params.orderNo = query.orderNo
    if (query.outWarehouseId !== undefined) params.fromWh = query.outWarehouseId
    if (query.inWarehouseId !== undefined) params.toWh = query.inWarehouseId
    if (query.status !== undefined) params.status = query.status
    if (query.minQuantity !== undefined) params.minQuantity = query.minQuantity
    if (query.maxQuantity !== undefined) params.maxQuantity = query.maxQuantity
    if (query.operatorName) params.operatorName = query.operatorName
    if (query.startDate) params.startDate = query.startDate
    if (query.endDate) params.endDate = query.endDate
    const res = await request.get('/transfer/page', { params })
    list.value = res.data.data.records
    total.value = res.data.data.total
  } finally { loading.value = false }
}
function handleSearch() { query.page = 1; fetchData() }
function handleReset() { query.orderNo = ''; query.outWarehouseId = undefined; query.inWarehouseId = undefined; query.status = undefined; query.minQuantity = undefined; query.maxQuantity = undefined; query.operatorName = ''; query.startDate = ''; query.endDate = ''; handleSearch() }
async function handleCancel(row: any) {
  await request.put(`/transfer/${row.id}/cancel`)
  ElMessage.success('已取消'); fetchData()
}
async function handleDelete(row: any) {
  try {
    const { value } = await ElMessageBox.prompt(`确定作废调拨单「${row.orderNo}」？`, '作废确认', {
      confirmButtonText: '确定作废',
      cancelButtonText: '取消',
      inputPlaceholder: '请填写作废原因（必填）',
      inputValidator: (val: string) => !!val.trim(),
      inputErrorMessage: '作废原因不能为空',
      type: 'danger',
    })
    await request.put(`/transfer/${row.id}/void`, { reason: value })
    ElMessage.success('已作废'); fetchData()
  } catch {}
}
async function handleApprove(row: any) {
  try {
    await ElMessageBox.confirm(`确认审核通过调拨单「${row.orderNo}」？通过后将更新库存。`, '审核确认', { type: 'info' })
  } catch { return }
  await request.put(`/transfer/${row.id}/approve`)
  ElMessage.success('已审核通过'); fetchData()
}
async function handleReject(row: any) {
  try {
    const { value } = await ElMessageBox.prompt(`确定驳回调拨单「${row.orderNo}」？`, '驳回', {
      inputPlaceholder: '请填写驳回原因（必填）',
      inputValidator: (val: string) => !!val.trim(),
      inputErrorMessage: '驳回原因不能为空',
      type: 'warning',
    })
    await request.put(`/transfer/${row.id}/reject`, { reason: value })
    ElMessage.success('已驳回'); fetchData()
  } catch {}
}
async function handleBatchDelete() {
  if (selectedIds.value.length === 0) { ElMessage.warning('请选择要删除的调拨单'); return }
  try {
    const { value } = await ElMessageBox.prompt(`确定作废选中的 ${selectedIds.value.length} 条调拨单？`, '批量作废确认', {
      confirmButtonText: '确定作废',
      cancelButtonText: '取消',
      inputPlaceholder: '请填写作废原因（必填）',
      inputValidator: (val: string) => !!val.trim(),
      inputErrorMessage: '作废原因不能为空',
      type: 'danger',
    })
    await request.put('/transfer/batch-void', { ids: selectedIds.value, reason: value })
    ElMessage.success('已作废'); selectedIds.value = []; fetchData()
  } catch {}
}
function handleExport() {
  downloadFile('/transfer/export', '库存调拨.xlsx')
}
onMounted(async () => {
  const wRes = await request.get('/warehouse/list')
  warehouses.value = wRes.data.data
  fetchData()
})
</script>

<template>
  <div>
    <div class="page-header"><h2>库存调拨</h2><el-button type="primary" @click="router.push('/transfer/create')">新建调拨单</el-button><el-button @click="handleExport">导出Excel</el-button></div>
    <div class="search-bar">
      <el-input v-model="query.orderNo" placeholder="调拨单号" clearable style="width:160px" @keyup.enter="handleSearch" @clear="handleSearch" />
      <el-select v-model="query.outWarehouseId" placeholder="调出仓库" clearable style="width:140px" @change="handleSearch">
        <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
      </el-select>
      <el-select v-model="query.inWarehouseId" placeholder="调入仓库" clearable style="width:140px" @change="handleSearch">
        <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
      </el-select>
      <el-select v-model="query.status" placeholder="状态" clearable style="width:100px" @change="handleSearch">
        <el-option label="草稿" :value="0" /><el-option label="已完成" :value="1" /><el-option label="已取消" :value="2" /><el-option label="待审批" :value="4" />
      </el-select>
      <el-input v-model="query.operatorName" placeholder="操作人" clearable style="width:120px" @keyup.enter="handleSearch" @clear="handleSearch" />
      <el-input v-model="query.minQuantity" placeholder="最小数量" clearable style="width:100px" @keyup.enter="handleSearch" @clear="handleSearch" />
      <el-input v-model="query.maxQuantity" placeholder="最大数量" clearable style="width:100px" @keyup.enter="handleSearch" @clear="handleSearch" />
      <el-date-picker v-model="query.startDate" type="date" placeholder="开始日期" value-format="YYYY-MM-DD" style="width:130px" @change="handleSearch" />
      <el-date-picker v-model="query.endDate" type="date" placeholder="结束日期" value-format="YYYY-MM-DD" style="width:130px" @change="handleSearch" />
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
        <el-table-column prop="orderNo" label="调拨单号" width="150" />
        <el-table-column prop="fromWarehouseName" label="调出仓库" width="120" />
        <el-table-column prop="toWarehouseName" label="调入仓库" width="120" />
        <el-table-column prop="totalQuantity" label="数量" sortable width="80" />
        <el-table-column prop="operatorName" label="操作人" width="80" />
        <el-table-column prop="orderDate" label="日期" width="90" />
        <el-table-column prop="remark" label="备注" min-width="130" show-overflow-tooltip />
        <el-table-column label="创建时间" width="150">
          <template #default="{ row }">{{ row.createTime ? row.createTime.substring(0, 16) : '-' }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type as any">{{ statusMap[row.status]?.label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="320" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="router.push(`/transfer/${row.id}`)">详情</el-button>
            <el-button v-if="row.status === 0" size="small" @click="router.push(`/transfer/create?edit=${row.id}`)">编辑</el-button>
            <el-button v-if="userStore.isAdmin && row.status === 4" size="small" type="success" @click="handleApprove(row)">通过</el-button>
            <el-button v-if="userStore.isAdmin && row.status === 4" size="small" type="warning" @click="handleReject(row)">驳回</el-button>
            <el-button v-if="row.status === 0" size="small" type="warning" @click="handleCancel(row)">取消</el-button>
            <el-button v-if="userStore.isAdmin" size="small" type="danger" @click="handleDelete(row)">作废</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:page-size="query.size" :total="total" layout="total, sizes, prev, pager, next" style="margin-top:16px;justify-content:flex-end" @current-change="query.page = $event; fetchData()" @size-change="query.page = 1; fetchData()" />
    </div>
  </div>
</template>
