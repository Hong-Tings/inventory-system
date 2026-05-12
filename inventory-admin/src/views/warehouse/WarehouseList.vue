<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request, { downloadFile } from '../../api/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../../store/user'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const treeData = ref<any[]>([])
const dialogVisible = ref(false)
const parentCandidates = ref<any[]>([])

const query = ref({ keyword: '', name: '', status: undefined as number | undefined })
const form = reactive<any>({ name: '', code: '', contact: '', phone: '', address: '', status: 1, remark: '', level: 4, parentId: undefined })

async function fetchTree() {
  loading.value = true
  try {
    const params: Record<string, any> = {}
    if (query.value.keyword) params.keyword = query.value.keyword
    if (query.value.name) params.name = query.value.name
    if (query.value.status !== undefined) params.status = query.value.status

    if (query.value.keyword) {
      const res = await request.get('/warehouse/search', { params: { keyword: query.value.keyword } })
      treeData.value = res.data.data || []
    } else {
      const res = await request.get('/warehouse/tree')
      treeData.value = res.data.data || []
    }
  } finally { loading.value = false }
}
function handleSearch() { fetchTree() }
function handleReset() { query.value = { keyword: '', name: '', status: undefined }; fetchTree() }

function openCreate(parent?: any) {
  Object.assign(form, { id: undefined, name: '', code: '', contact: '', phone: '', address: '', status: 1, remark: '' })
  if (parent) {
    const childLevel = (parent.level || 1) + 1
    if (childLevel <= 4) {
      form.level = childLevel
      form.parentId = parent.id
    } else {
      ElMessage.warning('已达到最大层级')
      return
    }
  } else {
    form.level = 1
    form.parentId = undefined
  }
  parentCandidates.value = []
  dialogVisible.value = true
}
function openEdit(row: any) {
  Object.assign(form, { ...row })
  if (form.level > 1) loadParentCandidates(form.level)
  dialogVisible.value = true
}
async function loadParentCandidates(level: number) {
  if (level > 1) {
    const levelMap: Record<number, number> = { 2: 1, 3: 2, 4: 3 }
    const res = await request.get('/warehouse/page', { params: { page: 1, size: 999, level: levelMap[level as keyof typeof levelMap] } })
    parentCandidates.value = res.data.data.records
  }
}
async function handleSave() {
  if (!form.name) { ElMessage.warning('请输入仓库名称'); return }
  if (form.level === 4 && (!form.address || !form.contact || !form.phone)) { ElMessage.warning('4级仓库需填写地址、负责人和联系电话'); return }
  try {
    if (form.id) await request.put(`/warehouse/${form.id}`, form)
    else await request.post('/warehouse', form)
    ElMessage.success('保存成功'); dialogVisible.value = false; fetchTree()
  } catch { /* handled */ }
}
async function handleToggleStatus(row: any) {
  const s = row.status === 1 ? 0 : 1
  await request.put(`/warehouse/${row.id}`, { ...row, status: s })
  ElMessage.success(s === 1 ? '已启用' : '已停用'); fetchTree()
}
async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(`确定作废仓库「${row.name}」及其所有子仓库？`, '确认作废', { type: 'warning' })
    await request.delete(`/warehouse/${row.id}`)
    ElMessage.success('已作废'); fetchTree()
  } catch (err: any) {
    if (err?.response?.data?.message) ElMessage.error(err.response.data.message)
  }
}
function handleExport() { downloadFile('/warehouse/export', '仓库.xlsx') }

function onLevelChange(level: number) {
  form.parentId = undefined
  loadParentCandidates(level)
}

onMounted(fetchTree)
</script>

<template>
  <div>
    <div class="page-header"><h2>仓库管理</h2>
      <div>
        <el-button type="primary" @click="openCreate()">+ 新增1级仓库</el-button>
        <el-button @click="handleExport">导出Excel</el-button>
      </div>
    </div>

    <div class="search-bar">
      <el-input v-model="query.keyword" placeholder="搜索仓库名称/编码" clearable style="width:220px" @keyup.enter="handleSearch" @clear="handleSearch" />
      <el-input v-model="query.name" placeholder="仓库名称" clearable style="width:160px" @keyup.enter="handleSearch" @clear="handleSearch" />
      <el-select v-model="query.status" placeholder="状态" clearable style="width:120px" @change="handleSearch">
        <el-option label="启用" :value="1" /><el-option label="停用" :value="0" />
      </el-select>
      <el-button type="primary" @click="handleSearch">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <div class="table-container">
      <el-table :data="treeData" v-loading="loading" stripe border row-key="id" :tree-props="{ children: 'children' }" default-expand-all>
        <el-table-column prop="name" label="仓库名称" min-width="200">
          <template #default="{ row }">
            <span :style="{ fontWeight: row.level === 1 ? 'bold' : 'normal', color: row.status === 0 ? '#999' : '' }">{{ row.name }}</span>
            <el-tag size="small" style="margin-left:6px;">{{ row.level }}级</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="code" label="编码" width="140" />
        <el-table-column prop="address" label="地址" width="160" show-overflow-tooltip />
        <el-table-column prop="contact" label="负责人" width="100" />
        <el-table-column prop="phone" label="联系电话" width="140" />
        <el-table-column label="商品总数" width="100" sortable>
          <template #default="{ row }"><span style="font-weight:600;">{{ row.productCount ?? '-' }}</span></template>
        </el-table-column>
        <el-table-column label="库存金额" width="120" sortable>
          <template #default="{ row }">¥{{ row.totalAmount?.toFixed(2) ?? '0.00' }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }"><el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">{{ row.status === 1 ? '启用' : '停用' }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button v-if="row.level < 4" size="small" type="primary" @click="openCreate(row)">+ 新增子级</el-button>
            <el-button size="small" :type="row.status === 1 ? 'warning' : 'success'" @click="handleToggleStatus(row)">{{ row.status === 1 ? '停用' : '启用' }}</el-button>
            <el-button v-if="userStore.isAdmin" size="small" type="danger" @click="handleDelete(row)">作废</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑仓库' : '新增仓库'" width="520px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="仓库名称" required><el-input v-model="form.name" placeholder="如：一号仓库、门店仓库" /></el-form-item>
        <el-form-item label="仓库编码"><span style="color:#999;line-height:32px;">{{ form.code || '自动生成' }}</span></el-form-item>
        <el-form-item label="仓库地址" :required="form.level === 4"><el-input v-model="form.address" :placeholder="form.level === 4 ? 'XX市XX区XX路XX号' : '虚拟节点无需地址'" /></el-form-item>
        <el-form-item label="层级" required>
          <el-select v-model="form.level" placeholder="选择层级" @change="onLevelChange">
            <el-option label="1级" :value="1" />
            <el-option label="2级" :value="2" />
            <el-option label="3级" :value="3" />
            <el-option label="4级" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="上级仓库" v-if="form.level && form.level > 1">
          <el-select v-model="form.parentId" placeholder="选择上级" filterable>
            <el-option v-for="w in parentCandidates" :key="w.id" :label="w.name" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-row :gutter="16" v-if="form.level === 4">
          <el-col :span="12"><el-form-item label="负责人" required><el-input v-model="form.contact" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="联系电话" required><el-input v-model="form.phone" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="备注"><el-input v-model="form.remark" type="textarea" :rows="2" placeholder="如：常温仓、冷冻仓" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="dialogVisible = false">取消</el-button><el-button type="primary" @click="handleSave">保存</el-button></template>
    </el-dialog>
  </div>
</template>
