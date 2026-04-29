<script setup lang="ts">
import { ref, onMounted } from 'vue'
import request from '../../api/request'
import { ElMessage } from 'element-plus'

interface SysConfig { id: number; configKey: string; configValue: string; remark: string }

const loading = ref(false)
const list = ref<SysConfig[]>([])

async function fetchData() {
  loading.value = true
  try {
    const res = await request.get('/system/config')
    list.value = res.data.data
  } finally { loading.value = false }
}

async function handleSave(row: SysConfig) {
  await request.put(`/system/config/${row.id}`, { configValue: row.configValue })
  ElMessage.success('已更新')
}

onMounted(fetchData)
</script>

<template>
  <div>
    <div class="page-header"><h2>系统配置</h2></div>
    <div class="table-container">
      <el-table :data="list" v-loading="loading" stripe border>
        <el-table-column label="配置项" width="220">
          <template #default="{ row }">
            <el-tag>{{ row.configKey }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="配置值" min-width="250">
          <template #default="{ row }">
            <el-input v-model="row.configValue" size="small" style="width:220px" />
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="说明" min-width="200" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="handleSave(row)">保存</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>
