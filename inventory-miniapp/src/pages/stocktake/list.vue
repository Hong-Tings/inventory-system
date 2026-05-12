<script setup>
import { ref } from 'vue'
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app'
import request from '@/api/request'
import FloatingHome from '@/components/FloatingHome'

const list = ref([])
const loading = ref(false)
const keyword = ref('')
const takeType = ref(null)
const filterStatus = ref(null)
const statusMap = { 0: '盘点中', 1: '已审核', 2: '已调整' }
const typeOptions = [{ v: null, l: '全部方式' }, { v: 0, l: '全盘' }, { v: 1, l: '抽盘' }]
const statusOptions = [{ v: null, l: '全部状态' }, { v: 0, l: '盘点中' }, { v: 1, l: '已审核' }, { v: 2, l: '已调整' }]

async function fetchList() {
  loading.value = true
  try {
    const params = { page: 1, size: 20 }
    if (keyword.value) params.orderNo = keyword.value
    if (takeType.value != null) params.takeType = takeType.value
    if (filterStatus.value != null) params.status = filterStatus.value
    const res = await request.get('/stock-take/page', { params })
    list.value = res.data.records
  } finally { loading.value = false }
}

function goDetail(id) { uni.navigateTo({ url: `/pages/stocktake/detail?id=${id}` }) }
function onSearch() { fetchList() }

onShow(fetchList)
onPullDownRefresh(() => { fetchList(); uni.stopPullDownRefresh() })
</script>

<template>
  <view class="page">
    <view class="page-bar">
      <text class="page-title">库存盘点</text>
      <text class="add-btn" @click="uni.navigateTo({ url: '/pages/stocktake/create' })">+ 新建</text>
    </view>

    <view class="search-bar" style="display:flex;gap:6px;flex-wrap:wrap;">
      <view style="display:flex;gap:6px;flex:1;min-width:200px;">
        <input v-model="keyword" class="search-input" placeholder="搜索盘点单号" style="flex:1;" @confirm="onSearch" />
        <view class="search-btn" @click="onSearch">搜索</view>
        <view class="reset-btn" @click="keyword = ''; takeType = null; filterStatus = null; fetchList()">重置</view>
      </view>
      <picker @change="e => { takeType = typeOptions[e.detail.value]?.v; fetchList() }" :range="typeOptions" range-key="l">
        <view class="filter-pill">{{ typeOptions.find(t => t.v === takeType)?.l }}</view>
      </picker>
      <picker @change="e => { filterStatus = statusOptions[e.detail.value]?.v; fetchList() }" :range="statusOptions" range-key="l">
        <view class="filter-pill">{{ statusOptions.find(s => s.v === filterStatus)?.l }}</view>
      </picker>
    </view>

    <view v-if="loading">
      <view class="skeleton" v-for="n in 3" :key="n">
        <view class="skeleton-line w60"></view>
        <view class="skeleton-line w80"></view>
        <view class="skeleton-line w40" style="margin-bottom:0;"></view>
      </view>
    </view>
    <view v-else>
      <view v-for="item in list" :key="item.id" class="card"
        :style="{ borderLeftColor: item.status === 1 ? '#2e7d32' : item.status === 2 ? '#1565c0' : '#ff9800' }">
        <view class="card-header">
          <view style="display:flex;align-items:center;gap:8px;">
            <text style="font-size:16px;">📋</text>
            <text class="order-no">{{ item.orderNo }}</text>
          </view>
          <text class="status" :class="'status-' + item.status">{{ statusMap[item.status] || '未知' }}</text>
        </view>
        <view class="card-body">
          <view style="display:flex;justify-content:space-between;">
            <text>{{ item.warehouseName || '-' }}</text>
            <text>{{ item.takeType === 0 ? '全盘' : '抽盘' }}</text>
          </view>
          <view style="display:flex;justify-content:space-between;color:#888;">
            <text>商品 {{ item.totalItems }} 项</text>
            <text :style="{ color: item.diffItems > 0 ? '#c62828' : '#888', fontWeight: item.diffItems > 0 ? 600 : 400 }">
              差异 {{ item.diffItems }}
            </text>
          </view>
        </view>
        <view class="card-footer">
          <text>{{ item.orderDate }}</text>
          <text>{{ item.operatorName }}</text>
        </view>
      </view>
      <view v-if="list.length === 0" class="empty">暂无盘点单</view>
    </view>
    <FloatingHome />
  </view>
</template>

<style scoped>
.filter-pill { background:#fff; border-radius:10px; padding:12px 14px; font-size:13px; white-space:nowrap; box-shadow:0 1px 4px rgba(0,0,0,0.04); }
</style>
