<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '../../api/request'
import type { Category } from '../../types/api'
import { ElMessage, ElMessageBox, ElForm } from 'element-plus'

const loading = ref(false)
const categories = ref<Category[]>([])
const dialogVisible = ref(false)
const formRef = ref<InstanceType<typeof ElForm>>()
const form = ref<{ id?: number; name: string; parentId: number | null; sort: number }>({
  name: '',
  parentId: null,
  sort: 0,
})

async function fetchTree() {
  loading.value = true
  try {
    const res = await request.get('/category/tree')
    categories.value = res.data.data
  } finally {
    loading.value = false
  }
}

function openCreate(parentId: number | null = null) {
  form.value = { name: '', parentId, sort: 0 }
  dialogVisible.value = true
}

function openEdit(row: any) {
  form.value = { id: row.id, name: row.name, parentId: row.parentId, sort: row.sort }
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.value.name) { ElMessage.warning('请输入分类名称'); return }
  try {
    if (form.value.id) {
      await request.put(`/category/${form.value.id}`, form.value)
    } else {
      await request.post('/category', form.value)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    fetchTree()
  } catch { /* handled */ }
}

async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('确定作废该分类？', '确认作废', { type: 'warning' })
    await request.delete(`/category/${id}`)
    ElMessage.success('已作废')
    fetchTree()
  } catch {}
}

onMounted(fetchTree)
</script>

<template>
  <div>
    <div class="page-header">
      <h2>商品分类</h2>
      <el-button type="primary" @click="openCreate(null)">新增根分类</el-button>
    </div>

    <div class="table-container">
      <el-table :data="categories" v-loading="loading" row-key="id" stripe border default-expand-all :tree-props="{ children: 'children', hasChildren: 'hasChildren' }">
        <el-table-column label="分类名称" min-width="280">
          <template #default="{ row }">
            <span :style="{ fontWeight: row.parentId ? 'normal' : 'bold', color: row.parentId ? '#666' : '#1b5e20', marginLeft: row.parentId ? '0' : '0' }">
              {{ row.parentId ? '  └ ' : '📁 ' }}{{ row.name }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openCreate(row.id)">+ 子分类</el-button>
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row.id)">作废</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑分类' : '新增分类'" width="400px">
      <el-form ref="formRef" :model="form" label-width="80px">
        <el-form-item label="分类名称" required>
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
