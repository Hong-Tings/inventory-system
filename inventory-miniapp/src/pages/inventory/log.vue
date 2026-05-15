<script setup>
import { ref, computed } from 'vue'
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app'
import request from '@/api/request'
import FloatingHome from '@/components/FloatingHome'

const list = ref([])
const loading = ref(false)
const keyword = ref('')
const showFilters = ref(false)

// 级联仓库选择
const warehouseTree = ref([])
const whCascade = ref([])
const whOptions = ref([])
const showWhPicker = ref(false)
const warehouseId = ref(null)

async function fetchData() {
  loading.value = true
  try {
    const params = { page: 1, size: 50 }
    if (keyword.value) { params.productName = keyword.value; params.refOrderNo = keyword.value }
    if (warehouseId.value) params.warehouseId = warehouseId.value
    const res = await request.get('/inventory/log/page', { params })
    list.value = res.data.records
  } finally { loading.value = false }
}

async function fetchWarehouseTree() {
  const res = await request.get('/warehouse/tree')
  warehouseTree.value = res.data || []
}

function onSearch() { fetchData() }

// 级联仓库
function openWarehousePicker() { whCascade.value = []; whOptions.value = warehouseTree.value || []; showWhPicker.value = true }
function selectWhLevel(item) {
  whCascade.value.push(item)
  if (item.children?.length) { whOptions.value = item.children }
  else { warehouseId.value = item.id; showWhPicker.value = false; fetchData() }
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

onShow(() => {
  fetchData()
  if (!warehouseTree.value.length) fetchWarehouseTree()
})
onPullDownRefresh(() => { fetchData(); uni.stopPullDownRefresh() })
</script>

<template>
  <view class="page">
    <view class="page-bar">
      <text class="page-title">库存流水</text>
    </view>
    <view class="search-row">
      <input v-model="keyword" class="search-input" placeholder="商品名称/编码或关联单号" @confirm="onSearch" style="flex:1;" />
      <view class="search-btn" @click="onSearch">搜索</view>
      <view class="reset-btn" @click="keyword = ''; warehouseId = null; fetchData()">重置</view>
      <view class="filter-btn" :class="{ active: warehouseId }" @click="showFilters = !showFilters">筛选</view>
    </view>

    <view v-if="showFilters" class="filter-bar">
      <view class="filter-row">
        <text class="filter-label">仓库</text>
        <view class="filter-pill filter-picker" :class="{ on: warehouseId }" @click="openWarehousePicker">{{ whLabel }}</view>
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
.filter-bar { background: #fff; border-radius: 10px; padding: 12px; margin-bottom: 10px; box-shadow: 0 1px 4px rgba(0,0,0,0.04); }
.filter-row { display: flex; align-items: center; gap: 8px; }
.filter-label { font-size: 12px; color: #888; white-space: nowrap; }
.search-btn { background: linear-gradient(135deg,#2e7d32,#43a047); color:#fff; border-radius:10px; padding:0 18px; font-size:13px; display:flex; align-items:center; white-space:nowrap; box-shadow:0 2px 8px rgba(46,125,50,0.2); }
.type-dot { width:8px; height:8px; border-radius:50%; display:inline-block; }
.reset-btn { background: #f5f5f5; color: #666; border-radius: 10px; padding: 0 16px; font-size: 13px; display: flex; align-items: center; white-space: nowrap; }
.filter-btn { background: #f5f5f5; color: #666; border-radius: 10px; padding: 0 16px; font-size: 13px; display: flex; align-items: center; white-space: nowrap; }
.filter-btn.active { background: #e8f5e9; color: #2e7d32; font-weight: 600; }
.filter-picker { background:#fff; border-radius:10px; padding:0 14px; font-size:13px; display:flex; align-items:center; white-space:nowrap; box-shadow:0 1px 4px rgba(0,0,0,0.04); color:#666; }
.filter-picker.on { background:#e8f5e9; color:#2e7d32; font-weight:600; }
.picker-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.4); z-index: 999; display: flex; align-items: flex-end; }
.picker-modal { background: #fff; border-radius: 16px 16px 0 0; width: 100%; max-height: 70vh; display: flex; flex-direction: column; }
.picker-header { display: flex; align-items: center; justify-content: space-between; padding: 16px; border-bottom: 1px solid #eee; }
.picker-cancel { color: #666; font-size: 14px; }
.wh-breadcrumb { display: flex; flex-wrap: wrap; gap: 4px; padding: 10px 16px; background: #f5f7fa; font-size: 13px; }
.wh-crumb { color: #2e7d32; }
.picker-list { padding: 0 16px 20px; max-height: 55vh; overflow-y: auto; }
.picker-item { display: flex; align-items: center; gap: 8px; padding: 12px 0; border-bottom: 1px solid #f5f5f5; }
</style>
