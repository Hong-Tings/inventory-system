<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import request from '@/api/request'

const order = ref(null)
const loading = ref(false)
const statusMap = { 0: '草稿', 1: '已出库', 2: '已取消' }

let id = null
onLoad((options) => { id = options?.id; fetchDetail() })

async function fetchDetail() {
  if (!id) return
  loading.value = true
  try {
    const res = await request.get(`/sales-order/${id}`)
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
        <view class="r"><text class="l">客户</text><text class="v">{{ order.customerName || '-' }}</text></view>
        <view class="r"><text class="l">仓库</text><text class="v">{{ order.warehouseName || '-' }}</text></view>
        <view class="r"><text class="l">出库日期</text><text class="v">{{ order.orderDate || '-' }}</text></view>
        <view class="r"><text class="l">销售员</text><text class="v">{{ order.salesman || '-' }}</text></view>
      </view>
      <view class="pl">
        <text class="pl-title">商品明细</text>
        <view v-if="order.items && order.items.length">
          <view class="pi" v-for="item in order.items" :key="item.id">
            <view><text class="pi-name">{{ item.productName || '-' }}</text><text class="pi-detail">{{ item.quantity }}件 × ¥{{ item.unitPrice?.toFixed(2) }}</text></view>
            <text class="pi-amt">¥{{ item.amount?.toFixed(2) }}</text>
          </view>
          <view class="pi-total"><text>合计</text><text style="color:#e65100;">¥{{ order.totalAmount?.toFixed(2) }}</text></view>
        </view>
        <view v-else style="text-align:center;color:#999;padding:20px;">暂无明细</view>
      </view>
    </view>
    <view v-else style="text-align:center;padding:40px;color:#999;">未找到出库单</view>
  </view>
</template>

<style scoped>
.page { padding: 12px; background: #f5f7f5; min-height: 100vh; }
.dh { background: #fff; border-radius: 8px; padding: 20px 16px; text-align: center; margin-bottom: 10px; }
.dh-no { font-size: 18px; font-weight: 700; display: block; }
.dh-st { display: inline-block; margin-top: 6px; font-size: 12px; padding: 3px 14px; border-radius: 12px; }
.dh-1 { background: #e8f5e9; color: #2e7d32; } .dh-0 { background: #f0f0f0; color: #999; } .dh-2 { background: #fbe9e7; color: #c62828; }
.ig { background: #fff; border-radius: 8px; padding: 16px; margin-bottom: 10px; }
.ig .r { display: flex; justify-content: space-between; padding: 10px 0; border-bottom: 1px solid #f0f0f0; font-size: 14px; }
.ig .r:last-child { border-bottom: none; }
.ig .l { color: #999; } .ig .v { font-weight: 500; }
.pl { background: #fff; border-radius: 8px; padding: 16px; margin-bottom: 10px; }
.pl-title { font-size: 14px; font-weight: 600; margin-bottom: 10px; display: block; }
.pi { display: flex; justify-content: space-between; padding: 10px 0; border-bottom: 1px solid #f5f5f5; font-size: 13px; }
.pi-name { font-weight: 500; display: block; } .pi-detail { color: #666; font-size: 12px; }
.pi-amt { color: #e65100; font-weight: 600; }
.pi-total { display: flex; justify-content: space-between; padding: 12px 0 0; font-weight: 600; font-size: 14px; border-top: 1px solid #eee; margin-top: 8px; }
</style>
