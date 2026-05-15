<script setup>
import { ref, computed } from 'vue'
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app'
import request from '@/api/request'
import FloatingHome from '@/components/FloatingHome'

const list = ref([])
const loading = ref(false)
const keyword = ref('')
const filterStatus = ref(null)
const startDate = ref('')
const endDate = ref('')
const pendingCount = ref(0)

// 级联仓库选择
const warehouseTree = ref([])
const whCascade = ref([])
const whOptions = ref([])
const showWhPicker = ref(false)
const warehouseId = ref(null)

const statusMap = { 0: '草稿', 1: '已入库', 2: '已取消', 3: '已作废', 4: '待审批' }
const statusOptions = [{ v: null, l: '全部状态' }, { v: 0, l: '草稿' }, { v: 4, l: '待审批' }, { v: 1, l: '已入库' }, { v: 2, l: '已取消' }]
const showFilters = ref(false)

async function fetchList() {
  loading.value = true
  try {
    const params = { page: 1, size: 20 }
    if (keyword.value) params.orderNo = keyword.value
    if (filterStatus.value !== null) params.status = filterStatus.value
    if (warehouseId.value) params.warehouseId = warehouseId.value
    if (startDate.value) params.startDate = startDate.value
    if (endDate.value) params.endDate = endDate.value
    const res = await request.get('/purchase-order/page', { params })
    list.value = res.data.records || []
    if (filterStatus.value != null && list.value.length > 0) {
      const allMatch = list.value.every(item => item.status === filterStatus.value)
      if (!allMatch) console.warn('status filter mismatch', filterStatus.value)
    }
  } finally { loading.value = false }
}

async function fetchPendingCount() {
  try {
    const res = await request.get('/purchase-order/page', { params: { page: 1, size: 1, status: 4 } })
    pendingCount.value = res.data.total || 0
  } catch {}
}

async function fetchWarehouseTree() {
  const res = await request.get('/warehouse/tree')
  warehouseTree.value = res.data || []
}

function goCreate() { uni.navigateTo({ url: '/pages/purchase/create' }) }
function goDetail(id) { uni.navigateTo({ url: `/pages/purchase/detail?id=${id}` }) }
function onSearch() { fetchList() }
function setFilterStatus(v) { filterStatus.value = v; fetchList() }
function handleReset() {
  keyword.value = ''; filterStatus.value = null; warehouseId.value = null
  startDate.value = ''; endDate.value = ''; whCascade.value = []
  fetchList()
}
function handleQuickReset() { handleReset() }

// 级联仓库
function openWarehousePicker() { whCascade.value = []; whOptions.value = warehouseTree.value || []; showWhPicker.value = true }
function selectWhLevel(item) {
  whCascade.value.push(item)
  if (item.children?.length) { whOptions.value = item.children }
  else { warehouseId.value = item.id; showWhPicker.value = false; fetchList() }
}
function goBackTo(index) {
  whCascade.value = whCascade.value.slice(0, index + 1)
  const parent = whCascade.value.length ? whCascade.value[whCascade.value.length - 1] : null
  whOptions.value = parent ? (parent.children || []) : (warehouseTree.value || [])
}
const whLabel = computed(() => {
  if (!warehouseId.value) return '选择仓库'
  function find(nodes, id) { for (const n of nodes) { if (n.id === id) return n.name; if (n.children) { const r = find(n.children, id); if (r) return r } } return null }
  return find(warehouseTree.value, warehouseId.value) || '仓库' + warehouseId.value
})

function filterActive() {
  return filterStatus.value !== null || warehouseId.value || startDate.value || endDate.value
}

onShow(() => {
  fetchList(); fetchPendingCount()
  if (!warehouseTree.value.length) fetchWarehouseTree()
})
onPullDownRefresh(() => { fetchList(); uni.stopPullDownRefresh() })
</script>

