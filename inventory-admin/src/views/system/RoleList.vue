<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '../../api/request'
import { ElMessage } from 'element-plus'

interface Role { id: number; name: string; code: string; description: string; status: number }

const loading = ref(false)
const list = ref<Role[]>([])
const dialogVisible = ref(false)
const form = ref<Partial<Role>>({ name: '', code: '', description: '' })

async function fetchData() {
  loading.value = true
  try { const res = await request.get('/role/list'); list.value = res.data.data } finally { loading.value = false }
}
function openCreate() { form.value = { name: '', code: '', description: '' }; dialogVisible.value = true }
function openEdit(row: Role) { form.value = { ...row }; dialogVisible.value = true }
async function handleSave() {
  if (!form.value.name || !form.value.code) { ElMessage.warning('请填写完整'); return }
  if ((form.value as any).id) await request.put(`/role/${(form.value as any).id}`, form.value)
  else await request.post('/role', form.value)
  ElMessage.success('保存成功'); dialogVisible.value = false; fetchData()
}
onMounted(fetchData)
</script>

<template>
  <div>
    <div class="page-header"><h2>角色管理</h2><el-button type="primary" @click="openCreate">新增角色</el-button></div>
    <div class="table-container">
      <el-table :data="list" v-loading="loading" stripe border>
        <el-table-column prop="name" label="角色名称" width="160" />
        <el-table-column prop="code" label="角色编码" width="160" />
        <el-table-column prop="description" label="描述" min-width="200" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }"><el-button size="small" @click="openEdit(row)">编辑</el-button></template>
        </el-table-column>
      </el-table>
    </div>
    <el-dialog v-model="dialogVisible" :title="(form as any).id ? '编辑角色' : '新增角色'" width="450px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="角色名称" required><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="角色编码" required><el-input v-model="form.code" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="handleSave">保存</el-button></template>
    </el-dialog>
  </div>
</template>
