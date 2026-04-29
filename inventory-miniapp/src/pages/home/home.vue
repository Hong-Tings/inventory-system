<script setup>
import { ref, onMounted } from 'vue'
import request from '@/api/request'

const stats = ref({ productCount: 0, warehouseCount: 0, todayPurchaseCount: 0, todaySalesCount: 0, alertCount: 0 })

onMounted(async () => {
  try {
    const res = await request.get('/dashboard/stats')
    stats.value = res.data
  } catch { /* ignore */ }
})

function go(url) { uni.switchTab({ url }) }
function goPage(url) { uni.navigateTo({ url }) }
</script>

<template>
  <view class="page">
    <view class="header-bar">
      <text class="header-title">工作台</text>
      <text class="header-role">管理员</text>
    </view>

    <view class="stats-grid">
      <view class="stat-card"><text class="stat-num">{{ stats.productCount }}</text><text class="stat-lbl">商品总数</text></view>
      <view class="stat-card"><text class="stat-num">{{ stats.todayPurchaseCount }}</text><text class="stat-lbl">今日入库</text></view>
      <view class="stat-card"><text class="stat-num">{{ stats.todaySalesCount }}</text><text class="stat-lbl">今日出库</text></view>
      <view class="stat-card"><text class="stat-num" style="color:#e65100;">{{ stats.alertCount }}</text><text class="stat-lbl">库存预警</text></view>
    </view>

    <view class="quick-grid">
      <view class="quick-item" @click="go('/pages/purchase/list')">
        <text class="quick-icon">↓</text>
        <text class="quick-lbl">采购入库</text>
      </view>
      <view class="quick-item" @click="go('/pages/sales/list')">
        <text class="quick-icon">↑</text>
        <text class="quick-lbl">销售出库</text>
      </view>
      <view class="quick-item" @click="go('/pages/inventory/query')">
        <text class="quick-icon">≡</text>
        <text class="quick-lbl">库存查询</text>
      </view>
      <view class="quick-item" @click="goPage('/pages/stocktake/list')">
        <text class="quick-icon">⊙</text>
        <text class="quick-lbl">库存盘点</text>
      </view>
      <view class="quick-item" @click="goPage('/pages/transfer/list')" style="border:1px dashed #2e7d32;">
        <text class="quick-icon">⇄</text>
        <text class="quick-lbl" style="color:#2e7d32;">库存调拨</text>
      </view>
    </view>

    <view class="alert-card" @click="goPage('/pages/alert/list')">
      <view style="display:flex;justify-content:space-between;width:100%;">
        <text style="font-weight:600;font-size:14px;">⚠ 库存预警</text>
        <text style="color:#c62828;font-weight:600;">{{ stats.alertCount }}项</text>
      </view>
      <text style="font-size:12px;color:#999;margin-top:4px;" v-if="stats.alertCount">{{ stats.alertCount }}项商品库存不足，点击查看</text>
      <text style="font-size:12px;color:#999;margin-top:4px;" v-else>暂无预警</text>
    </view>
  </view>
</template>

<style scoped>
.page { padding: 12px; background: #f5f7f5; min-height: 100vh; }
.header-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.header-title { font-size: 18px; font-weight: 700; color: #333; }
.header-role { font-size: 13px; color: #999; }

.stats-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin-bottom: 12px; }
.stat-card { background: #fff; border-radius: 8px; padding: 16px; text-align: center; }
.stat-num { font-size: 24px; font-weight: 700; color: #2e7d32; display: block; }
.stat-lbl { font-size: 12px; color: #999; margin-top: 4px; display: block; }

.quick-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin-bottom: 12px; }
.quick-item { background: #fff; border-radius: 8px; padding: 16px; text-align: center; }
.quick-icon { font-size: 24px; display: block; }
.quick-lbl { font-size: 13px; color: #333; margin-top: 6px; display: block; }

.alert-card { background: #fff; border-radius: 8px; padding: 14px 16px; display: flex; flex-direction: column; }
</style>
