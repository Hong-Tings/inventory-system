<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import request, { downloadFile } from '../../api/request'
import type { PurchaseOrder, PageResult, PageParams, Supplier, Warehouse } from '../../types/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../store/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const list = ref<PurchaseOrder[]>([])
const total = ref(0)
const selectedIds = ref<number[]>([])
const suppliers = ref<Supplier[]>([])
const warehouses = ref<Warehouse[]>([])
const query = reactive<PageParams & { orderNo?: string; status?: number; startDate?: string; endDate?: string; supplierId?: number; warehouseId?: number; minQuantity?: number; maxQuantity?: number }>({
  page: 1, size: 10, orderNo: '', status: undefined, startDate: '', endDate: '', supplierId: undefined, warehouseId: undefined, minQuantity: undefined, maxQuantity: undefined,
})

const statusMap: Record<number, { label: string; type: string }> = {
  0: { label: '草稿', type: 'info' },
  1: { label: '已入库', type: 'success' },
  2: { label: '已取消', type: 'danger' },
  4: { label: '待审批', type: 'warning' },
}

async function fetchData() {
  loading.value = true
  try {
    const res = await request.get('/purchase-order/page', { params: query })
    list.value = res.data.data.records
    total.value = res.data.data.total
  } finally { loading.value = false }
}
function handleSearch() { query.page = 1; fetchData() }
function handleReset() { query.orderNo = ''; query.status = undefined; query.startDate = ''; query.endDate = ''; query.supplierId = undefined; query.warehouseId = undefined; query.minQuantity = undefined; query.maxQuantity = undefined; handleSearch() }
async function handleCancel(row: PurchaseOrder) {
  await request.put(`/purchase-order/${row.id}/cancel`)
  ElMessage.success('已取消'); fetchData()
}
async function handleDelete(row: PurchaseOrder) {
  try {
    const { value } = await ElMessageBox.prompt(`确定作废入库单「${row.orderNo}」？`, '作废确认', {
      confirmButtonText: '确定作废',
      cancelButtonText: '取消',
      inputPlaceholder: '请填写作废原因（必填）',
      inputValidator: (val: string) => !!val.trim(),
      inputErrorMessage: '作废原因不能为空',
      type: 'danger',
    })
    await request.put(`/purchase-order/${row.id}/void`, { reason: value })
    ElMessage.success('已作废'); fetchData()
  } catch {} // cancelled or error
}
async function handleApprove(row: PurchaseOrder) {
  try {
    await ElMessageBox.confirm(`确认审核通过入库单「${row.orderNo}」？通过后将更新库存。`, '审核确认', { type: 'info' })
  } catch { return }
  await request.put(`/purchase-order/${row.id}/approve`)
  ElMessage.success('已审核通过'); fetchData()
}
async function handleReject(row: PurchaseOrder) {
  try {
    const { value } = await ElMessageBox.prompt(`确定驳回入库单「${row.orderNo}」？`, '驳回', {
      inputPlaceholder: '请填写驳回原因（必填）',
      inputValidator: (val: string) => !!val.trim(),
      inputErrorMessage: '驳回原因不能为空',
      type: 'warning',
    })
    await request.put(`/purchase-order/${row.id}/reject`, { reason: value })
    ElMessage.success('已驳回'); fetchData()
  } catch {}
}
async function handleBatchDelete() {
  try {
    const { value } = await ElMessageBox.prompt(`确定作废选中的 ${selectedIds.value.length} 个入库单？`, '批量作废确认', {
      confirmButtonText: '确定作废',
      cancelButtonText: '取消',
      inputPlaceholder: '请填写作废原因（必填）',
      inputValidator: (val: string) => !!val.trim(),
      inputErrorMessage: '作废原因不能为空',
      type: 'danger',
    })
    await request.put('/purchase-order/batch-void', { ids: selectedIds.value, reason: value })
    ElMessage.success('已作废'); selectedIds.value = []; fetchData()
  } catch {}
}
async function fetchSuppliers() { const res = await request.get('/supplier/list'); suppliers.value = res.data.data }
async function fetchWarehouses() { const res = await request.get('/warehouse/list'); warehouses.value = res.data.data }
function handleExport() { downloadFile('/purchase-order/export', '采购入库.xlsx') }
function getSummaries(param: { columns: any[]; data: any[] }) {
  const { columns, data } = param
  const sums: string[] = []
  columns.forEach((column, index) => {
    if (index === 0) { sums[index] = '合计'; return }
    if (column.property === 'totalQuantity' || column.property === 'totalAmount') {
      const values = data.map(item => Number(item[column.property]))
      if (!values.every(value => isNaN(value))) {
        const total = values.reduce((prev, curr) => prev + curr, 0)
        sums[index] = column.property === 'totalAmount' ? '¥' + total.toFixed(2) : total.toString()
      } else { sums[index] = '' }
    } else { sums[index] = '' }
  })
  return sums
}
onMounted(() => { fetchData(); fetchSuppliers(); fetchWarehouses() })
</script>

