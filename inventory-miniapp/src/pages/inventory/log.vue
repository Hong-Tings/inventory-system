<script setup>
import { ref, onMounted } from 'vue'
import request from '@/api/request'

const list = ref([])
const loading = ref(false)
const keyword = ref('')

async function fetchData() {
  loading.value = true
  try {
    const params = { page: 1, size: 50 }
    if (keyword.value) params.productName = keyword.value
    const res = await request.get('/inventory/log/page', { params })
    list.value = res.data.records
  } finally { loading.value = false }
}

function onSearch() { fetchData() }

const typeMap = {
  PURCHASE_IN: '采购入库', SALES_OUT: '销售出库',
  TRANSFER_IN: '调拨入库', TRANSFER_OUT: '调拨出库',
  PURCHASE_CANCEL: '取消入库', SALES_CANCEL: '取消出库',
  STOCKTAKE: '盘点调整',
}

onMounted(fetchData)
</script>

<template>
  <view class="page">
    <view class="search-bar">
      <input v-model="keyword" class="search-input" placeholder="搜索商品名称" @confirm="onSearch" />
      <view class="search-btn" @click="onSearch">搜索</view>
    </view>

    <view v-if="loading" class="loading">加载中...</view>
    <view v-else>
      <view v-for="item in list" :key="item.id" class="card">
        <view class="card-header">
          <text class="log-type">{{ typeMap[item.changeType] || item.changeType }}</text>
          <text :class="item.changeQty > 0 ? 'qty-in' : 'qty-out'">{{ item.changeQty > 0 ? '+' : '' }}{{ item.changeQty }}</text>
        </view>
        <view class="card-body">
          <text v-if="item.productName">商品: {{ item.productName }}</text>
          <text v-if="item.warehouseName">仓库: {{ item.warehouseName }}</text>
          <text v-if="item.refOrderNo">单号: {{ item.refOrderNo }}</text>
          <text v-if="item.operatorName">操作人: {{ item.operatorName }}</text>
          <text class="log-time">{{ item.createTime ? item.createTime.substring(0, 16) : '' }}</text>
        </view>
      </view>
      <view v-if="list.length === 0" class="empty">暂无流水</view>
    </view>
  </view>
</template>

<style scoped>
.page { padding: 12px; background: #f5f7f5; min-height: 100vh; }
.search-bar { display: flex; gap: 8px; margin-bottom: 12px; }
.search-input { flex: 1; border: 1px solid #dcdfe6; border-radius: 6px; padding: 8px 12px; font-size: 14px; background: #fff; }
.search-btn { background: #2e7d32; color: #fff; border: none; border-radius: 6px; padding: 8px 16px; font-size: 14px; }
.card { background: #fff; border-radius: 8px; padding: 12px 16px; margin-bottom: 8px; box-shadow: 0 1px 3px rgba(0,0,0,0.04); }
.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.log-type { font-weight: 600; font-size: 14px; color: #333; }
.qty-in { font-size: 16px; font-weight: 700; color: #2e7d32; }
.qty-out { font-size: 16px; font-weight: 700; color: #c62828; }
.card-body { display: flex; flex-direction: column; gap: 2px; font-size: 13px; color: #666; }
.log-time { font-size: 12px; color: #999; margin-top: 4px; }
.loading, .empty { text-align: center; color: #999; padding: 40px 0; }
</style>
