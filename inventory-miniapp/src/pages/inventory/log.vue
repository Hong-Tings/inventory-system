<script setup>
import { ref } from 'vue'
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app'
import request from '@/api/request'
import FloatingHome from '@/components/FloatingHome'

const list = ref([])
const loading = ref(false)
const keyword = ref('')
const orderKeyword = ref('')

async function fetchData() {
  loading.value = true
  try {
    const params = { page: 1, size: 50 }
    if (keyword.value) params.productName = keyword.value
    if (orderKeyword.value) params.refOrderNo = orderKeyword.value
    const res = await request.get('/inventory/log/page', { params })
    list.value = res.data.records
  } finally { loading.value = false }
}

function onSearch() { fetchData() }
function onInput(e) { if (!e.detail.value) fetchData() }

const typeMap = {
  PURCHASE_IN: '采购入库', SALES_OUT: '销售出库',
  TRANSFER_IN: '调拨入库', TRANSFER_OUT: '调拨出库',
  PURCHASE_CANCEL: '取消入库', SALES_CANCEL: '取消出库',
  STOCKTAKE: '盘点调整',
}
const typeColors = {
  PURCHASE_IN: '#2e7d32', SALES_OUT: '#e65100',
  TRANSFER_IN: '#1565c0', TRANSFER_OUT: '#7b1fa2',
  PURCHASE_CANCEL: '#888', SALES_CANCEL: '#888',
  STOCKTAKE: '#f57f17',
}
const pageMap = {
  PURCHASE_IN: '/pages/purchase/detail', PURCHASE_CANCEL: '/pages/purchase/detail',
  SALES_OUT: '/pages/sales/detail', SALES_CANCEL: '/pages/sales/detail',
  TRANSFER_IN: '/pages/transfer/detail', TRANSFER_OUT: '/pages/transfer/detail',
  STOCKTAKE: '/pages/stocktake/detail',
}

function goToOrder(item) {
  const url = pageMap[item.changeType]
  if (!url || !item.refOrderNo) return
  uni.navigateTo({ url: url + '?orderNo=' + encodeURIComponent(item.refOrderNo) })
}

onShow(fetchData)
onPullDownRefresh(() => { fetchData(); uni.stopPullDownRefresh() })
</script>

<template>
  <view class="page">
    <view class="page-bar">
      <text class="page-title">库存流水</text>
    </view>
    <view class="search-row">
      <input v-model="keyword" class="search-input" placeholder="商品名称/编码" @confirm="onSearch" style="flex:1;" />
      <input v-model="orderKeyword" class="search-input" placeholder="关联单号" @confirm="onSearch" style="flex:0.8;" />
      <view class="search-btn" @click="onSearch">搜索</view>
      <view class="reset-btn" @click="keyword = ''; orderKeyword = ''; fetchData()">重置</view>
    </view>

    <view v-if="loading" class="loading">加载中...</view>
    <view v-else>
      <view v-for="item in list" :key="item.id" class="card" @click="goToOrder(item)" style="border-left-color:#43a047;">
        <view class="card-header" style="margin-bottom:8px;">
          <view style="display:flex;align-items:center;gap:6px;">
            <view class="type-dot" :style="{ background: typeColors[item.changeType] || '#999' }"></view>
            <text style="font-weight:600;font-size:14px;">{{ typeMap[item.changeType] || item.changeType }}</text>
          </view>
          <text :style="{ fontSize:'16px', fontWeight:'700', color: (item.changeQty || 0) > 0 ? '#2e7d32' : '#c62828' }">
            {{ item.changeQty > 0 ? '+' : '' }}{{ item.changeQty }}
          </text>
        </view>
        <view class="card-body">
          <text style="display:flex;align-items:center;gap:4px;">
            <text style="color:#bbb;">📦</text> {{ item.productName || '-' }}
          </text>
          <text v-if="item.warehouseName" style="display:flex;align-items:center;gap:4px;">
            <text style="color:#bbb;">🏭</text> {{ item.warehouseName }}
          </text>
          <text v-if="item.refOrderNo" style="font-size:12px;color:#888;">单号: {{ item.refOrderNo }}</text>
          <text v-if="item.operatorName" style="font-size:12px;color:#888;">操作人: {{ item.operatorName }}</text>
        </view>
        <view class="card-footer">
          <text>{{ item.createTime ? item.createTime.substring(0, 16) : '' }}</text>
        </view>
      </view>
      <view v-if="list.length === 0" class="empty">暂无流水</view>
    </view>
    <FloatingHome />
  </view>
</template>

<style scoped>
.search-row { display: flex; gap: 8px; margin-bottom: 14px; }
.search-btn { background: linear-gradient(135deg,#2e7d32,#43a047); color:#fff; border-radius:10px; padding:0 18px; font-size:13px; display:flex; align-items:center; white-space:nowrap; box-shadow:0 2px 8px rgba(46,125,50,0.2); }
.type-dot { width:8px; height:8px; border-radius:50%; display:inline-block; }
.reset-btn { background: #f5f5f5; color: #666; border-radius: 10px; padding: 0 16px; font-size: 13px; display: flex; align-items: center; white-space: nowrap; }
</style>
