<script setup lang="ts">
import { ref, onMounted, nextTick, computed } from 'vue'
import request from '../../api/request'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'

const router = useRouter()
const loading = ref(true)
const stats = ref({
  productCount: 0, warehouseCount: 0,
  todayPurchaseCount: 0, todaySalesCount: 0,
  todayPurchaseQty: 0, todaySalesQty: 0,
  alertCount: 0,
})
const alertList = ref<any[]>([])
const recentLogs = ref<any[]>([])

const purchaseData = ref<any>({ labels: [], inQty: [], inAmt: [] })
const salesData = ref<any>({ labels: [], outQty: [], outAmt: [] })

const chartPurchaseDaily = ref<HTMLDivElement>()
const chartSalesDaily = ref<HTMLDivElement>()
const chartPurchaseCum = ref<HTMLDivElement>()
const chartSalesCum = ref<HTMLDivElement>()

const moduleMap: Record<string, string> = {
  product: '商品', purchase: '采购入库', sales: '销售出库',
  stocktake: '盘点', transfer: '调拨', inventory: '库存',
  supplier: '供应商', customer: '客户', warehouse: '仓库',
  system: '系统', auth: '登录',
}

const cumData = computed(() => {
  const now = new Date()
  const prefix = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
  const days: string[] = []
  const inCum: number[] = []
  const outCum: number[] = []
  let inS = 0, outS = 0
  for (let i = 0; i < purchaseData.value.labels.length; i++) {
    const d = purchaseData.value.labels[i]
    if (!d.startsWith(prefix)) continue
    days.push(d.slice(8, 10) + '日')
    inS += purchaseData.value.inQty[i] || 0
    inCum.push(inS)
    const oi = salesData.value.labels.indexOf(d)
    outS += oi !== -1 ? (salesData.value.outQty[oi] || 0) : 0
    outCum.push(outS)
  }
  return { days, inCum, outCum }
})

function renderChart(el: HTMLDivElement | undefined, title: string,
  labels: string[], data: number[], color: string) {
  if (!el || !labels.length) return
  const chart = echarts.init(el)
  chart.setOption({
    title: { text: title, left: 'center', textStyle: { fontSize: 12, color: '#666' } },
    tooltip: { trigger: 'axis', formatter: (p: any) => `${p[0].axisValue}<br/><b>${p[0].value}</b> 件` },
    grid: { left: '2%', right: '2%', bottom: '2%', top: '20%', containLabel: true },
    xAxis: {
      type: 'category', data: labels,
      axisLabel: { fontSize: 9, color: '#999', interval: 'auto' },
      axisLine: { lineStyle: { color: '#eee' } },
    },
    yAxis: {
      type: 'value', minInterval: 1,
      axisLabel: { fontSize: 9 },
      splitLine: { lineStyle: { color: '#f5f5f5' } },
    },
    series: [{
      type: 'line', smooth: true, data,
      symbol: 'circle', symbolSize: 3,
      lineStyle: { width: 2, color },
      itemStyle: { color },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: color + '30' }, { offset: 1, color: color + '05' }
        ])
      },
    }],
  })
}

function formatTime(t: string) {
  if (!t) return ''
  const d = new Date(t)
  const now = new Date()
  const isToday = d.toDateString() === now.toDateString()
  const mm = String(d.getMonth() + 1).padStart(2, '0')
  const dd = String(d.getDate()).padStart(2, '0')
  const hh = String(d.getHours()).padStart(2, '0')
  const mi = String(d.getMinutes()).padStart(2, '0')
  if (isToday) return `今天 ${hh}:${mi}`
  const yesterday = new Date(now); yesterday.setDate(yesterday.getDate() - 1)
  if (d.toDateString() === yesterday.toDateString()) return `昨天 ${hh}:${mi}`
  return `${mm}月${dd}日 ${hh}:${mi}`
}

