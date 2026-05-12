<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import request from '@/api/request'
import FloatingHome from '@/components/FloatingHome'

const order = ref(null)
const loading = ref(false)
const actionLoading = ref(false)
const statusMap = { 0: '草稿', 1: '已完成', 2: '已取消', 4: '待审批' }

let id = null
onLoad(async (options) => {
  if (options?.id) {
    id = options.id
  } else if (options?.orderNo) {
    const res = await request.get('/transfer/page', { params: { orderNo: options.orderNo, page: 1, size: 1 } })
    if (res.data.records?.length) id = res.data.records[0].id
  }
  if (id) fetchDetail()
})

async function fetchDetail() {
  if (!id) return
  loading.value = true
  try {
    const res = await request.get(`/transfer/${id}`)
    order.value = res.data
  } finally { loading.value = false }
}

async function submitOrder() {
  actionLoading.value = true
  try {
    await request.put(`/transfer/${id}/submit`)
    uni.showToast({ title: '已提交审批', icon: 'success' })
    fetchDetail()
  } finally { actionLoading.value = false }
}

async function cancelOrder() {
  uni.showModal({
    title: '确认取消', content: '确定要取消此调拨单吗？',
    success: async (r) => {
      if (!r.confirm) return
      actionLoading.value = true
      try {
        await request.put(`/transfer/${id}/cancel`)
        uni.showToast({ title: '已取消', icon: 'success' })
        fetchDetail()
      } finally { actionLoading.value = false }
    },
  })
}
</script>

<template>
  <view class="page">
    <view v-if="loading" style="text-align:center;padding:40px;color:#999;">加载中...</view>
    <view v-else-if="order">
      <view class="dh">
        <text class="dh-no">{{ order.orderNo }}</text>
        <text class="dh-st" :class="'dh-' + order.status">{{ statusMap[order.status] || '未知' }}</text>
      </view>
      <view class="ig">
        <view class="r"><text class="l">调出仓库</text><text class="v">{{ order.fromWarehouseName || '-' }}</text></view>
        <view class="r"><text class="l">调入仓库</text><text class="v">{{ order.toWarehouseName || '-' }}</text></view>
        <view class="r"><text class="l">数量</text><text class="v">{{ order.totalQuantity }}</text></view>
        <view class="r"><text class="l">调拨日期</text><text class="v">{{ order.orderDate || '-' }}</text></view>
        <view v-if="order.approverName" class="r"><text class="l">审核人</text><text class="v">{{ order.approverName }}</text></view>
        <view v-if="order.approveTime" class="r"><text class="l">审核时间</text><text class="v">{{ order.approveTime.substring(0, 16) }}</text></view>
      </view>

      <!-- 商品明细 -->
      <view class="pl">
        <text class="pl-title">商品明细</text>
        <view v-if="order.items && order.items.length">
          <view class="pi" v-for="item in order.items" :key="item.id">
            <view><text class="pi-name">{{ item.productName || '-' }}</text><text class="pi-detail">数量: {{ item.quantity }}  批次: {{ item.batchNo || '-' }}</text></view>
          </view>
        </view>
        <view v-else style="text-align:center;color:#999;padding:20px;">暂无明细</view>
      </view>
    </view>
    <view v-else style="text-align:center;padding:40px;color:#999;">未找到调拨单</view>

    <view class="action-bar">
      <button v-if="order?.status === 0" class="btn-submit" :loading="actionLoading" @click="submitOrder">提交调拨</button>
      <button v-if="order?.status === 0" class="btn-edit" :loading="actionLoading" @click="uni.navigateTo({ url: '/pages/transfer/create?id=' + order.id })">编辑</button>
      <button v-if="order?.status === 0" class="btn-cancel" :loading="actionLoading" @click="cancelOrder">取消</button>
    </view>
    <FloatingHome />
  </view>
</template>

<style scoped>
.page { padding: 12px; background: #f5f7f5; min-height: 100vh; }
.dh { background: #fff; border-radius: 8px; padding: 20px 16px; text-align: center; margin-bottom: 10px; }
.dh-no { font-size: 18px; font-weight: 700; display: block; }
.dh-st { display: inline-block; margin-top: 6px; font-size: 12px; padding: 3px 14px; border-radius: 12px; }
.dh-1 { background: #e8f5e9; color: #2e7d32; }
.dh-0 { background: #f0f0f0; color: #999; }
.dh-2 { background: #fbe9e7; color: #c62828; } .dh-4 { background: #fff3e0; color: #e65100; }
.ig { background: #fff; border-radius: 8px; padding: 16px; margin-bottom: 10px; }
.ig .r { display: flex; justify-content: space-between; padding: 10px 0; border-bottom: 1px solid #f0f0f0; font-size: 14px; }
.ig .r:last-child { border-bottom: none; }
.ig .l { color: #999; } .ig .v { font-weight: 500; }
.pl { background: #fff; border-radius: 8px; padding: 16px; margin-bottom: 10px; }
.pl-title { font-size: 14px; font-weight: 600; margin-bottom: 10px; display: block; }
.pi { display: flex; justify-content: space-between; padding: 10px 0; border-bottom: 1px solid #f5f5f5; font-size: 13px; }
.pi-name { font-weight: 500; display: block; } .pi-detail { color: #666; font-size: 12px; }
.action-bar { display: flex; gap: 10px; margin-top: 12px; padding-bottom: 20px; }
.btn-submit { flex: 1; background: #2e7d32; color: #fff; border: none; border-radius: 8px; height: 42px; line-height: 42px; font-size: 15px; }
.btn-edit { flex: 1; background: #fff; color: #2e7d32; border: 1.5px solid #2e7d32; border-radius: 8px; height: 42px; line-height: 42px; font-size: 15px; font-weight: 600; }
.btn-cancel { flex: 1; background: #fff; color: #666; border: 1px solid #dcdfe6; border-radius: 8px; height: 42px; line-height: 42px; font-size: 15px; }
</style>
