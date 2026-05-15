<script setup>
import { ref, onMounted } from 'vue'
import request from '@/api/request'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const statusBarHeight = ref(0)
const stats = ref({ productCount: 0, warehouseCount: 0, todayPurchaseQty: 0, todaySalesQty: 0, alertCount: 0 })
const animated = ref(false)

onMounted(async () => {
  statusBarHeight.value = uni.getSystemInfoSync().statusBarHeight || 20
  try {
    const res = await request.get('/dashboard/stats')
    stats.value = res.data
  } catch { /* ignore */ }
  // 触发数字动画
  setTimeout(() => { animated.value = true }, 100)
})

function goPage(url) {
  const tabs = ['/pages/purchase/list','/pages/sales/list','/pages/home/home','/pages/inventory/query','/pages/profile/profile']
  if (tabs.includes(url)) uni.switchTab({ url })
  else uni.navigateTo({ url })
}
</script>

<template>
  <view class="page" :style="{ paddingTop: statusBarHeight + 'px' }">
    <!-- 顶部用户栏 -->
    <view class="top-bar">
      <view>
        <text class="top-title">📦 进销存</text>
        <text class="top-sub">{{ userStore.userInfo?.realName || userStore.userInfo?.username || '用户' }}</text>
      </view>
      <view class="top-avatar" @click="goPage('/pages/profile/profile')">
        {{ (userStore.userInfo?.realName || '用').charAt(0) }}
      </view>
    </view>

    <!-- 统计卡片网格 -->
    <view class="stat-grid">
      <view class="stat-card sc-green" @click="goPage('/pages/product/list')">
        <view class="sc-icon">📦</view>
        <text class="sc-num">{{ animated ? stats.productCount : 0 }}</text>
        <text class="sc-lbl">商品总数</text>
      </view>
      <view class="stat-card sc-blue" @click="goPage('/pages/warehouse/list')">
        <view class="sc-icon">🏭</view>
        <text class="sc-num">{{ animated ? stats.warehouseCount : 0 }}</text>
        <text class="sc-lbl">仓库数量</text>
      </view>
      <view class="stat-card sc-orange" @click="goPage('/pages/purchase/list')">
        <view class="sc-icon">📥</view>
        <text class="sc-num">{{ animated ? stats.todayPurchaseQty : 0 }}</text>
        <text class="sc-lbl">今日入库</text>
      </view>
      <view class="stat-card sc-red" @click="goPage('/pages/alert/list')">
        <view class="sc-icon">⚠️</view>
        <text class="sc-num" style="color:#c62828;">{{ animated ? stats.alertCount : 0 }}</text>
        <text class="sc-lbl">库存预警</text>
      </view>
    </view>

    <!-- 快捷操作 -->
    <view class="section">
      <text class="section-title">快捷操作</text>
      <view class="action-grid">
        <view class="action-btn ab-green" @click="goPage('/pages/purchase/create')">
          <view class="ab-icon">📥</view>
          <text class="ab-lbl">采购入库</text>
        </view>
        <view class="action-btn ab-orange" @click="goPage('/pages/sales/create')">
          <view class="ab-icon">📤</view>
          <text class="ab-lbl">销售出库</text>
        </view>
        <view class="action-btn ab-blue" @click="goPage('/pages/stocktake/create')">
          <view class="ab-icon">📋</view>
          <text class="ab-lbl">库存盘点</text>
        </view>
        <view class="action-btn ab-purple" @click="goPage('/pages/transfer/create')">
          <view class="ab-icon">🔄</view>
          <text class="ab-lbl">库存调拨</text>
        </view>
      </view>
    </view>

    <!-- 单据入口网格 -->
    <view class="section">
      <text class="section-title">单据列表</text>
      <view class="doc-grid">
        <view class="doc-item" @click="goPage('/pages/purchase/list')">
          <text class="doc-icon" style="color:#2e7d32;">📦</text>
          <text class="doc-lbl">入库单</text>
        </view>
        <view class="doc-item" @click="goPage('/pages/sales/list')">
          <text class="doc-icon" style="color:#e65100;">📤</text>
          <text class="doc-lbl">出库单</text>
        </view>
        <view class="doc-item" @click="goPage('/pages/stocktake/list')">
          <text class="doc-icon" style="color:#1565c0;">📋</text>
          <text class="doc-lbl">盘点单</text>
        </view>
        <view class="doc-item" @click="goPage('/pages/transfer/list')">
          <text class="doc-icon" style="color:#7b1fa2;">🔄</text>
          <text class="doc-lbl">调拨单</text>
        </view>
        <view class="doc-item" @click="goPage('/pages/inventory/log')">
          <text class="doc-icon" style="color:#00838f;">📊</text>
          <text class="doc-lbl">库存流水</text>
        </view>
        <view class="doc-item" @click="goPage('/pages/product/list')">
          <text class="doc-icon" style="color:#558b2f;">📃</text>
          <text class="doc-lbl">商品列表</text>
        </view>
        <view class="doc-item" @click="goPage('/pages/pending/list')">
          <text class="doc-icon" style="color:#e65100;">⏳</text>
          <text class="doc-lbl">待审批</text>
        </view>
      </view>
    </view>
  </view>