<template>
  <div>
    <div class="page-header"><h2>采购入库</h2><el-button type="primary" @click="router.push('/purchase/create')">新建入库单</el-button><el-button @click="handleExport">导出Excel</el-button></div>
    <div class="search-bar">
      <el-input v-model="query.orderNo" placeholder="入库单号" clearable style="width:180px" @keyup.enter="handleSearch" @clear="handleSearch" />
      <el-select v-model="query.supplierId" placeholder="供应商" clearable filterable style="width:160px" @change="handleSearch">
        <el-option v-for="s in suppliers" :key="s.id" :label="s.name" :value="s.id" />
      </el-select>
      <el-select v-model="query.warehouseId" placeholder="仓库" clearable style="width:140px" @change="handleSearch">
        <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
      </el-select>
      <el-select v-model="query.status" placeholder="状态" clearable style="width:120px" @change="handleSearch">
        <el-option label="草稿" :value="0" /><el-option label="已入库" :value="1" /><el-option label="已取消" :value="2" /><el-option label="待审批" :value="4" />
      </el-select>
      <el-date-picker v-model="query.startDate" type="date" placeholder="开始日期" value-format="YYYY-MM-DD" style="width:140px" @change="handleSearch" />
      <el-date-picker v-model="query.endDate" type="date" placeholder="结束日期" value-format="YYYY-MM-DD" style="width:140px" @change="handleSearch" />
      <el-input v-model="query.minQuantity" placeholder="最小数量" clearable style="width:120px" @keyup.enter="handleSearch" @clear="handleSearch" />
      <el-input v-model="query.maxQuantity" placeholder="最大数量" clearable style="width:120px" @keyup.enter="handleSearch" @clear="handleSearch" />
      <el-button type="primary" @click="handleSearch">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>
    <div class="table-container">
      <div style="margin-bottom:10px;display:flex;align-items:center;gap:8px;">
        <span style="font-size:13px;color:#666;">{{ selectedIds.length ? '已选 ' + selectedIds.length + ' 项' : '批量操作' }}</span>
        <el-button v-if="userStore.isAdmin" size="small" type="danger" :disabled="!selectedIds.length" @click="handleBatchDelete">批量作废</el-button>
      </div>
      <el-table :data="list" v-loading="loading" stripe border show-summary :summary-method="getSummaries" @selection-change="(rows: any[]) => selectedIds = rows.map(r => r.id)">
        <el-table-column type="selection" width="40" />
        <el-table-column prop="orderNo" label="入库单号" width="180" />
        <el-table-column prop="supplierName" label="供应商" width="160" />
        <el-table-column prop="warehouseName" label="仓库" width="120" />
        <el-table-column prop="totalQuantity" label="总数量" sortable width="80" />
        <el-table-column prop="totalAmount" label="总金额" sortable width="120">
          <template #default="{ row }">¥{{ row.totalAmount?.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="operatorName" label="操作人" width="100" />
        <el-table-column prop="orderDate" label="入库日期" sortable width="120" />
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" sortable width="160">
          <template #default="{ row }">{{ row.createTime ? row.createTime.substring(0, 16) : '-' }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type as any">{{ statusMap[row.status]?.label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="router.push(`/purchase/${row.id}`)">详情</el-button>
            <el-button v-if="row.status === 0" size="small" @click="router.push(`/purchase/create?edit=${row.id}`)">编辑</el-button>
            <el-button v-if="userStore.isAdmin && row.status === 4" size="small" type="success" @click="handleApprove(row)">通过</el-button>
            <el-button v-if="userStore.isAdmin && row.status === 4" size="small" type="warning" @click="handleReject(row)">驳回</el-button>
            <el-button v-if="row.status === 0" size="small" type="warning" @click="handleCancel(row)">取消</el-button>
            <el-button v-if="row.status === 1" size="small" type="warning" @click="handleCancel(row)">取消入库</el-button>
            <el-button v-if="userStore.isAdmin && (row.status === 0 || row.status === 2)" size="small" type="danger" @click="handleDelete(row)">作废</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:page-size="query.size" :total="total" layout="total, sizes, prev, pager, next" style="margin-top:16px;justify-content:flex-end" @current-change="query.page = $event; fetchData()" @size-change="query.page = 1; fetchData()" />
    </div>
  </div>
</template>
