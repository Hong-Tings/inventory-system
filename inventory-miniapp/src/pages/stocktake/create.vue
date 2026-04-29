<script setup>
import { ref, onMounted } from 'vue'
import request from '@/api/request'

const warehouses = ref([])
const products = ref([])
const loading = ref(false)

const form = ref({
  warehouseId: null,
  takeType: 0,
  remark: '',
})

const items = ref([])

async function fetchWarehouses() {
  const res = await request.get('/warehouse/list')
  warehouses.value = res.data
}

async function fetchProducts() {
  const res = await request.get('/product/list')
  products.value = res.data
}

async function startTake() {
  if (!form.value.warehouseId) {
    uni.showToast({ title: '请选择仓库', icon: 'none' }); return
  }
  loading.value = true
  try {
    const res = await request.post('/stock-take', form.value)
    form.value.stockTakeId = res.data

    const invRes = await request.get('/inventory/page', {
      params: { warehouseId: form.value.warehouseId, page: 1, size: 999 }
    })
    items.value = (invRes.data.records || []).map(i => ({
      productId: i.productId,
      productName: i.productName,
      batchNo: i.batchNo,
      bookQty: i.quantity,
      actualQty: i.quantity,
      diffQty: 0,
    }))
  } finally { loading.value = false }
}

function onQtyChange(item) {
  item.diffQty = item.actualQty - item.bookQty
}

function scanCode() {
  uni.scanCode({
    success: (res) => {
      const code = res.result
      const p = products.value.find(x => x.code === code)
      if (!p) { uni.showToast({ title: '未找到该商品', icon: 'none' }); return }
      const existing = items.value.find(i => i.productId === p.id)
      if (existing) {
        uni.showToast({ title: `已在列表中: ${p.name}`, icon: 'none' })
      } else {
        items.value.push({
          productId: p.id, productName: p.name, batchNo: '',
          bookQty: 0, actualQty: 0, diffQty: 0,
        })
      }
    },
    fail: () => {},
  })
}

async function finishTake() {
  if (!form.value.stockTakeId) return
  loading.value = true
  try {
    for (const item of items.value) {
      await request.post('/stock-take/item', {
        stockTakeId: form.value.stockTakeId,
        productId: item.productId,
        batchNo: item.batchNo,
        bookQty: item.bookQty,
        actualQty: item.actualQty,
        diffQty: item.diffQty,
      })
    }
    uni.showToast({ title: '盘点完成', icon: 'success' })
    uni.navigateBack()
  } finally { loading.value = false }
}

onMounted(() => { fetchWarehouses(); fetchProducts() })
</script>

<template>
  <view class="page">
    <view class="section">
      <view class="form-item">
        <text class="label">仓库 *</text>
        <picker @change="e => form.warehouseId = warehouses[e.detail.value]?.id" :range="warehouses" range-key="name">
          <view class="picker">{{ warehouses.find(w => w.id === form.warehouseId)?.name || '请选择' }}</view>
        </picker>
      </view>
      <button v-if="!form.stockTakeId" class="start-btn" :loading="loading" @click="startTake">开始盘点</button>
    </view>

    <view v-if="items.length > 0" class="section">
      <view class="section-header">
        <text class="section-title">盘点录入 ({{ items.length }})</text>
        <view class="scan-btn" @click="scanCode">📷</view>
      </view>

      <view v-for="(item, index) in items" :key="index" class="item-card">
        <text class="item-name">{{ item.productName }}</text>
        <view class="qty-row">
          <view class="qty-field">
            <text class="label-sm">账面</text>
            <text class="qty-book">{{ item.bookQty }}</text>
          </view>
          <view class="qty-field">
            <text class="label-sm">实盘</text>
            <input v-model="item.actualQty" class="input-sm" type="number" @input="onQtyChange(item)" />
          </view>
          <view class="qty-field">
            <text class="label-sm">差异</text>
            <text class="qty-diff" :class="{ 'has-diff': item.diffQty !== 0 }">{{ item.diffQty > 0 ? '+' : '' }}{{ item.diffQty }}</text>
          </view>
        </view>
      </view>

      <button class="submit-btn" :loading="loading" @click="finishTake">完成盘点</button>
    </view>
  </view>
</template>

<style scoped>
.page { padding: 16px; padding-bottom: 60px; }
.section { background: #fff; border-radius: 8px; padding: 16px; margin-bottom: 12px; }
.form-item { margin-bottom: 12px; }
.label { display: block; font-size: 14px; color: #333; margin-bottom: 4px; }
.picker { border: 1px solid #dcdfe6; border-radius: 6px; padding: 10px 12px; font-size: 14px; background: #fff; }
.start-btn { width: 100%; background: #2e7d32; color: #fff; border: none; border-radius: 6px; height: 42px; line-height: 42px; font-size: 15px; }
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.section-title { font-weight: bold; }
.item-card { background: #f8f9fa; border-radius: 6px; padding: 12px; margin-bottom: 8px; }
.item-name { font-weight: bold; font-size: 14px; }
.qty-row { display: flex; gap: 12px; margin-top: 8px; }
.qty-field { flex: 1; display: flex; flex-direction: column; align-items: center; gap: 4px; }
.label-sm { font-size: 12px; color: #666; }
.qty-book { font-size: 18px; font-weight: bold; }
.input-sm { border: 1px solid #2e7d32; border-radius: 4px; padding: 6px 8px; font-size: 16px; width: 60px; text-align: center; }
.qty-diff { font-size: 16px; font-weight: bold; color: #666; }
.has-diff { color: #f56c6c; }
.submit-btn { width: 100%; background: #2e7d32; color: #fff; border: none; border-radius: 6px; height: 42px; line-height: 42px; font-size: 15px; margin-top: 12px; }
.scan-btn { width: 40px; height: 40px; background: #e8f5e9; border-radius: 6px; display: flex; align-items: center; justify-content: center; font-size: 20px; flex-shrink: 0; }
</style>
