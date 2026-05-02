<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import request, { downloadFile } from '../../api/request'
import type { Customer, PageResult, PageParams } from '../../types/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../../store/user'

const userStore = useUserStore()
const loading = ref(false)
const list = ref<Customer[]>([])
const total = ref(0)
const query = reactive<PageParams & { name?: string; contact?: string; phone?: string; address?: string }>({ page: 1, size: 10, name: '', contact: '', phone: '', address: '' })
const dialogVisible = ref(false)
const form = reactive<Partial<Customer>>({ name: '', code: '', contact: '', phone: '', address: '', remark: '' })

async function fetchData() {
  loading.value = true
  try {
    const res = await request.get('/customer/page', { params: query })
    list.value = res.data.data.records
    total.value = res.data.data.total
  } finally { loading.value = false }
}
function handleSearch() { query.page = 1; fetchData() }
function handleReset() { query.name = ''; query.contact = ''; query.phone = ''; query.address = ''; handleSearch() }
function handleExport() { downloadFile('/customer/export', '客户.xlsx') }
async function handleDelete(row: Customer) {
  try {
    await ElMessageBox.confirm(`确定作废客户「${row.name}」？`, '确认作废', { type: 'warning' })
    await request.delete(`/customer/${row.id}`)
    ElMessage.success('已作废'); fetchData()
  } catch {}
}
function openCreate() { Object.assign(form, { id: undefined, name: '', code: '', contact: '', phone: '', address: '', remark: '' }); dialogVisible.value = true }
function openEdit(row: Customer) { Object.assign(form, row); dialogVisible.value = true }
async function handleSave() {
  if (!form.name) return
  if ((form as any).id) await request.put(`/customer/${(form as any).id}`, form)
  else await request.post('/customer', form)
  ElMessage.success('保存成功'); dialogVisible.value = false; fetchData()
}
onMounted(fetchData)
</script>

<template>
  <div>
    <div class="page-header"><h2>客户管理</h2><div><el-button @click="handleExport">导出Excel</el-button><el-button type="primary" @click="openCreate">新增客户</el-button></div></div>
    <div class="search-bar">
      <el-input v-model="query.name" placeholder="客户名称" clearable style="width:180px" @keyup.enter="handleSearch" @clear="handleSearch" />
      <el-input v-model="query.contact" placeholder="联系人" clearable style="width:120px" @keyup.enter="handleSearch" @clear="handleSearch" />
      <el-input v-model="query.phone" placeholder="联系电话" clearable style="width:150px" @keyup.enter="handleSearch" @clear="handleSearch" />
      <el-input v-model="query.address" placeholder="地址" clearable style="width:200px" @keyup.enter="handleSearch" @clear="handleSearch" />
      <el-button type="primary" @click="handleSearch">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>
    <div class="table-container">
      <el-table :data="list" v-loading="loading" stripe border>
        <el-table-column prop="code" label="编码" width="150" />
        <el-table-column prop="name" label="名称" min-width="180" />
        <el-table-column prop="contact" label="联系人" width="120" />
        <el-table-column prop="phone" label="电话" width="140" />
        <el-table-column prop="address" label="地址" min-width="200" />
        <el-table-column prop="remark" label="备注" min-width="160" show-overflow-tooltip />
        <el-table-column prop="createTime" label="新增时间" width="170" sortable>
          <template #default="{ row }">{{ row.createTime ? row.createTime.substring(0, 16) : '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="190" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="userStore.isAdmin" size="small" type="danger" @click="handleDelete(row)">作废</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:page-size="query.size" :total="total" layout="total, sizes, prev, pager, next" style="margin-top:16px;justify-content:flex-end" @current-change="query.page = $event; fetchData()" @size-change="query.page = 1; fetchData()" />
    </div>
    <el-dialog v-model="dialogVisible" :title="(form as any).id ? '编辑客户' : '新增客户'" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="编码"><span style="color:#999;line-height:32px;">{{ form.code || '自动生成' }}</span></el-form-item>
        <el-form-item label="名称" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="联系人"><el-input v-model="form.contact" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="地址"><el-input v-model="form.address" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="handleSave">保存</el-button></template>
    </el-dialog>
  </div>
</template>
