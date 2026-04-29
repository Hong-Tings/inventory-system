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
    if (keyword.value) params.name = keyword.value
    const res = await request.get('/product/page', params)
    list.value = res.data.records
  } finally { loading.value = false }
}

function onSearch() { fetchData() }

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
          <text class="product-name">{{ item.name }}</text>
          <text class="product-status" :class="item.status === 1 ? 'on' : 'off'">{{ item.status === 1 ? '启用' : '停用' }}</text>
        </view>
        <view class="card-body">
          <text>编码: {{ item.code }}</text>
          <text v-if="item.spec">规格: {{ item.spec }}</text>
          <text>分类: {{ item.categoryName || '-' }} | 单位: {{ item.unit }}</text>
          <text>采购价: ¥{{ item.purchasePrice ?? '-' }} | 销售价: ¥{{ item.salePrice ?? '-' }}</text>
        </view>
      </view>
      <view v-if="list.length === 0" class="empty">暂无商品</view>
    </view>
  </view>
</template>

<style scoped>
.page { padding: 12px; background: #f5f7f5; min-height: 100vh; }
.search-bar { display: flex; gap: 8px; margin-bottom: 12px; }
.search-input { flex: 1; border: 1px solid #dcdfe6; border-radius: 6px; padding: 8px 12px; font-size: 14px; background: #fff; }
.search-btn { background: #2e7d32; color: #fff; border: none; border-radius: 6px; padding: 8px 16px; font-size: 14px; }
.card { background: #fff; border-radius: 8px; padding: 12px 16px; margin-bottom: 10px; box-shadow: 0 1px 3px rgba(0,0,0,0.04); }
.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.product-name { font-weight: 600; font-size: 14px; }
.product-status { font-size: 11px; padding: 2px 8px; border-radius: 3px; }
.product-status.on { background: #e8f5e9; color: #2e7d32; }
.product-status.off { background: #f5f5f5; color: #999; }
.card-body { display: flex; flex-direction: column; gap: 3px; font-size: 13px; color: #666; }
.loading, .empty { text-align: center; color: #999; padding: 40px 0; }
</style>
