<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import request from '../../api/request'
import type { SysUser, PageParams } from '../../types/api'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const list = ref<SysUser[]>([])
const total = ref(0)
const query = reactive<PageParams & { username?: string; realName?: string; roleType?: string }>({ page: 1, size: 10, username: '', realName: '', roleType: '' })
const dialogVisible = ref(false)
const pwdDialogVisible = ref(false)
const pwdForm = reactive<any>({ id: null, username: '', newPassword: '' })
const form = reactive<any>({ username: '', realName: '', position: '', phone: '', email: '', password: '', role: 2 })

async function fetchData() {
  loading.value = true
  try {
    const res = await request.get('/user/page', { params: query })
    list.value = res.data.data.records; total.value = res.data.data.total
  } finally { loading.value = false }
}
function handleSearch() { query.page = 1; fetchData() }
function handleReset() { query.username = ''; query.realName = ''; query.roleType = ''; handleSearch() }
function openCreate() { Object.assign(form, { username: '', realName: '', position: '', phone: '', email: '', password: '', role: 2 }); dialogVisible.value = true }
function openEdit(row: any) {
  Object.assign(form, { ...row, password: '' })
  dialogVisible.value = true
}
async function handleSave() {
  if (!form.username) { ElMessage.warning('请输入用户名'); return }
  try {
    if (form.id) await request.put(`/user/${form.id}`, form)
    else await request.post('/user', form)
    ElMessage.success('保存成功'); dialogVisible.value = false; fetchData()
  } catch { /* handled */ }
}
function openPwdDialog(row: any) {
  pwdForm.id = row.id
  pwdForm.username = row.username
  pwdForm.newPassword = ''
  pwdDialogVisible.value = true
}
async function handleDelete(row: any) {
  if (row.username === 'admin') { ElMessage.warning('admin账号不可删除'); return }
  try {
    await request.delete(`/user/${row.id}`)
    ElMessage.success('已删除'); fetchData()
  } catch { /* handled */ }
}
async function handleToggleStatus(row: any) {
  if (row.username === 'admin') { ElMessage.warning('admin账号不可禁用'); return }
  const s = row.status === 1 ? 0 : 1
  await request.put(`/user/${row.id}`, { ...row, status: s })
  ElMessage.success(s === 1 ? '已启用' : '已禁用'); fetchData()
}
async function handleResetPwd() {
  if (!pwdForm.newPassword || pwdForm.newPassword.length < 4) { ElMessage.warning('密码至少4位'); return }
  await request.put(`/user/${pwdForm.id}`, { id: pwdForm.id, password: pwdForm.newPassword })
  ElMessage.success('密码已更新'); pwdDialogVisible.value = false
}
onMounted(fetchData)
</script>

<template>
  <div>
    <div class="page-header"><h2>员工管理</h2><el-button type="primary" @click="openCreate">+ 新增员工</el-button></div>
    <div class="search-bar">
      <el-input v-model="query.username" placeholder="账号" clearable style="width:150px" @keyup.enter="handleSearch" @clear="handleSearch" />
      <el-input v-model="query.realName" placeholder="姓名" clearable style="width:150px" @keyup.enter="handleSearch" @clear="handleSearch" />
      <el-select v-model="query.roleType" placeholder="权限" clearable style="width:130px" @change="handleSearch">
        <el-option label="管理员" value="admin" />
        <el-option label="员工" value="employee" />
      </el-select>
      <el-button type="primary" @click="handleSearch">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>
    <div class="table-container">
      <el-table :data="list" v-loading="loading" stripe border>
        <el-table-column prop="username" label="账号" width="120" />
        <el-table-column prop="realName" label="姓名" width="100" />
        <el-table-column prop="position" label="职位" width="100" />
        <el-table-column label="权限" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.role === 1" type="danger" size="small">管理员</el-tag>
            <el-tag v-else type="success" size="small">员工</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="电话" width="130" />
        <el-table-column prop="status" label="状态" width="70">
          <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" sortable>
          <template #default="{ row }">{{ row.createTime ? row.createTime.substring(0, 16) : '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" @click="openPwdDialog(row)">改密</el-button>
            <el-button size="small" :type="row.status === 1 ? 'warning' : 'success'" @click="handleToggleStatus(row)">{{ row.status === 1 ? '禁用' : '启用' }}</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)" :disabled="row.username === 'admin'">{{ row.username === 'admin' ? '不可删除' : '删除' }}</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:page-size="query.size" :total="total" layout="total, sizes, prev, pager, next" style="margin-top:16px;justify-content:flex-end" @current-change="query.page = $event; fetchData()" @size-change="query.page = 1; fetchData()" />
    </div>
    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑员工' : '新增员工'" width="480px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="账号" required><el-input v-model="form.username" /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item v-if="!form.id" label="密码" required><el-input v-model="form.password" type="password" /></el-form-item>
        <el-form-item label="权限">
          <el-radio-group v-model="form.role" :disabled="form.username === 'admin'">
            <el-radio :value="1">管理员（全部权限）</el-radio>
            <el-radio :value="2">员工（仅操作权限）</el-radio>
          </el-radio-group>
          <div v-if="form.username === 'admin'" style="color:#999;font-size:12px;margin-top:4px;">admin账号权限不可修改</div>
        </el-form-item>
        <el-form-item label="职位"><el-input v-model="form.position" placeholder="如：销售员、仓管员、养护工" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="handleSave">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="pwdDialogVisible" title="修改密码" width="400px">
      <el-form :model="pwdForm" label-width="80px">
        <el-form-item label="账号"><el-input :model-value="pwdForm.username" disabled /></el-form-item>
        <el-form-item label="新密码" required><el-input v-model="pwdForm.newPassword" type="password" show-password /></el-form-item>
      </el-form>
      <template #footer><el-button @click="pwdDialogVisible = false">取消</el-button><el-button type="primary" @click="handleResetPwd">确认修改</el-button></template>
    </el-dialog>
  </div>
</template>
