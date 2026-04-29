<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '../../api/request'
import type { SalesOrder } from '../../types/api'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const order = ref<SalesOrder | null>(null)
const statusMap: Record<number, { label: string; type: string }> = {
  0: { label: '草稿', type: 'info' }, 1: { label: '已出库', type: 'success' }, 2: { label: '已取消', type: 'danger' },
}
async function fetchDetail() {
  loading.value = true
  try {
    const res = await request.get(`/sales-order/${route.params.id}`)
    order.value = res.data.data
  } finally { loading.value = false }
}
async function handleCancel() { await request.put(`/sales-order/${route.params.id}/cancel`); ElMessage.success('已取消'); fetchDetail() }
onMounted(fetchDetail)
</script>

<template>
  <div v-loading="loading">
    <div class="page-header">
      <h2>出库单详情</h2>
      <div>
        <el-button @click="router.push('/sales')">返回列表</el-button>
        <el-button v-if="order?.status === 0" type="danger" @click="handleCancel">取消出库</el-button>
      </div>
    </div>
    <div class="detail-card" v-if="order">
      <h3>基本信息</h3>
      <el-descriptions :column="3" border>
        <el-descriptions-item label="出库单号">{{ order.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="客户">{{ order.customerName }}</el-descriptions-item>
        <el-descriptions-item label="仓库">{{ order.warehouseName }}</el-descriptions-item>
        <el-descriptions-item label="出库日期">{{ order.orderDate }}</el-descriptions-item>
        <el-descriptions-item label="销售员">{{ order.salesman || '-' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusMap[order.status]?.type as any">{{ statusMap[order.status]?.label }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="外部单号">{{ order.externalOrderNo || '-' }}</el-descriptions-item>
        <el-descriptions-item label="总金额">¥{{ order.totalAmount?.toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="总数量">{{ order.totalQuantity }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ order.operatorName }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ order.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </div>
    <div class="detail-card" v-if="order?.items">
      <h3>商品明细</h3>
      <el-table :data="order.items" border stripe>
        <el-table-column prop="productName" label="商品名称" min-width="160" />
        <el-table-column prop="productCode" label="编码" width="120" />
        <el-table-column prop="quantity" label="数量" width="80" />
        <el-table-column prop="unitPrice" label="售价" width="100">
          <template #default="{ row }">¥{{ row.unitPrice?.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="amount" label="金额" width="100">
          <template #default="{ row }">¥{{ row.amount?.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="batchNo" label="批次号" width="140" />
      </el-table>
    </div>
  </div>
</template>
