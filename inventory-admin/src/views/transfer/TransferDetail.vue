<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '../../api/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '../../store/user'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const order = ref<any | null>(null)
const userStore = useUserStore()

const statusMap: Record<number, { label: string; type: string }> = {
  0: { label: '草稿', type: 'info' },
  1: { label: '已完成', type: 'success' },
  2: { label: '已取消', type: 'danger' },
  4: { label: '待审批', type: 'warning' },
}

async function fetchDetail() {
  loading.value = true
  try {
    const res = await request.get(`/transfer/${route.params.id}`)
    order.value = res.data.data
  } catch {
    ElMessage.error('获取调拨单详情失败')
    order.value = null
  } finally { loading.value = false }
}

async function handleSubmit() {
  try { await ElMessageBox.confirm('提交后进入审批状态，请联系管理员审批通过。', '提交审批', { type: 'info' }) } catch { return }
  await request.put(`/transfer/${route.params.id}/submit`)
  ElMessage.success('已提交审批'); fetchDetail()
}

async function handleCancel() {
  try { await ElMessageBox.confirm('确定取消此调拨单？', '确认取消', { type: 'warning' }) } catch { return }
  await request.put(`/transfer/${route.params.id}/cancel`)
  ElMessage.success('已取消'); fetchDetail()
}

async function handleApprove() {
  try {
    await ElMessageBox.confirm('确认审核通过此调拨单？通过后将更新库存。', '审核确认', { type: 'info' })
  } catch { return }
  await request.put(`/transfer/${route.params.id}/approve`)
  ElMessage.success('已审核通过'); fetchDetail()
}

async function handleReject() {
  try {
    const { value } = await ElMessageBox.prompt('确定驳回此调拨单？', '驳回', {
      inputPlaceholder: '请填写驳回原因（必填）',
      inputValidator: (val: string) => !!val.trim(),
      inputErrorMessage: '驳回原因不能为空',
      type: 'warning',
    })
    await request.put(`/transfer/${route.params.id}/reject`, { reason: value })
    ElMessage.success('已驳回'); fetchDetail()
  } catch {}
}

onMounted(fetchDetail)
</script>

<template>
  <div v-loading="loading">
    <div class="page-header">
      <h2>调拨单详情</h2>
      <div>
        <el-button @click="router.push('/transfer')">返回列表</el-button>
        <el-button v-if="order?.status === 0" type="primary" @click="handleSubmit">提交审批</el-button>
        <el-button v-if="userStore.isAdmin && order?.status === 4" type="success" @click="handleApprove">审核通过</el-button>
        <el-button v-if="userStore.isAdmin && order?.status === 4" type="warning" @click="handleReject">驳回</el-button>
        <el-button v-if="order?.status === 0 || order?.status === 4" type="danger" @click="handleCancel">取消调拨</el-button>
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
        <el-descriptions-item v-if="order.approverName" label="审核人">{{ order.approverName }}</el-descriptions-item>
        <el-descriptions-item v-if="order.approveTime" label="审核时间">{{ order.approveTime }}</el-descriptions-item>
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
