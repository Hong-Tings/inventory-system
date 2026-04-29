<script setup lang="ts">
import { ref, onMounted, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import request from '../../api/request'
import type { Warehouse, PageParams } from '../../types/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../../store/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const list = ref<Warehouse[]>([])
const total = ref(0)
const selectedIds = ref<number[]>([])
const dialogVisible = ref(false)

const query = reactive<PageParams & { name?: string; status?: number; contact?: string; phone?: string; address?: string }>({ page: 1, size: 10, name: '', status: undefined, contact: '', phone: '', address: '' })
const form = reactive<any>({ name: '', code: '', contact: '', phone: '', address: '', status: 1, remark: '' })

async function fetchData() {
  loading.value = true
  try {
    const res = await request.get('/warehouse/page', { params: query })
    list.value = res.data.data.records
    total.value = res.data.data.total
  } finally { loading.value = false }
}
function handleSearch() { query.page = 1; fetchData() }
function handleReset() { query.name = ''; query.status = undefined; query.contact = ''; query.phone = ''; query.address = ''; handleSearch() }
function openCreate() { Object.assign(form, { id: undefined, name: '', code: '', contact: '', phone: '', address: '', status: 1, remark: '' }); dialogVisible.value = true }
function openEdit(row: any) { Object.assign(form, { ...row }); dialogVisible.value = true }
async function handleSave() {
  if (!form.name || !form.address || !form.contact || !form.phone) { ElMessage.warning('请填写完整信息'); return }
  try {
    if (form.id) await request.put(`/warehouse/${form.id}`, form)
    else await request.post('/warehouse', form)
    ElMessage.success('保存成功'); dialogVisible.value = false; fetchData()
  } catch { /* handled */ }
}
async function handleToggleStatus(row: any) {
  const s = row.status === 1 ? 0 : 1
  await request.put(`/warehouse/${row.id}`, { ...row, status: s })
  ElMessage.success(s === 1 ? '已启用' : '已停用'); fetchData()
}
async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(`确定作废仓库「${row.name}」？`, '确认作废', { type: 'warning' })
    await request.delete(`/warehouse/${row.id}`)
    ElMessage.success('已作废'); fetchData()
  } catch (err: any) {
    if (err?.response?.data?.message) ElMessage.error(err.response.data.message)
  }
}
async function handleBatchToggle(status: number) {
  if (!selectedIds.value.length) { ElMessage.warning('请先选择仓库'); return }
  for (const id of selectedIds.value) {
    await request.put(`/warehouse/${id}`, { id, status })
  }
  ElMessage.success(`已${status === 1 ? '启用' : '停用'} ${selectedIds.value.length} 个仓库`)
  selectedIds.value = []; fetchData()
}
function handleExport() { window.open('/api/v1/warehouse/export', '_blank') }
onMounted(fetchData)
</script>

<template>
  <div>
    <div class="page-header"><h2>仓库管理</h2>
      <div>
        <el-button type="primary" @click="openCreate">+ 新增仓库</el-button>
        <el-button @click="handleExport">导出Excel</el-button>
      </div>
    </div>

    <div class="search-bar">
      <el-input v-model="query.name" placeholder="仓库名称" clearable style="width:200px" @keyup.enter="handleSearch" @clear="handleSearch" />
      <el-input v-model="query.contact" placeholder="联系人" clearable style="width:140px" @keyup.enter="handleSearch" @clear="handleSearch" />
      <el-input v-model="query.phone" placeholder="联系电话" clearable style="width:160px" @keyup.enter="handleSearch" @clear="handleSearch" />
      <el-input v-model="query.address" placeholder="仓库地址" clearable style="width:200px" @keyup.enter="handleSearch" @clear="handleSearch" />
      <el-select v-model="query.status" placeholder="状态" clearable style="width:120px" @change="handleSearch">
        <el-option label="启用" :value="1" /><el-option label="停用" :value="0" />
      </el-select>
      <el-button type="primary" @click="handleSearch">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <div style="margin-bottom:10px;" v-if="selectedIds.length">
      <span style="font-size:13px;color:#666;margin-right:8px;">已选 {{ selectedIds.length }} 项</span>
      <el-button size="small" type="success" @click="handleBatchToggle(1)">批量启用</el-button>
      <el-button size="small" type="warning" @click="handleBatchToggle(0)">批量停用</el-button>
    </div>

    <div class="table-container">
      <el-table :data="list" v-loading="loading" stripe border @selection-change="(rows: any[]) => selectedIds = rows.map(r => r.id)">
        <el-table-column type="selection" width="40" />
        <el-table-column prop="code" label="编码" width="100" />
        <el-table-column prop="name" label="仓库名称" width="140" />
        <el-table-column prop="address" label="地址" width="120" show-overflow-tooltip />
        <el-table-column prop="contact" label="负责人" width="90" />
        <el-table-column prop="phone" label="联系电话" width="130" />
        <el-table-column label="商品总数" width="100">
          <template #default="{ row }"><span style="font-weight:600;">{{ row.productCount ?? '-' }}</span></template>
        </el-table-column>
        <el-table-column label="库存金额" width="120">
          <template #default="{ row }">¥{{ row.totalAmount?.toFixed(2) ?? '0.00' }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="70">
          <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '停用' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" width="120" show-overflow-tooltip />
        <el-table-column prop="createTime" label="新增时间" width="170" sortable>
          <template #default="{ row }">{{ row.createTime ? row.createTime.substring(0, 16) : '-' }}</template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="170" sortable>
          <template #default="{ row }">{{ row.updateTime ? row.updateTime.substring(0, 16) : '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" :type="row.status === 1 ? 'warning' : 'success'" @click="handleToggleStatus(row)">{{ row.status === 1 ? '停用' : '启用' }}</el-button>
            <el-button v-if="userStore.isAdmin" size="small" type="danger" @click="handleDelete(row)">作废</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:page-size="query.size" :total="total" layout="total, sizes, prev, pager, next" style="margin-top:16px;justify-content:flex-end" @current-change="query.page = $event; fetchData()" @size-change="fetchData" />
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑仓库' : '新增仓库'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="仓库名称" required><el-input v-model="form.name" placeholder="如：一号仓库、门店仓库" /></el-form-item>
        <el-form-item label="仓库编码"><span style="color:#999;line-height:32px;">{{ form.code || '自动生成' }}</span></el-form-item>
        <el-form-item label="仓库地址" required><el-input v-model="form.address" placeholder="XX市XX区XX路XX号" /></el-form-item>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="负责人" required><el-input v-model="form.contact" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="联系电话" required><el-input v-model="form.phone" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" placeholder="如：常温仓、冷冻仓" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="handleSave">保存</el-button></template>
    </el-dialog>
  </div>
</template>
