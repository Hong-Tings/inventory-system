<script setup>
import { ref, onMounted } from 'vue'
import request from '@/api/request'

const list = ref([])
const loading = ref(false)
const statusMap = { 0: '盘点中', 1: '已审核', 2: '已调整' }

async function fetchList() {
  loading.value = true
  try {
    const res = await request.get('/stock-take/page', { params: { page: 1, size: 20 } })
    list.value = res.data.records
  } finally { loading.value = false }
}

function goDetail(id) {
  uni.navigateTo({ url: `/pages/stocktake/detail?id=${id}` })
}

onMounted(fetchList)
</script>

<template>
  <view class="page">
    <view class="page-header">
      <text class="page-title">库存盘点</text>
      <button class="add-btn" @click="uni.navigateTo({ url: '/pages/stocktake/create' })">+ 新建</button>
    </view>

    <view v-if="loading" class="loading">加载中...</view>
    <view v-else>
      <view v-for="item in list" :key="item.id" class="card" @click="goDetail(item.id)">
        <view class="card-header">
          <text class="order-no">{{ item.orderNo }}</text>
          <text class="status" :class="'status-' + item.status">{{ statusMap[item.status] || '未知' }}</text>
        </view>
        <view class="card-body">
          <text>仓库: {{ item.warehouseName || '-' }}</text>
          <text>盘点方式: {{ item.takeType === 0 ? '全盘' : '抽盘' }}</text>
          <text>商品数: {{ item.totalItems }} | 差异: {{ item.diffItems }}</text>
        </view>
        <view class="card-footer">
          <text>{{ item.orderDate }}</text>
        </view>
      </view>
      <view v-if="list.length === 0" class="empty">暂无盘点单</view>
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
.status-0 { background: #fff8e1; color: #f57f17; }
.status-1 { background: #e8f5e9; color: #2e7d32; }
.status-2 { background: #e3f2fd; color: #1565c0; }
.card-body { display: flex; flex-direction: column; gap: 4px; font-size: 13px; color: #666; }
.card-footer { margin-top: 8px; font-size: 12px; color: #999; }
.loading, .empty { text-align: center; color: #999; padding: 40px 0; }
</style>