onMounted(async () => {
  try {
    const res = await request.get('/dashboard/stats')
    stats.value = res.data.data
    alertList.value = res.data.data.alertList || []
  } catch { /* ignore */ }

  try {
    const [pRes, sRes, logRes] = await Promise.all([
      request.get('/report/purchase-summary', { params: { days: 30 } }),
      request.get('/report/sales-summary', { params: { days: 30 } }),
      request.get('/system/log/page', { params: { page: 1, size: 8 } }),
    ])
    purchaseData.value = pRes.data.data
    salesData.value = sRes.data.data
    recentLogs.value = logRes.data.data.records || []
  } catch { /* ignore */ }

  nextTick(() => {
    const pLabels = (purchaseData.value.labels || []).map((d: string) => d.slice(5))
    const sLabels = (salesData.value.labels || []).map((d: string) => d.slice(5))
    renderChart(chartPurchaseDaily.value, '近30天每日入库', pLabels, purchaseData.value.inQty || [], '#2e7d32')
    renderChart(chartSalesDaily.value, '近30天每日出库', sLabels, salesData.value.outQty || [], '#e65100')
    renderChart(chartPurchaseCum.value, '本月累计入库', cumData.value.days, cumData.value.inCum, '#2e7d32')
    renderChart(chartSalesCum.value, '本月累计出库', cumData.value.days, cumData.value.outCum, '#e65100')
  })
  loading.value = false
})
</script>

<template>
  <div v-loading="loading">
    <h2 style="margin-bottom:20px;border:none;padding:0;">工作台</h2>

    <!-- 顶部统计 -->
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon" style="background:#e8f5e9;color:#2e7d32;">
          <svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor"><path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H5.17L4 17.17V4h16v12z"/></svg>
        </div>
        <div>
          <div class="stat-label">商品总数</div>
          <div class="stat-value">{{ stats.productCount }}</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:#e3f2fd;color:#1565c0;">
          <svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor"><path d="M19 8h-1V3H6v5H5c-1.66 0-3 1.34-3 3v6h4v4h12v-4h4v-6c0-1.66-1.34-3-3-3zM8 5h8v3H8V5zm8 14H8v-4h8v4zm2-4v-2H6v2H4v-4c0-.55.45-1 1-1h14c.55 0 1 .45 1 1v4h-2z"/></svg>
        </div>
        <div>
          <div class="stat-label">仓库数量</div>
          <div class="stat-value">{{ stats.warehouseCount }}</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:#e8f5e9;color:#2e7d32;">
          <svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor"><path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm-7 14l-5-5 1.41-1.41L12 14.17l4.59-4.58L18 11l-6 6z"/></svg>
        </div>
        <div>
          <div class="stat-label">今日入库</div>
          <div class="stat-value">{{ stats.todayPurchaseCount }}<span style="font-size:14px;font-weight:400;color:#999;"> 单</span>
            <span style="font-size:20px;margin-left:8px;">{{ stats.todayPurchaseQty }}</span><span style="font-size:14px;font-weight:400;color:#999;"> 件</span>
          </div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:#fff3e0;color:#e65100;">
          <svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor"><path d="M19 3H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zm0 16H5V5h14v14zM7 11h10v2H7z"/></svg>
        </div>
        <div>
          <div class="stat-label">今日出库</div>
          <div class="stat-value">{{ stats.todaySalesCount }}<span style="font-size:14px;font-weight:400;color:#999;"> 单</span>
            <span style="font-size:20px;margin-left:8px;">{{ stats.todaySalesQty }}</span><span style="font-size:14px;font-weight:400;color:#999;"> 件</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 快捷操作 -->
    <div class="section-card">
      <div class="section-title">快捷操作</div>
      <div style="display:flex;gap:8px;flex-wrap:wrap;">
        <el-button type="primary" size="small" @click="router.push('/purchase/create')">+ 入库</el-button>
        <el-button type="primary" size="small" style="background:#e65100;border-color:#e65100;" @click="router.push('/sales/create')">+ 出库</el-button>
        <el-button size="small" @click="router.push('/stocktake/create')">+ 盘点</el-button>
        <el-button size="small" @click="router.push('/transfer/create')">+ 调拨</el-button>
        <el-divider direction="vertical" />
        <el-button text size="small" @click="router.push('/product')">商品管理</el-button>
        <el-button text size="small" @click="router.push('/inventory')">库存查看</el-button>
        <el-button text size="small" @click="router.push('/report')">统计报表</el-button>
      </div>
    </div>

    <!-- 图表 2x2 -->
    <div class="chart-grid">
      <div class="chart-card">
        <div ref="chartPurchaseDaily" style="height:190px;" />
        <div v-if="!purchaseData.labels?.length" style="height:190px;display:flex;align-items:center;justify-content:center;color:#999;">暂无数据</div>
      </div>
      <div class="chart-card">
        <div ref="chartSalesDaily" style="height:190px;" />
        <div v-if="!salesData.labels?.length" style="height:190px;display:flex;align-items:center;justify-content:center;color:#999;">暂无数据</div>
      </div>
      <div class="chart-card">
        <div ref="chartPurchaseCum" style="height:190px;" />
        <div v-if="!cumData.days.length" style="height:190px;display:flex;align-items:center;justify-content:center;color:#999;">暂无本月数据</div>
      </div>
      <div class="chart-card">
        <div ref="chartSalesCum" style="height:190px;" />
        <div v-if="!cumData.days.length" style="height:190px;display:flex;align-items:center;justify-content:center;color:#999;">暂无本月数据</div>
      </div>
    </div>

    <!-- 底部：最近动态 + 库存预警 -->
    <div class="bottom-row">
      <!-- 最近动态 -->
      <div class="recent-card">
        <div class="bottom-title">最近动态</div>
        <div v-if="recentLogs.length">
          <div v-for="log in recentLogs" :key="log.id" class="log-item">
            <span class="log-time">{{ formatTime(log.createTime) }}</span>
            <el-tag size="small" :type="log.action === 'delete' ? 'danger' : 'info'" style="margin-right:6px;">{{ moduleMap[log.module] || log.module }}</el-tag>
            <span class="log-detail">{{ log.detail || log.targetId || '-' }}</span>
          </div>
        </div>
        <div v-else style="color:#999;text-align:center;padding:20px;font-size:13px;">暂无动态</div>
      </div>

      <!-- 库存预警（半宽，列表形式） -->
      <div class="alert-card">
        <div class="bottom-title" style="display:flex;justify-content:space-between;align-items:center;">
          <span>库存预警 ({{ stats.alertCount }})</span>
          <el-button v-if="stats.alertCount" text size="small" @click="router.push('/inventory')">查看全部 →</el-button>
        </div>
        <div v-if="alertList.length">
          <el-table :data="alertList" stripe border size="small" @row-click="() => router.push('/inventory')" style="cursor:pointer;">
            <el-table-column prop="productName" label="商品" min-width="80" />
            <el-table-column prop="warehouseName" label="仓库" width="70" />
            <el-table-column prop="quantity" label="库存" width="60" align="center">
              <template #default="{ row }"><span style="color:#f56c6c;font-weight:600;">{{ row.quantity }}</span></template>
            </el-table-column>
            <el-table-column prop="minStock" label="安全库存" width="70" align="center" />
          </el-table>
        </div>
        <div v-else style="color:#999;text-align:center;padding:20px;font-size:13px;">暂无预警</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 16px; }
