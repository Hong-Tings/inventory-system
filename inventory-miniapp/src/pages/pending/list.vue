<script setup>
import { ref, onShow } from 'vue'
import request from '@/api/request'
import FloatingHome from '@/components/FloatingHome'

const list = ref([])
const loading = ref(false)
const tab = ref('purchase')
const statusMap = { purchase: { 0: '草稿', 4: '待审批' }, sales: { 0: '草稿', 4: '待审批' }, transfer: { 0: '草稿', 4: '待审批' }, stocktake: { 0: '盘点中' } }
const moduleNames = { purchase: '采购入库', sales: '销售出库', transfer: '库存调拨', stocktake: '库存盘点' }

async function fetchList() {
  loading.value = true
  try {
    let records = []
    if (tab.value === 'purchase') {
      const res = await request.get('/purchase-order/page', { params: { page: 1, size: 50, status: 4 } })
      records = (res.data.records || []).map(r => ({ ...r, _module: 'purchase', _moduleName: '采购入库', _link: '/pages/purchase/detail?id=' + r.id }))
    } else if (tab.value === 'sales') {
      const res = await request.get('/sales-order/page', { params: { page: 1, size: 50, status: 4 } })
      records = (res.data.records || []).map(r => ({ ...r, _module: 'sales', _moduleName: '销售出库', _link: '/pages/sales/detail?id=' + r.id }))
    } else if (tab.value === 'transfer') {
      const res = await request.get('/transfer/page', { params: { page: 1, size: 50, status: 4 } })
      records = (res.data.records || []).map(r => ({ ...r, _module: 'transfer', _moduleName: '库存调拨', _link: '/pages/transfer/detail?id=' + r.id }))
    } else if (tab.value === 'stocktake') {
      const res = await request.get('/stock-take/page', { params: { page: 1, size: 50, status: 0 } })
      records = (res.data.records || []).map(r => ({ ...r, _module: 'stocktake', _moduleName: '库存盘点', _link: '/pages/stocktake/detail?id=' + r.id }))
    }
    list.value = records
  } finally { loading.value = false }
}

function goDetail(item) { uni.navigateTo({ url: item._link }) }

onShow(fetchList)
</script>

<template>
  <view class="page">
    <view class="page-bar">
      <text class="page-title">待审批</text>
    </view>
    <scroll-view scroll-x class="tab-scroll">
      <view v-for="(name, key) in moduleNames" :key="key" class="tab-pill" :class="{ on: tab === key }" @click="tab = key; fetchList()">{{ name }}</view>
    </scroll-view>

    <view v-if="loading" class="loading">加载中...</view>
    <view v-else>
      <view v-for="item in list" :key="item.id" class="card" @click="goDetail(item)">
        <view class="card-header">
          <view style="display:flex;align-items:center;gap:6px;">
            <text style="font-size:14px;">{{ item._moduleName }}</text>
            <text class="order-no" style="font-size:13px;">{{ item.orderNo }}</text>
          </view>
          <text class="status status-4">待审批</text>
        </view>
        <view class="card-body">
          <text>{{ item.supplierName || item.customerName || item.fromWarehouseName || item.warehouseName || '-' }}</text>
          <text style="color:#888;">数量: {{ item.totalQuantity || item.totalItems || '-' }}</text>
        </view>
        <view class="card-footer">
          <text>{{ item.orderDate }}</text>
          <text>{{ item.operatorName }}</text>
        </view>
      </view>
      <view v-if="list.length === 0 && !loading" class="empty">暂无待审批单据</view>
    </view>
    <FloatingHome />
  </view>
</template>

<style scoped>
.page { padding: 16px; }
.page-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.page-title { font-size: 18px; font-weight: bold; }
.tab-scroll { display: flex; gap: 8px; margin-bottom: 14px; overflow-x: auto; white-space: nowrap; }
.tab-pill { background: #f5f5f5; border-radius: 10px; padding: 8px 16px; font-size: 13px; color: #666; }
.tab-pill.on { background: #fff3e0; color: #e65100; font-weight: 600; }
.loading, .empty { text-align: center; color: #999; padding: 40px 0; }
.card { background: #fff; border-radius: 8px; padding: 12px 16px; margin-bottom: 12px; box-shadow: 0 1px 4px rgba(0,0,0,0.08); border-left: 4px solid #ff9800; }
.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
.order-no { font-size: 13px; color: #333; }
.status { font-size: 12px; padding: 2px 8px; border-radius: 4px; }
.status-4 { background: #fff3e0; color: #e65100; }
.card-body { font-size: 13px; color: #666; display: flex; justify-content: space-between; }
.card-footer { display: flex; justify-content: space-between; margin-top: 8px; font-size: 12px; color: #999; border-top: 1px solid #f0f4f0; padding-top: 6px; }
</style>