<template>
  <view class="page">
    <view class="page-bar">
      <text class="page-title">采购入库</text>
      <view style="display:flex;gap:6px;align-items:center;">
        <text v-if="pendingCount > 0" class="pending-badge" @click="setFilterStatus(4)">{{ pendingCount }}待审批</text>
        <text class="add-btn" @click="goCreate">+ 新建</text>
      </view>
    </view>
    <view class="search-bar" style="display:flex;gap:6px;">
      <input v-model="keyword" class="search-input" placeholder="搜索入库单号" style="flex:1;" @confirm="onSearch" />
      <view class="search-btn" @click="onSearch">搜索</view>
      <view class="reset-btn" @click="handleQuickReset()">重置</view>
      <view class="filter-btn" :class="{ active: filterActive() }" @click="showFilters = !showFilters">筛选</view>
    </view>

    <!-- 筛选栏 -->
    <view v-if="showFilters" class="filter-bar">
      <view class="filter-row">
        <text class="filter-label">状态</text>
        <scroll-view scroll-x class="filter-scroll">
          <view v-for="opt in statusOptions" :key="opt.v ?? 'all'" class="filter-pill"
            :class="{ on: filterStatus === opt.v }" @click="setFilterStatus(opt.v)">{{ opt.l }}</view>
        </scroll-view>
      </view>
      <view class="filter-row">
        <text class="filter-label">仓库</text>
        <view class="filter-pill filter-picker" :class="{ on: warehouseId }" @click="openWarehousePicker">{{ whLabel }}</view>
      </view>
      <view class="filter-row">
        <text class="filter-label">日期</text>
        <picker mode="date" :value="startDate" :end="endDate || undefined" @change="e => { startDate = e.detail.value; fetchList() }">
          <text class="date-text">{{ startDate || '开始日期' }}</text>
        </picker>
        <text style="color:#ccc;margin:0 4px;">~</text>
        <picker mode="date" :value="endDate" :start="startDate || undefined" @change="e => { endDate = e.detail.value; fetchList() }">
          <text class="date-text">{{ endDate || '结束日期' }}</text>
        </picker>
      </view>
    </view>

    <!-- 级联仓库弹窗 -->
    <view v-if="showWhPicker" class="picker-overlay" @click="showWhPicker = false">
      <view class="picker-modal" @click.stop>
        <view class="picker-header">
          <text class="picker-cancel" @click="showWhPicker = false">取消</text>
          <text style="font-weight:bold;">选择仓库</text>
          <view style="width:40px;"></view>
        </view>
        <view v-if="whCascade.length" class="wh-breadcrumb">
          <text v-for="(c, i) in whCascade" :key="i" class="wh-crumb" @click="goBackTo(i)">{{ c.name }}<text v-if="i < whCascade.length - 1"> ›</text></text>
        </view>
        <scroll-view scroll-y class="picker-list">
          <view v-for="item in whOptions" :key="item.id" class="picker-item" @click="selectWhLevel(item)">
            <text :style="{ fontWeight: item.children?.length ? 'bold' : 'normal' }">{{ item.name }}</text>
            <text style="font-size:11px;color:#999;margin-left:4px;">{{ item.level }}级</text>
            <text v-if="item.children?.length" style="margin-left:auto;color:#ccc;">›</text>
          </view>
          <view v-if="!whOptions.length" style="text-align:center;padding:30px 0;color:#999;">无下级仓库</view>
        </scroll-view>
      </view>
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
      <view v-for="item in list" :key="item.id" class="card" @click="goDetail(item.id)"
        :style="{ borderLeftColor: item.status === 1 ? '#2e7d32' : item.status === 2 ? '#c62828' : item.status === 4 ? '#ff9800' : '#e0e0e0' }">
        <view class="card-header">
          <view style="display:flex;align-items:center;gap:8px;">
            <text style="font-size:16px;">📦</text>
            <text class="order-no">{{ item.orderNo }}</text>
          </view>
          <text class="status" :class="'status-' + item.status">{{ statusMap[item.status] || '未知' }}</text>
        </view>
        <view class="card-body">
          <view style="display:flex;justify-content:space-between;">
            <text>{{ item.supplierName || '-' }}</text>
            <text>{{ item.warehouseName || '-' }}</text>
          </view>
          <view style="display:flex;justify-content:space-between;color:#888;">
            <text>数量: {{ item.totalQuantity }}</text>
            <text style="font-weight:600;color:#e65100;">¥{{ item.totalAmount?.toFixed(2) }}</text>
          </view>
        </view>
        <view class="card-footer">
          <text>{{ item.orderDate }}</text>
          <text v-if="item.operatorName" style="display:flex;align-items:center;gap:4px;">
            <text style="color:#ccc;">👤</text> {{ item.operatorName }}
          </text>
        </view>
      </view>
      <view v-if="list.length > 0" class="summary-bar">
        <text>合计 {{ list.reduce((s,i) => s + (i.totalQuantity||0), 0) }} 件</text>
        <text>¥{{ list.reduce((s,i) => s + (i.totalAmount||0), 0).toFixed(2) }}</text>
      </view>
      <view v-if="list.length === 0" class="empty">暂无入库单</view>
    </view>
    <FloatingHome />
  </view>
