<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import request from '@/api/request'

const order = ref(null)
const loading = ref(false)
const statusMap = { 0: '草稿', 1: '已完成', 2: '已取消' }

let id = null
onLoad((options) => { id = options?.id; fetchDetail() })

async function fetchDetail() {
  if (!id) return
  loading.value = true
  try {
    const res = await request.get(`/transfer/${id}`)
    order.value = res.data
  } finally { loading.value = false }
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
      </view>
    </view>
    <view v-else style="text-align:center;padding:40px;color:#999;">未找到调拨单</view>
  </view>
</template>

<style scoped>
.page { padding: 12px; background: #f5f7f5; min-height: 100vh; }
.dh { background: #fff; border-radius: 8px; padding: 20px 16px; text-align: center; margin-bottom: 10px; }
.dh-no { font-size: 18px; font-weight: 700; display: block; }
.dh-st { display: inline-block; margin-top: 6px; font-size: 12px; padding: 3px 14px; border-radius: 12px; }
.dh-1 { background: #e8f5e9; color: #2e7d32; }
.dh-0 { background: #f0f0f0; color: #999; }
.dh-2 { background: #fbe9e7; color: #c62828; }
.ig { background: #fff; border-radius: 8px; padding: 16px; margin-bottom: 10px; }
.ig .r { display: flex; justify-content: space-between; padding: 10px 0; border-bottom: 1px solid #f0f0f0; font-size: 14px; }
.ig .r:last-child { border-bottom: none; }
.ig .l { color: #999; } .ig .v { font-weight: 500; }
</style>
