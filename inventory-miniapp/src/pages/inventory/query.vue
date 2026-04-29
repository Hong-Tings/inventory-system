<script setup>
import { ref, onMounted } from 'vue'
import request from '@/api/request'

const list = ref([])
const loading = ref(false)
const warehouses = ref([])
const warehouseId = ref(null)
const keyword = ref('')

async function fetchData() {
  loading.value = true
  try {
    const params = { page: 1, size: 50 }
    if (warehouseId.value) params.warehouseId = warehouseId.value
    if (keyword.value) params.productName = keyword.value
    const res = await request.get('/inventory/page', { params })
    list.value = res.data.records
  } finally { loading.value = false }
}

async function fetchWarehouses() {
  const res = await request.get('/warehouse/list')
  warehouses.value = res.data
}

function onSearch() { fetchData() }

onMounted(() => { fetchWarehouses(); fetchData() })
</script>

<template>
  <view class="page">
    <view class="page-header">
      <text class="page-title">库存查询</text>
      <text class="nav-link" @click="uni.navigateTo({ url: '/pages/inventory/log' })">流水 ›</text>
    </view>
    <view class="search-bar">
      <input v-model="keyword" class="search-input" placeholder="搜索商品名称" @confirm="onSearch" />
      <picker @change="e => { warehouseId = warehouses[e.detail.value]?.id; fetchData() }" :range="warehouses" range-key="name">
        <view class="filter-btn">{{ warehouses.find(w => w.id === warehouseId)?.name || '全部仓库' }}</view>
      </picker>
    </view>

    <view v-if="loading" class="loading">加载中...</view>
    <view v-else>
      <view v-for="item in list" :key="item.id" class="card" @click="uni.navigateTo({ url: '/pages/product/list' })">
        <view class="ir">
          <view class="ii">
            <text class="in">{{ item.productName }}</text>
            <text class="im">{{ item.productCode }} · {{ item.warehouseName }}</text>
          </view>
          <text class="iq" :class="{ lw: item.quantity <= 5 }">{{ item.quantity }}</text>
        </view>
      </view>
      <view v-if="list.length === 0" class="empty">暂无库存数据</view>
    </view>
  </view>
</template>

<style scoped>
.page { padding: 12px; background: #f5f7f5; min-height: 100vh; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.page-title { font-size: 16px; font-weight: 600; }
.nav-link { font-size: 13px; color: #2e7d32; }
.search-bar { display: flex; gap: 8px; margin-bottom: 12px; }
.search-input { flex: 1; border: 1px solid #dcdfe6; border-radius: 6px; padding: 8px 12px; font-size: 14px; background: #fff; }
.filter-btn { border: 1px solid #dcdfe6; border-radius: 6px; padding: 8px 12px; font-size: 14px; background: #fff; white-space: nowrap; }
.card { background: #fff; border-radius: 8px; padding: 12px 16px; margin-bottom: 10px; box-shadow: 0 1px 4px rgba(0,0,0,0.08); }
.ir { display: flex; justify-content: space-between; align-items: center; }
.ii .in { font-weight: 600; font-size: 14px; }
.ii .im { font-size: 12px; color: #999; margin-top: 2px; }
.iq { font-size: 20px; font-weight: 700; color: #2e7d32; }
.iq.lw { color: #c62828; }
.loading, .empty { text-align: center; color: #999; padding: 40px 0; }
</style>
