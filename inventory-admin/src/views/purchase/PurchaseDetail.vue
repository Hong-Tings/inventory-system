<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '../../api/request'
import type { PurchaseOrder } from '../../types/api'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const order = ref<PurchaseOrder | null>(null)

const statusMap: Record<number, { label: string; type: string }> = {
  0: { label: '草稿', type: 'info' },
  1: { label: '已入库', type: 'success' },
  2: { label: '已取消', type: 'danger' },
}

async function fetchDetail() {
  loading.value = true
  try {
    const res = await request.get(`/purchase-order/${route.params.id}`)
    order.value = res.data.data
  } catch {
    ElMessage.error('获取入库单详情失败')
    order.value = null
  } finally { loading.value = false }
}

async function handleCancel() {
  try {
    await ElMessageBox.confirm('确定取消此入库单？取消后库存将回退，不可恢复。', '确认取消', { type: 'warning' })
  } catch { return }
  await request.put(`/purchase-order/${route.params.id}/cancel`)
  ElMessage.success('已取消'); fetchDetail()
}

function handlePrint() {
  window.print()
}

onMounted(fetchDetail)
</script>

<template>
  <div v-loading="loading">
    <div class="page-header">
      <h2>入库单详情</h2>
      <div>
        <el-button @click="router.push('/purchase')">返回列表</el-button>
        <el-button @click="handlePrint">打印</el-button>
        <el-button v-if="order?.status === 0" type="danger" @click="handleCancel">取消入库</el-button>
      </div>
    </div>
    <div class="detail-card" v-if="order">
      <h3>基本信息</h3>
      <el-descriptions :column="3" border>
        <el-descriptions-item label="入库单号">{{ order.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="供应商">{{ order.supplierName }}</el-descriptions-item>
        <el-descriptions-item label="仓库">{{ order.warehouseName }}</el-descriptions-item>
        <el-descriptions-item label="入库日期">{{ order.orderDate }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusMap[order.status]?.type as any">{{ statusMap[order.status]?.label }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="操作人">{{ order.operatorName }}</el-descriptions-item>
        <el-descriptions-item label="总金额">¥{{ order.totalAmount?.toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="总数量">{{ order.totalQuantity }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="3">{{ order.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </div>
    <div class="detail-card" v-if="order?.items">
      <h3>商品明细</h3>
      <el-table :data="order.items" border stripe>
        <el-table-column prop="productName" label="商品名称" min-width="160" />
        <el-table-column prop="productCode" label="编码" width="120" />
        <el-table-column prop="quantity" label="数量" width="80" />
        <el-table-column prop="unitPrice" label="单价" width="100">
          <template #default="{ row }">¥{{ row.unitPrice?.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="amount" label="金额" width="100">
          <template #default="{ row }">¥{{ row.amount?.toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="batchNo" label="批次号" width="140" />
        <el-table-column prop="productionDate" label="生产日期" width="120" />
        <el-table-column prop="expiryDate" label="有效期" width="120" />
      </el-table>
    </div>
  </div>
</template>

<style>
@media print {
  .sidebar, .navbar, .page-header div, .el-button, .el-pagination { display: none !important; }
  .detail-card { box-shadow: none !important; border: 1px solid #ddd !important; }
  body { background: white !important; }
  @page { margin: 1.5cm; }
}
</style>