.stat-card {
  background: #fff; border-radius: 8px; padding: 16px 18px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06); border: 1px solid #e8ece8;
  display: flex; align-items: center; gap: 14px;
}
.stat-icon {
  width: 40px; height: 40px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
}
.stat-label { font-size: 13px; color: #999; margin-bottom: 2px; }
.stat-value { font-size: 24px; font-weight: 700; color: #303133; }

.section-card {
  background: #fff; border-radius: 8px; padding: 14px 18px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06); border: 1px solid #e8ece8;
  margin-bottom: 16px;
}
.section-title {
  font-size: 13px; font-weight: 600; color: #303133;
  margin-bottom: 10px; padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.chart-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 12px; }
.chart-card {
  background: #fff; border-radius: 8px; padding: 6px 4px 2px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06); border: 1px solid #e8ece8;
}

.bottom-row { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.recent-card, .alert-card {
  background: #fff; border-radius: 8px; padding: 14px 16px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06); border: 1px solid #e8ece8;
}
.bottom-title {
  font-size: 13px; font-weight: 600; color: #303133;
  margin-bottom: 10px; padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.log-item {
  display: flex; align-items: center; gap: 8px;
  padding: 6px 0; font-size: 12px;
  border-bottom: 1px solid #f5f5f5;
}
.log-item:last-child { border-bottom: none; }
.log-time { color: #999; white-space: nowrap; flex-shrink: 0; width: 72px; }
.log-detail { color: #333; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
</style>
