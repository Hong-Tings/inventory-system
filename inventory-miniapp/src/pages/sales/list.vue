<script setup>
import { ref } from 'vue'
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app'
import request from '@/api/request'

const list = ref([])
const loading = ref(false)
const keyword = ref('')
const statusMap = { 0: '草稿', 1: '已出库', 2: '已取消', 3: '已作废', 4: '待审批' }

async function fetchList() {
  loading.value = true
  try {
    const params = { page: 1, size: 20 }
    if (keyword.value) params.orderNo = keyword.value
    const res = await request.get('/sales-order/page', { params })
    list.value = res.data.records
  } finally { loading.value = false }
}

function goCreate() { uni.navigateTo({ url: '/pages/sales/create' }) }
function goDetail(id) { uni.navigateTo({ url: `/pages/sales/detail?id=${id}` }) }
function onSearch() { fetchList() }

onShow(fetchList)
onPullDownRefresh(() => { fetchList(); uni.stopPullDownRefresh() })
</script>

<template>
  <view class="page">
    <view class="page-bar">
      <text class="page-title">销售出库</text>
      <text class="add-btn" @click="goCreate">+ 新建</text>
    </view>
    <view class="search-bar" style="display:flex;gap:6px;">
      <input v-model="keyword" class="search-input" placeholder="搜索出库单号" style="flex:1;" @confirm="onSearch" />
      <view class="search-btn" @click="onSearch">搜索</view>
      <view class="reset-btn" @click="keyword = ''; fetchList()">重置</view>
    </view>

    <!-- 骨架屏 -->
    <view v-if="loading">
      <view class="skeleton" v-for="n in 4" :key="n">
        <view class="skeleton-line w60"></view>
        <view class="skeleton-line w80"></view>
        <view class="skeleton-line w40" style="margin-bottom:0;"></view>
      </view>
    </view>
    <view v-else>
      <view v-for="item in list" :key="item.id" class="card"
        :style="{ borderLeftColor: item.status === 1 ? '#2e7d32' : item.status === 2 ? '#c62828' : status === 4 ? '#ff9800' : '#e0e0e0' }">
        <view class="card-header">
          <view style="display:flex;align-items:center;gap:8px;">
            <text style="font-size:16px;">📤</text>
            <text class="order-no">{{ item.orderNo }}</text>
          </view>
          <text class="status" :class="'status-' + item.status">{{ statusMap[item.status] || '未知' }}</text>
        </view>
        <view class="card-body">
          <view style="display:flex;justify-content:space-between;">
            <text>{{ item.customerName || '-' }}</text>
            <text>{{ item.warehouseName || '-' }}</text>
          </view>
          <view style="display:flex;justify-content:space-between;color:#888;">
            <text>数量: {{ item.totalQuantity }}</text>
            <text style="font-weight:600;color:#e65100;">¥{{ item.totalAmount?.toFixed(2) }}</text>
          </view>
        </view>
        <view class="card-footer">
          <text>{{ item.orderDate }}</text>
          <text v-if="item.salesman" style="display:flex;align-items:center;gap:4px;">
            <text style="color:#ccc;">👤</text> {{ item.salesman }}
          </text>
        </view>
      </view>
      <view v-if="list.length > 0" class="summary-bar">
        <text>合计 {{ list.reduce((s,i) => s + (i.totalQuantity||0), 0) }} 件</text>
        <text>¥{{ list.reduce((s,i) => s + (i.totalAmount||0), 0).toFixed(2) }}</text>
      </view>
      <view v-if="list.length === 0" class="empty">暂无出库单</view>
    </view>
  </view>
</template>

<style scoped>
</style>
