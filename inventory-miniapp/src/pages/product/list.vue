<script setup>
import { ref, onMounted } from 'vue'
import { onPullDownRefresh } from '@dcloudio/uni-app'
import request from '@/api/request'
import FloatingHome from '@/components/FloatingHome'

const list = ref([])
const loading = ref(false)
const keyword = ref('')
const codeKeyword = ref('')
const page = ref(1)
const hasMore = ref(true)
const stockSort = ref('')

async function fetchData(append = false) {
  if (loading.value) return
  loading.value = true
  try {
    if (!append) { page.value = 1; hasMore.value = true }
    const params = { page: page.value, size: 20 }
    if (keyword.value) params.name = keyword.value
    if (codeKeyword.value) params.code = codeKeyword.value
    const res = await request.get('/product/page', { params })
    let records = res.data.records || []
    if (stockSort.value === 'asc') records.sort((a, b) => (a.inventoryQuantity || 0) - (b.inventoryQuantity || 0))
    else if (stockSort.value === 'desc') records.sort((a, b) => (b.inventoryQuantity || 0) - (a.inventoryQuantity || 0))
    if (append) { list.value = [...list.value, ...records] }
    else { list.value = records }
    hasMore.value = records.length >= 20
  } finally { loading.value = false }
}

function onSearch() { fetchData() }
function toggleStockSort() {
  stockSort.value = stockSort.value === 'desc' ? 'asc' : 'desc'
  fetchData()
}
function onScrollToLower() {
  if (!hasMore.value || loading.value) return
  page.value++
  fetchData(true)
}

onMounted(() => fetchData())
onPullDownRefresh(() => { fetchData(); uni.stopPullDownRefresh() })
</script>

<template>
  <view class="page" style="padding:12px;height:100vh;display:flex;flex-direction:column;">
    <view style="display:flex;gap:8px;margin-bottom:10px;">
      <input v-model="keyword" class="search-input" placeholder="搜索名称" @confirm="onSearch" style="flex:1;" />
      <view class="pill-btn" @click="onSearch">搜索</view>
      <view class="reset-btn" @click="keyword = ''; codeKeyword = ''; fetchData()">重置</view>
    </view>
    <view style="display:flex;gap:8px;margin-bottom:12px;">
      <input v-model="codeKeyword" class="search-input" placeholder="搜索编码" @confirm="onSearch" style="flex:2;" />
      <view class="pill-btn" :class="{ active: stockSort }" @click="toggleStockSort">
        库存{{ stockSort === 'asc' ? '↑' : stockSort === 'desc' ? '↓' : '' }}
      </view>
    </view>

    <scroll-view scroll-y class="scroll-list" @scrolltolower="onScrollToLower">
      <view v-if="loading && list.length === 0" class="loading">加载中...</view>
      <view v-else>
        <view v-for="item in list" :key="item.id" class="card" @click="uni.navigateTo({ url: '/pages/product/detail?id=' + item.id })">
          <view class="card-header">
            <view style="display:flex;align-items:center;gap:6px;">
              <text style="font-size:16px;">📦</text>
              <text class="order-no">{{ item.name }}</text>
            </view>
            <text class="status" :class="item.status === 1 ? 'status-1' : 'status-0'">{{ item.status === 1 ? '启用' : '停用' }}</text>
          </view>
          <view class="card-body">
            <text>编码: {{ item.code }} | 规格: {{ item.spec || '-' }}</text>
            <text>分类: {{ item.categoryName || '-' }} | 单位: {{ item.unit }}</text>
            <view style="display:flex;justify-content:space-between;padding-top:4px;border-top:1px solid #f0f4f0;margin-top:4px;">
              <text style="font-size:12px;">采购 ¥{{ item.purchasePrice ?? '-' }}</text>
              <text style="font-size:12px;">销售 ¥{{ item.salePrice ?? '-' }}</text>
              <text style="font-weight:600;color:#2e7d32;">库存 {{ item.inventoryQuantity ?? 0 }}</text>
            </view>
          </view>
        </view>
        <view v-if="loading && list.length > 0" class="loading">加载更多...</view>
        <view v-if="!hasMore && list.length > 0" class="empty" style="padding:16px 0;font-size:12px;">已加载全部</view>
        <view v-if="list.length === 0 && !loading" class="empty">暂无商品</view>
      </view>
    </scroll-view>
    <FloatingHome />
  </view>
</template>

<style scoped>
.scroll-list { flex: 1; overflow-y: auto; }
.pill-btn {
  background: #fff; border-radius: 10px; padding: 0 16px; font-size: 13px;
  display: flex; align-items: center; white-space: nowrap;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04); color: #666;
}
.pill-btn.active { color: #2e7d32; font-weight: 600; }
</style>
