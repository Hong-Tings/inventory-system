<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import request from '@/api/request'

const order = ref(null)
const loading = ref(false)
const statusMap = { 0: '盘点中', 1: '已审核', 2: '已调整' }

let id = null
onLoad((options) => { id = options?.id; fetchDetail() })

async function fetchDetail() {
  if (!id) return
  loading.value = true
  try {
    const res = await request.get(`/stock-take/${id}`)
    order.value = res.data
  } finally { loading.value = false }
}
</script>

<template>
  <view class="page">
    <view v-if="loading" style="text-align:center;padding:40px;color:#999;">加载中...</view>
    <view v-else-if="order">
      <view class="detail-header">
        <text class="dh-no">{{ order.orderNo }}</text>
        <text class="dh-st" :class="'dh-' + order.status">{{ statusMap[order.status] || '未知' }}</text>
      </view>
      <view class="info-grid">
        <view class="ig-row"><text class="ig-l">仓库</text><text class="ig-v">{{ order.warehouseName || '-' }}</text></view>
        <view class="ig-row"><text class="ig-l">盘点方式</text><text class="ig-v">{{ order.takeType === 0 ? '全盘' : '抽盘' }}</text></view>
        <view class="ig-row"><text class="ig-l">盘点日期</text><text class="ig-v">{{ order.orderDate || '-' }}</text></view>
        <view class="ig-row"><text class="ig-l">商品数</text><text class="ig-v">{{ order.totalItems }}</text></view>
        <view class="ig-row"><text class="ig-l">差异数</text><text class="ig-v" style="color:#e65100;">{{ order.diffItems }}</text></view>
      </view>
    </view>
    <view v-else style="text-align:center;padding:40px;color:#999;">未找到盘点单</view>
  </view>
</template>

<style scoped>
.page { padding: 12px; background: #f5f7f5; min-height: 100vh; }
.detail-header { background: #fff; border-radius: 8px; padding: 20px 16px; text-align: center; margin-bottom: 10px; }
.dh-no { font-size: 18px; font-weight: 700; display: block; }
.dh-st { display: inline-block; margin-top: 6px; font-size: 12px; padding: 3px 14px; border-radius: 12px; }
.dh-1 { background: #e8f5e9; color: #2e7d32; }
.dh-0 { background: #fff8e1; color: #f57f17; }
.dh-2 { background: #e3f2fd; color: #1565c0; }
.info-grid { background: #fff; border-radius: 8px; padding: 16px; margin-bottom: 10px; }
.ig-row { display: flex; justify-content: space-between; padding: 10px 0; border-bottom: 1px solid #f0f0f0; font-size: 14px; }
.ig-row:last-child { border-bottom: none; }
.ig-l { color: #999; } .ig-v { font-weight: 500; color: #333; }
</style>
