<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import request from '../../api/request'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'

const router = useRouter()
const stats = ref({
  productCount: 0, warehouseCount: 0,
  todayPurchaseCount: 0, todaySalesCount: 0,
  todayPurchaseQty: 0, todaySalesQty: 0,
  alertCount: 0,
})
const alertList = ref<any[]>([])

const purchaseData = ref<any>({ labels: [], inQty: [], inAmt: [] })
const salesData = ref<any>({ labels: [], outQty: [], outAmt: [] })
const purchaseChartRef = ref<HTMLDivElement>()
const salesChartRef = ref<HTMLDivElement>()
let purchaseChart: any = null
let salesChart: any = null

function renderChart(container: HTMLDivElement | undefined, labels: string[], data: number[]) {
  if (!container) return
  const chart = echarts.init(container)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '3%', bottom: '5%', containLabel: true },
    xAxis: { type: 'category', data: labels, axisLabel: { fontSize: 10, color: '#999' } },
    yAxis: { type: 'value', minInterval: 1, axisLabel: { fontSize: 10 } },
    series: [{
      type: 'bar',
      data: data,
      itemStyle: { color: '#2e7d32', borderRadius: [3,3,0,0] },
      barWidth: '50%',
    }],
  })
  return chart
}

onMounted(async () => {
  try {
    const res = await request.get('/dashboard/stats')
    stats.value = res.data.data
    alertList.value = res.data.data.alertList || []
  } catch { /* ignore */ }

  try {
    const [pRes, sRes] = await Promise.all([
      request.get('/report/purchase-summary', { params: { days: 30 } }),
      request.get('/report/sales-summary', { params: { days: 30 } }),
    ])
    purchaseData.value = pRes.data.data
    salesData.value = sRes.data.data
  } catch { /* ignore */ }

  nextTick(() => {
    if (purchaseData.value.labels?.length) {
      purchaseChart = renderChart(purchaseChartRef.value, purchaseData.value.labels, purchaseData.value.inQty)
    }
    if (salesData.value.labels?.length) {
      salesChart = renderChart(salesChartRef.value, salesData.value.labels, salesData.value.outQty)
    }
  })
})
</script>

<template>
  <div>
    <h2 style="margin-bottom:20px;border:none;padding:0;">工作台</h2>

    <!-- 顶部统计 -->
    <div class="stats-grid">
      <div class="stat-card"><div class="stat-label">商品总数</div><div class="stat-value">{{ stats.productCount }}</div></div>
      <div class="stat-card"><div class="stat-label">仓库数量</div><div class="stat-value">{{ stats.warehouseCount }}</div></div>
      <div class="stat-card"><div class="stat-label">今日入库</div>
        <div style="display:flex;align-items:baseline;gap:8px;">
          <span class="stat-value" style="font-size:22px;color:#2e7d32;">{{ stats.todayPurchaseCount }}</span>
          <span style="font-size:13px;color:#999;">单</span>
          <span class="stat-value" style="font-size:22px;">{{ stats.todayPurchaseQty }}</span>
          <span style="font-size:13px;color:#999;">件</span>
        </div>
      </div>
      <div class="stat-card"><div class="stat-label">今日出库</div>
        <div style="display:flex;align-items:baseline;gap:8px;">
          <span class="stat-value" style="font-size:22px;color:#e65100;">{{ stats.todaySalesCount }}</span>
          <span style="font-size:13px;color:#999;">单</span>
          <span class="stat-value" style="font-size:22px;">{{ stats.todaySalesQty }}</span>
          <span style="font-size:13px;color:#999;">件</span>
        </div>
      </div>
    </div>

    <!-- 快捷操作 + 库存预警 -->
    <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px;margin-bottom:20px;">
      <div class="stat-card">
        <div class="stat-label" style="margin-bottom:12px;">快捷操作</div>
        <div style="display:flex;gap:10px;flex-wrap:wrap;">
          <el-button type="primary" @click="router.push('/purchase/create')">+ 新建入库单</el-button>
          <el-button type="primary" style="background:#e65100;border-color:#e65100;" @click="router.push('/sales/create')">+ 新建出库单</el-button>
          <el-button plain @click="router.push('/stocktake/create')">+ 新建盘点单</el-button>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-label" style="margin-bottom:8px;color:#e65100;font-weight:600;">
          库存预警 ({{ stats.alertCount }})
          <el-button text size="small" @click="router.push('/inventory')" style="float:right;">查看全部</el-button>
        </div>
        <div v-if="alertList.length">
          <el-table :data="alertList" stripe border size="small" max-height="150" @row-click="() => router.push('/inventory')" style="cursor:pointer;">
            <el-table-column prop="productName" label="商品" min-width="90" />
            <el-table-column prop="warehouseName" label="仓库" width="80" />
            <el-table-column prop="quantity" label="库存" width="55">
              <template #default="{ row }"><span style="color:#f56c6c;font-weight:600;">{{ row.quantity }}</span></template>
            </el-table-column>
            <el-table-column prop="minStock" label="安全库存" width="70" />
          </el-table>
        </div>
        <div v-else style="color:#999;text-align:center;padding:16px;font-size:13px;">暂无预警</div>
      </div>
    </div>

    <!-- 入库/出库统计图 -->
    <div style="display:grid;grid-template-columns:1fr 1fr;gap:16px;">
      <div class="stat-card">
        <div class="stat-label" style="margin-bottom:8px;">入库统计（近30天）</div>
        <div ref="purchaseChartRef" style="height:220px;" v-if="purchaseData.labels?.length"></div>
        <div v-else style="height:220px;display:flex;align-items:center;justify-content:center;color:#999;">暂无数据</div>
      </div>
      <div class="stat-card">
        <div class="stat-label" style="margin-bottom:8px;">出库统计（近30天）</div>
        <div ref="salesChartRef" style="height:220px;" v-if="salesData.labels?.length"></div>
        <div v-else style="height:220px;display:flex;align-items:center;justify-content:center;color:#999;">暂无数据</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.stats-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 24px; }
.stat-card {
  background: #fff; border-radius: 8px; padding: 20px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06); border: 1px solid #e8ece8;
}
.stat-label { font-size: 13px; color: #999; margin-bottom: 4px; }
.stat-value { font-size: 26px; font-weight: 700; color: #333; }
</style>