</template>

<style scoped>
.page { padding: 16px; }
.page-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.page-title { font-size: 18px; font-weight: bold; }
.add-btn { background: #2e7d32; color: #fff; border: none; border-radius: 10px; padding: 8px 16px; font-size: 13px; }
.pending-badge { background: #fff3e0; color: #e65100; border-radius: 10px; padding: 8px 12px; font-size: 12px; font-weight: 600; white-space: nowrap; }
.search-bar { margin-bottom: 8px; }
.search-input { border: 1px solid #dcdfe6; border-radius: 10px; padding: 12px 14px; font-size: 13px; background: #fff; }
.search-btn { background: linear-gradient(135deg,#2e7d32,#43a047); color:#fff; border-radius:10px; padding:0 18px; font-size:13px; display:flex; align-items:center; white-space:nowrap; }
.reset-btn { background: #f5f5f5; color: #666; border-radius: 10px; padding: 0 16px; font-size: 13px; display: flex; align-items: center; white-space: nowrap; }
.filter-btn { background: #f5f5f5; color: #666; border-radius: 10px; padding: 0 16px; font-size: 13px; display: flex; align-items: center; white-space: nowrap; }
.filter-btn.active { background: #e8f5e9; color: #2e7d32; font-weight: 600; }

.filter-bar { background: #fff; border-radius: 10px; padding: 12px; margin-bottom: 10px; box-shadow: 0 1px 4px rgba(0,0,0,0.04); }
.filter-row { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.filter-row:last-child { margin-bottom: 0; }
.filter-label { font-size: 12px; color: #888; white-space: nowrap; min-width: 36px; }
.filter-scroll { display: flex; gap: 6px; overflow-x: auto; white-space: nowrap; }
.filter-pill { background: #f5f5f5; border-radius: 8px; padding: 6px 12px; font-size: 12px; color: #666; white-space: nowrap; }
.filter-pill.on { background: #e8f5e9; color: #2e7d32; font-weight: 600; }
.filter-picker { display: inline-flex; align-items: center; }
.date-text { border: 1px solid #dcdfe6; border-radius: 6px; padding: 6px 10px; font-size: 12px; background: #fff; min-width: 90px; display: inline-block; color: #666; }

.picker-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.4); z-index: 999; display: flex; align-items: flex-end; }
.picker-modal { background: #fff; border-radius: 16px 16px 0 0; width: 100%; max-height: 70vh; display: flex; flex-direction: column; }
.picker-header { display: flex; align-items: center; justify-content: space-between; padding: 16px; border-bottom: 1px solid #eee; }
.picker-cancel { color: #666; font-size: 14px; }
.wh-breadcrumb { display: flex; flex-wrap: wrap; gap: 4px; padding: 10px 16px; background: #f5f7fa; font-size: 13px; }
.wh-crumb { color: #2e7d32; }
.picker-list { padding: 0 16px 20px; max-height: 55vh; overflow-y: auto; }
.picker-item { display: flex; align-items: center; gap: 8px; padding: 12px 0; border-bottom: 1px solid #f5f5f5; }
</style>
