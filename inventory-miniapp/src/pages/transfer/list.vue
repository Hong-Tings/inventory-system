<script setup>
import { ref, onMounted } from 'vue'
import request from '@/api/request'

const list = ref([])
const loading = ref(false)
const statusMap = { 0: '草稿', 1: '已完成', 2: '已取消' }

async function fetchList() {
  loading.value = true
  try {
    const res = await request.get('/transfer/page', { params: { page: 1, size: 20 } })
    list.value = res.data.records
  } finally { loading.value = false }
}

function goCreate() { uni.navigateTo({ url: '/pages/transfer/create' }) }
function goDetail(id) { uni.navigateTo({ url: `/pages/transfer/detail?id=${id}` }) }

onMounted(fetchList)
</script>

<template>
  <view class="page">
    <view class="page-header">
      <text class="page-title">库存调拨</text>
      <button class="add-btn" @click="goCreate">+ 新建</button>
    </view>

    <view v-if="loading" class="loading">加载中...</view>
    <view v-else>
      <view v-for="item in list" :key="item.id" class="card" @click="goDetail(item.id)">
        <view class="card-header">
          <text class="order-no">{{ item.orderNo }}</text>
          <text class="status" :class="'status-' + item.status">{{ statusMap[item.status] || '未知' }}</text>
        </view>
        <view class="card-body">
          <text>调出: {{ item.fromWarehouseName || '-' }} → 调入: {{ item.toWarehouseName || '-' }}</text>
          <text>数量: {{ item.totalQuantity }}</text>
        </view>
        <view class="card-footer">
          <text>{{ item.orderDate }}</text>
        </view>
      </view>
      <view v-if="list.length === 0" class="empty">暂无调拨单</view>
    </view>
  </view>
</template>

<style scoped>
.page { padding: 16px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.page-title { font-size: 18px; font-weight: bold; }
.add-btn { background: #2e7d32; color: #fff; border: none; border-radius: 6px; padding: 8px 16px; font-size: 14px; }
.card { background: #fff; border-radius: 8px; padding: 12px 16px; margin-bottom: 12px; box-shadow: 0 1px 4px rgba(0,0,0,0.08); }
.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.order-no { font-weight: bold; font-size: 15px; }
.status { font-size: 12px; padding: 2px 8px; border-radius: 4px; }
.status-0 { background: #f0f0f0; color: #666; }
.status-1 { background: #e6f7e6; color: #52c41a; }
.status-2 { background: #fff0f0; color: #ff4d4f; }
.card-body { display: flex; flex-direction: column; gap: 4px; font-size: 13px; color: #666; }
.card-footer { margin-top: 8px; font-size: 12px; color: #999; }
.loading, .empty { text-align: center; color: #999; padding: 40px 0; }
</style>
