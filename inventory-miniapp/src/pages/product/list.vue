<script setup>
import { ref, onMounted } from 'vue'
import { onPullDownRefresh } from '@dcloudio/uni-app'
import request from '@/api/request'
import FloatingHome from '@/components/FloatingHome'

const list = ref([])
const loading = ref(false)
const keyword = ref('')
const page = ref(1)
const hasMore = ref(true)
const sortField = ref('')
const sortDir = ref('')
const filterStatus = ref(null)

const showSort = ref(false)
const activeSort = ref('') // 'stock', 'purchase', 'sale'
const activeSortDir = ref('') // 'asc', 'desc'

async function fetchData(append = false) {
  if (loading.value) return
  loading.value = true
  try {
    if (!append) { page.value = 1; hasMore.value = true }
    const params = { page: page.value, size: 20 }
    if (keyword.value) { params.name = keyword.value; params.code = keyword.value }
    if (filterStatus.value != null) params.status = filterStatus.value
    const res = await request.get('/product/page', { params })
    let records = res.data.records || []
    // 客户端排序
    if (sortField.value === 'stock') records.sort((a, b) => sortDir.value === 'asc' ? (a.inventoryQuantity||0) - (b.inventoryQuantity||0) : (b.inventoryQuantity||0) - (a.inventoryQuantity||0))
    else if (sortField.value === 'purchase') records.sort((a, b) => sortDir.value === 'asc' ? (a.purchasePrice||0) - (b.purchasePrice||0) : (b.purchasePrice||0) - (a.purchasePrice||0))
    else if (sortField.value === 'sale') records.sort((a, b) => sortDir.value === 'asc' ? (a.salePrice||0) - (b.salePrice||0) : (b.salePrice||0) - (a.salePrice||0))
    if (append) { list.value = [...list.value, ...records] }
    else { list.value = records }
    hasMore.value = records.length >= 20
  } finally { loading.value = false }
}

function onSearch() { fetchData() }
function setSort(field, dir) {
  if (activeSort.value === field && activeSortDir.value === dir) {
    activeSort.value = ''; activeSortDir.value = '' // 取消排序
  } else {
    activeSort.value = field; activeSortDir.value = dir
  }
  sortField.value = activeSort.value; sortDir.value = activeSortDir.value
  showSort.value = false; fetchData()
}
function sortLabel() {
  if (!activeSort.value) return '排序'
  const labels = { stock: '库存', purchase: '采购价', sale: '销售价' }
  return labels[activeSort.value] + (activeSortDir.value === 'asc' ? '↑' : '↓')
}
function setStatusFilter(v) { filterStatus.value = v; fetchData() }
function handleReset() {
  keyword.value = ''; filterStatus.value = null
  activeSort.value = ''; activeSortDir.value = ''; sortField.value = ''; sortDir.value = ''
  showSort.value = false; fetchData()
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
    <view style="display:flex;gap:6px;margin-bottom:8px;">
      <input v-model="keyword" class="search-input" placeholder="搜索名称或编码" @confirm="onSearch" style="flex:1;" />
      <view class="search-btn" @click="onSearch">搜索</view>
      <view class="reset-btn" @click="handleReset()">重置</view>
      <view class="sort-btn" :class="{ active: activeSort }" @click="activeSort ? setSort(activeSort, activeSortDir === 'asc' ? 'desc' : 'asc') : showSort = !showSort">{{ sortLabel() }}</view>
    </view>
    <view v-if="showSort" class="sort-panel">
      <view class="sort-row" @click="setSort('stock', 'desc')">库存 <text :class="{ active: activeSort === 'stock' && activeSortDir === 'desc' }">↓</text><text :class="{ active: activeSort === 'stock' && activeSortDir === 'asc' }">↑</text></view>
      <view class="sort-row" @click="setSort('purchase', 'desc')">采购价 <text :class="{ active: activeSort === 'purchase' && activeSortDir === 'desc' }">↓</text><text :class="{ active: activeSort === 'purchase' && activeSortDir === 'asc' }">↑</text></view>
      <view class="sort-row" @click="setSort('sale', 'desc')">销售价 <text :class="{ active: activeSort === 'sale' && activeSortDir === 'desc' }">↓</text><text :class="{ active: activeSort === 'sale' && activeSortDir === 'asc' }">↑</text></view>
    </view>
    <view style="display:flex;gap:6px;margin-bottom:10px;flex-wrap:wrap;">
      <text class="st-pill" :class="{ on: filterStatus === null }" @click="setStatusFilter(null)">全部</text>
      <text class="st-pill" :class="{ on: filterStatus === 1 }" @click="setStatusFilter(1)">启用</text>
      <text class="st-pill" :class="{ on: filterStatus === 0 }" @click="setStatusFilter(0)">停用</text>
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
.search-btn { background: #2e7d32; color: #fff; border-radius: 10px; padding: 0 16px; font-size: 13px; display: flex; align-items: center; white-space: nowrap; }
.reset-btn { background: #f5f5f5; color: #666; border-radius: 10px; padding: 0 16px; font-size: 13px; display: flex; align-items: center; white-space: nowrap; }
.sort-btn { background: #fff; border-radius: 10px; padding: 0 12px; font-size: 12px; display: flex; align-items: center; white-space: nowrap; box-shadow:0 1px 4px rgba(0,0,0,0.04); color:#666; }
.sort-btn.active { color: #2e7d32; font-weight: 600; }
.sort-panel { background:#fff; border-radius:10px; padding:8px; margin-bottom:10px; display:flex; gap:8px; box-shadow:0 1px 4px rgba(0,0,0,0.04); }
.sort-row { flex:1; text-align:center; padding:6px 0; border-radius:6px; font-size:13px; color:#666; background:#f5f5f5; }
.sort-row text { color:#bbb; margin:0 2px; }
.sort-row text.active { color:#2e7d32; font-weight:700; }
.st-pill { background: #f5f5f5; border-radius: 8px; padding: 4px 10px; font-size: 12px; color: #666; }
.st-pill.on { background: #e8f5e9; color: #2e7d32; font-weight: 600; }
</style>