</template>

<style scoped>
.page { padding: 0 16px 20px; background: #e8f0e8; min-height: 100vh; }

/* ===== 顶部用户栏 ===== */
.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0 20px;
}
.top-title { font-size: 22px; font-weight: 700; color: #1a1a1a; display: block; }
.top-sub { font-size: 13px; color: #888; margin-top: 2px; display: block; }
.top-avatar {
  width: 42px; height: 42px; border-radius: 50%;
  background: linear-gradient(135deg, #2e7d32, #43a047);
  color: #fff; display: flex; align-items: center; justify-content: center;
  font-size: 16px; font-weight: 700;
  box-shadow: 0 3px 10px rgba(46,125,50,0.25);
  transition: transform 0.15s;
}
.top-avatar:active { transform: scale(0.9); }

/* ===== 统计卡片 ===== */
.stat-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 20px;
}
.stat-card {
  border-radius: 16px;
  padding: 18px 16px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.06);
  transition: all 0.15s;
  animation: cardPop 0.4s ease backwards;
}
.stat-card:nth-child(1) { animation-delay: 0.05s; }
.stat-card:nth-child(2) { animation-delay: 0.10s; }
.stat-card:nth-child(3) { animation-delay: 0.15s; }
.stat-card:nth-child(4) { animation-delay: 0.20s; }
.stat-card:active { transform: scale(0.94); }

.sc-green { background: linear-gradient(135deg, #e8f5e9, #c8e6c9); }
.sc-blue { background: linear-gradient(135deg, #e3f2fd, #bbdefb); }
.sc-orange { background: linear-gradient(135deg, #fff3e0, #ffe0b2); }
.sc-red { background: linear-gradient(135deg, #fce4ec, #f8bbd0); }

.sc-icon { font-size: 24px; margin-bottom: 8px; }
.sc-num { font-size: 28px; font-weight: 800; color: #1a1a1a; display: block; line-height: 1.1; }
.sc-lbl { font-size: 12px; color: #666; margin-top: 4px; display: block; }

/* ===== 区块 ===== */
.section { margin-bottom: 20px; }
.section-title {
  font-size: 14px;
  font-weight: 700;
  color: #555;
  display: block;
  margin-bottom: 12px;
  letter-spacing: 0.5px;
}

/* ===== 快捷操作 ===== */
.action-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}
.action-btn {
  border-radius: 14px;
  padding: 18px 12px;
  text-align: center;
  box-shadow: 0 3px 12px rgba(0,0,0,0.05);
  transition: all 0.15s;
  background: #fff;
  border-top: 3px solid transparent;
}
.action-btn:active { transform: scale(0.94); }
.ab-green { border-top-color: #2e7d32; }
.ab-orange { border-top-color: #e65100; }
.ab-blue { border-top-color: #1565c0; }
.ab-purple { border-top-color: #7b1fa2; }
.ab-icon { font-size: 26px; display: block; margin-bottom: 6px; }
.ab-lbl { font-size: 13px; color: #333; font-weight: 600; }

/* ===== 单据入口 ===== */
.doc-grid {
  display: grid;
  grid-template-columns: 1fr 1fr 1fr;
  gap: 10px;
}
.doc-item {
  background: #fff;
  border-radius: 14px;
  padding: 18px 6px;
  text-align: center;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  transition: all 0.15s;
}
.doc-item:active { transform: scale(0.94); }
.doc-icon { display: block; font-size: 24px; margin-bottom: 6px; }
.doc-lbl { font-size: 12px; color: #555; font-weight: 500; display: block; }

/* ===== 动画 ===== */
@keyframes cardPop {
  from { opacity: 0; transform: scale(0.85) translateY(10px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}
</style>
