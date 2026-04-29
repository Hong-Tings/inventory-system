<script setup lang="ts">
import { ref, onMounted, nextTick, watch } from 'vue'
import request from '../../api/request'
import * as echarts from 'echarts'

const tab = ref('purchase')
const purchaseData = ref<any>({ labels: [], inQty: [], inAmt: [] })
const salesData = ref<any>({ labels: [], outQty: [], outAmt: [] })
const alertList = ref<any[]>([])
const purchaseChartRef = ref<HTMLDivElement>()
const salesChartRef = ref<HTMLDivElement>()
let chart: any = null

const turnoverData = ref<any[]>([])

async function fetchReports() {
  try {
    const [pRes, sRes, aRes] = await Promise.all([
      request.get('/report/purchase-summary', { params: { days: 30 } }),
      request.get('/report/sales-summary', { params: { days: 30 } }),
      request.get('/report/inventory-alert'),
    ])
    purchaseData.value = pRes.data.data
    salesData.value = sRes.data.data
    alertList.value = aRes.data.data || []
  } catch { /* ignored */ }
}

function renderChart() {
  const el = tab.value === 'purchase' ? purchaseChartRef.value : salesChartRef.value
  if (!el) return
  if (chart) chart.dispose()
  chart = echarts.init(el)

  const data = tab.value === 'purchase' ? purchaseData.value : salesData.value
  const labels = data.labels || []
  const seriesData = tab.value === 'purchase' ? (data.inQty || []) : (data.outQty || [])

  chart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      data: labels,
      axisLabel: { fontSize: 11, color: '#999' },
    },
    yAxis: { type: 'value', minInterval: 1, axisLabel: { fontSize: 11 } },
    series: [{
      type: 'bar',
      data: seriesData,
      itemStyle: { color: '#2e7d32', borderRadius: [4,4,0,0] },
      barWidth: '60%',
    }],
  })
}

async function fetchTurnover() {
  try {
    const res = await request.get('/report/turnover-rate', { params: { days: 30 } })
    turnoverData.value = res.data.data || []
  } catch { /* ignore */ }
}

watch(tab, (val) => {
  if (val === 'turnover') {
    nextTick(() => fetchTurnover())
  } else if (val !== 'alert') {
    nextTick(() => renderChart())
  }
})

onMounted(async () => {
  await fetchReports()
  if (tab.value !== 'turnover') {
    nextTick(() => renderChart())
  }
})
</script>

<template>
  <div>
    <div class="page-header"><h2>统计报表</h2></div>
    <el-card>
      <el-tabs v-model="tab">
        <el-tab-pane label="入库汇总" name="purchase">
          <div v-show="tab === 'purchase' && purchaseData.labels?.length" ref="purchaseChartRef" style="height:260px;width:100%;"></div>
          <div v-if="tab === 'purchase' && (!purchaseData.labels || purchaseData.labels.length === 0)" style="height:260px;display:flex;align-items:center;justify-content:center;color:#999;">暂无数据</div>
          <el-descriptions :column="3" border style="margin-top:16px">
            <el-descriptions-item label="总单数">{{ purchaseData?.totalOrders || 0 }}</el-descriptions-item>
            <el-descriptions-item label="入库总数量">{{ purchaseData.inQty?.reduce((a: any, b: any) => Number(a) + Number(b), 0) || 0 }}</el-descriptions-item>
            <el-descriptions-item label="入库总金额">¥{{ (purchaseData.inAmt?.reduce((a: any, b: any) => Number(a) + Number(b), 0) || 0).toFixed(2) }}</el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>
        <el-tab-pane label="出库汇总" name="sales">
          <div v-show="tab === 'sales' && salesData.labels?.length" ref="salesChartRef" style="height:260px;width:100%;"></div>
          <div v-if="tab === 'sales' && (!salesData.labels || salesData.labels.length === 0)" style="height:260px;display:flex;align-items:center;justify-content:center;color:#999;">暂无数据</div>
          <el-descriptions :column="3" border style="margin-top:16px">
            <el-descriptions-item label="总单数">{{ salesData?.totalOrders || 0 }}</el-descriptions-item>
            <el-descriptions-item label="出库总数量">{{ salesData.outQty?.reduce((a: any, b: any) => Number(a) + Number(b), 0) || 0 }}</el-descriptions-item>
            <el-descriptions-item label="出库总金额">¥{{ (salesData.outAmt?.reduce((a: any, b: any) => Number(a) + Number(b), 0) || 0).toFixed(2) }}</el-descriptions-item>
          </el-descriptions>
        </el-tab-pane>
        <el-tab-pane label="库存预警" name="alert">
          <el-table :data="alertList" stripe border>
            <el-table-column prop="productName" label="商品名称" min-width="160" />
            <el-table-column prop="productCode" label="编码" width="120" />
            <el-table-column prop="warehouseName" label="仓库" width="140" />
            <el-table-column prop="quantity" label="当前库存" min="100" />
            <el-table-column prop="minStock" label="安全库存" width="100" />
          </el-table>
          <el-empty v-if="!alertList.length" description="暂无库存预警" />
        </el-tab-pane>
        <el-tab-pane label="周转率" name="turnover">
          <div style="margin-top:16px" v-if="turnoverData.length">
            <el-table :data="turnoverData" stripe border>
              <el-table-column prop="productName" label="商品名称" min-width="180" />
              <el-table-column prop="totalOut" label="出库总量" width="100" />
              <el-table-column prop="avgInventory" label="平均库存" width="100" />
              <el-table-column prop="turnoverRate" label="周转率" width="100">
                <template #default="{ row }">{{ row.turnoverRate?.toFixed(2) }}</template>
              </el-table-column>
            </el-table>
          </div>
          <el-empty v-else description="暂无周转数据" />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>
