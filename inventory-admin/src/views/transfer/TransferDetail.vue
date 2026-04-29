<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '../../api/request'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const order = ref<any | null>(null)

const statusMap: Record<number, { label: string; type: string }> = {
  0: { label: '草稿', type: 'info' },
  1: { label: '已完成', type: 'success' },
  2: { label: '已取消', type: 'danger' },
}

async function fetchDetail() {
  loading.value = true
  try {
    const res = await request.get(`/transfer/${route.params.id}`)
    order.value = res.data.data
  } finally { loading.value = false }
}

async function handleSubmit() {
  await request.put(`/transfer/${route.params.id}/submit`)
  ElMessage.success('已调拨')
  fetchDetail()
}

async function handleCancel() {
  await request.put(`/transfer/${route.params.id}/cancel`)
  ElMessage.success('已取消')
  fetchDetail()
}

onMounted(fetchDetail)
</script>

<template>
  <div v-loading="loading">
    <div class="page-header">
      <h2>调拨单详情</h2>
      <div>
        <el-button @click="router.push('/transfer')">返回列表</el-button>
        <el-button v-if="order?.status === 0" type="primary" @click="handleSubmit">确认调拨</el-button>
        <el-button v-if="order?.status === 0" type="danger" @click="handleCancel">取消调拨</el-button>
      </div>
    </div>
    <div class="detail-card" v-if="order">
      <h3>基本信息</h3>
      <el-descriptions :column="3" border>
        <el-descriptions-item label="调拨单号">{{ order.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="调出仓库">{{ order.outWarehouseName }}</el-descriptions-item>
        <el-descriptions-item label="调入仓库">{{ order.inWarehouseName }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusMap[order.status]?.type as any">{{ statusMap[order.status]?.label }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="操作人">{{ order.operatorName }}</el-descriptions-item>
        <el-descriptions-item label="日期">{{ order.orderDate }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="3">{{ order.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </div>
    <div class="detail-card" v-if="order?.items">
      <h3>商品明细</h3>
      <el-table :data="order.items" border stripe>
        <el-table-column prop="productName" label="商品名称" min-width="180" />
        <el-table-column prop="quantity" label="数量" width="80" />
        <el-table-column prop="batchNo" label="批次号" width="140" />
      </el-table>
    </div>
  </div>
</template>
