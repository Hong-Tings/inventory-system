<script setup>
import { ref, onMounted } from 'vue'
import request from '@/api/request'

const list = ref([])
const loading = ref(false)
const keyword = ref('')
const statusMap = { 0: '草稿', 1: '已入库', 2: '已取消' }

async function fetchList() {
  loading.value = true
  try {
    const params = { page: 1, size: 20 }
    if (keyword.value) params.orderNo = keyword.value
    const res = await request.get('/purchase-order/page', { params })
    list.value = res.data.records
  } finally { loading.value = false }
}

function goCreate() { uni.navigateTo({ url: '/pages/purchase/create' }) }
function goDetail(id) { uni.navigateTo({ url: `/pages/purchase/detail?id=${id}` }) }
function onSearch() { fetchList() }

onMounted(fetchList)
</script>

<template>
  <view class="page">
    <view class="page-bar">
      <text class="page-title">采购入库</text>
      <button class="add-btn" @click="goCreate">+ 新建</button>
    </view>
    <view class="search-bar">
      <input v-model="keyword" class="search-input" placeholder="搜索入库单号" @confirm="onSearch" />
    </view>

    <view v-if="loading" class="loading">加载中...</view>
    <view v-else>
      <view v-for="item in list" :key="item.id" class="card" @click="goDetail(item.id)">
        <view class="card-header">
          <text class="order-no">{{ item.orderNo }}</text>
          <text class="status" :class="'status-' + item.status">{{ statusMap[item.status] || '未知' }}</text>
        </view>
        <view class="card-body">
          <text>供应商: {{ item.supplierName || '-' }}</text>
          <text>仓库: {{ item.warehouseName || '-' }}</text>
          <text>数量: {{ item.totalQuantity }} | ¥{{ item.totalAmount?.toFixed(2) }}</text>
        </view>
        <view class="card-footer">
          <text>{{ item.orderDate }}</text>
          <text v-if="item.operatorName">{{ item.operatorName }}</text>
        </view>
      </view>
      <view v-if="list.length === 0" class="empty">暂无入库单</view>
    </view>
  </view>
</template>

<style scoped>
.page { padding: 16px; }
.page-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.page-title { font-size: 18px; font-weight: bold; }
.add-btn { background: #2e7d32; color: #fff; border: none; border-radius: 6px; padding: 8px 16px; font-size: 14px; }
.search-bar { margin-bottom: 12px; }
.search-input { width: 100%; border: 1px solid #dcdfe6; border-radius: 6px; padding: 8px 12px; font-size: 14px; background: #fff; box-sizing: border-box; }
.card { background: #fff; border-radius: 8px; padding: 12px 16px; margin-bottom: 12px; box-shadow: 0 1px 4px rgba(0,0,0,0.08); }
.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.order-no { font-weight: bold; font-size: 15px; }
.status { font-size: 12px; padding: 2px 8px; border-radius: 4px; }
.status-0 { background: #f0f0f0; color: #666; }
.status-1 { background: #e6f7e6; color: #52c41a; }
.status-2 { background: #fff0f0; color: #ff4d4f; }
.card-body { display: flex; flex-direction: column; gap: 4px; font-size: 13px; color: #666; }
.card-footer { display: flex; justify-content: space-between; margin-top: 8px; font-size: 12px; color: #999; }
.loading, .empty { text-align: center; color: #999; padding: 40px 0; }
</style>
