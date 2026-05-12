<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import request from '@/api/request'
import FloatingHome from '@/components/FloatingHome'

const order = ref(null)
const loading = ref(false)
const actionLoading = ref(false)
const statusMap = { 0: '草稿', 1: '已入库', 2: '已取消', 4: '待审批' }

let id = null

onLoad(async (options) => {
  if (options?.id) {
    id = options.id
  } else if (options?.orderNo) {
    const res = await request.get('/purchase-order/page', { params: { orderNo: options.orderNo, page: 1, size: 1 } })
    if (res.data.records?.length) id = res.data.records[0].id
  }
  if (id) fetchDetail()
})

async function fetchDetail() {
  if (!id) return
  loading.value = true
  try {
    const res = await request.get(`/purchase-order/${id}`)
    order.value = res.data
  } finally { loading.value = false }
}

async function submitOrder() {
  actionLoading.value = true
  try {
    await request.put(`/purchase-order/${id}/submit`)
    uni.showToast({ title: '已提交', icon: 'success' })
    fetchDetail()
  } finally { actionLoading.value = false }
}

async function cancelOrder() {
  uni.showModal({
    title: '确认取消', content: '确定要取消此入库单吗？',
    success: async (r) => {
      if (!r.confirm) return
      actionLoading.value = true
      try {
        await request.put(`/purchase-order/${id}/cancel`)
        uni.showToast({ title: '已取消', icon: 'success' })
        fetchDetail()
      } finally { actionLoading.value = false }
    },
  })
}
</script>

<template>
  <view class="page">
    <view v-if="loading" class="loading">加载中...</view>
    <view v-else-if="order">
      <!-- 单号状态卡片 -->
      <view class="detail-card" style="text-align:center;padding:24px 16px;">
        <text style="font-size:20px;font-weight:700;color:#1a1a1a;display:block;">{{ order.orderNo }}</text>
        <text style="display:inline-block;margin-top:8px;font-size:12px;padding:4px 16px;border-radius:12px;font-weight:500;"
          :style="{
            background: order.status === 1 ? '#e8f5e9' : order.status === 2 ? '#fce4ec' : order.status === 4 ? '#fff3e0' : '#f5f5f5',
            color: order.status === 1 ? '#2e7d32' : order.status === 2 ? '#c62828' : order.status === 4 ? '#e65100' : '#888'
          }">
          {{ statusMap[order.status] || '未知' }}
        </text>
      </view>

      <!-- 基本信息 -->
      <view class="detail-card">
        <text class="section-title">基本信息</text>
        <view class="detail-divider"></view>
        <view style="display:grid;grid-template-columns:1fr 1fr;gap:12px;">
          <view>
            <text class="detail-label">供应商</text>
            <text class="detail-value">{{ order.supplierName || '-' }}</text>
          </view>
          <view>
            <text class="detail-label">仓库</text>
            <text class="detail-value">{{ order.warehouseName || '-' }}</text>
          </view>
          <view>
            <text class="detail-label">入库日期</text>
            <text class="detail-value">{{ order.orderDate || '-' }}</text>
          </view>
          <view>
            <text class="detail-label">操作人</text>
            <text class="detail-value">{{ order.operatorName || '-' }}</text>
          </view>
        </view>
        <view v-if="order.remark" style="margin-top:12px;">
          <text class="detail-label">备注</text>
          <text style="font-size:13px;color:#666;display:block;margin-top:4px;">{{ order.remark }}</text>
        </view>
      </view>

      <!-- 商品明细 -->
      <view class="detail-card">
        <text class="section-title">商品明细</text>
        <view class="detail-divider"></view>
        <view v-if="order.items && order.items.length">
          <view v-for="(item, idx) in order.items" :key="item.id">
            <view style="display:flex;justify-content:space-between;align-items:center;padding:12px 0;">
              <view style="flex:1;">
                <text style="font-weight:600;font-size:14px;color:#1a1a1a;display:block;">{{ item.productName || '-' }}</text>
                <text style="font-size:12px;color:#999;margin-top:2px;display:block;">
                  编码: {{ item.productCode }}　|　批次: {{ item.batchNo || '-' }}
                </text>
              </view>
              <view style="text-align:right;">
                <text style="font-size:14px;font-weight:600;color:#e65100;display:block;">¥{{ item.amount?.toFixed(2) }}</text>
                <text style="font-size:12px;color:#999;">{{ item.quantity }} × ¥{{ item.unitPrice?.toFixed(2) }}</text>
              </view>
            </view>
            <view v-if="idx < order.items.length - 1" style="height:1px;background:#f0f4f0;"></view>
          </view>
          <view style="display:flex;justify-content:space-between;padding:14px 0 0;margin-top:8px;border-top:2px solid #e8f5e9;font-weight:700;font-size:16px;">
            <text style="color:#333;">合计</text>
            <text style="color:#e65100;">¥{{ order.totalAmount?.toFixed(2) }}</text>
          </view>
        </view>
        <view v-else style="text-align:center;color:#aaa;padding:20px;">暂无明细</view>
      </view>
    </view>
    <view v-else class="empty">未找到入库单</view>

    <view style="display:flex;gap:10px;margin-top:12px;padding-bottom:20px;">
      <button v-if="order?.status === 0" class="btn-pri" :loading="actionLoading" @click="submitOrder">提交入库</button>
      <button v-if="order?.status === 0" class="btn-sec" :loading="actionLoading" @click="cancelOrder">取消</button>
    </view>
    <FloatingHome />
  </view>
</template>

<style scoped>
.btn-pri { flex:1; background:linear-gradient(135deg,#2e7d32,#43a047); color:#fff; border:none; border-radius:10px; height:44px; line-height:44px; font-size:15px; font-weight:600; box-shadow:0 4px 12px rgba(46,125,50,0.25); }
.btn-sec { flex:1; background:#fff; color:#666; border:1px solid #e0e0e0; border-radius:10px; height:44px; line-height:44px; font-size:15px; }
</style>
