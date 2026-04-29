<script setup>
import { ref, onMounted } from 'vue'
import request from '@/api/request'

const suppliers = ref([])
const warehouses = ref([])
const products = ref([])
const submitting = ref(false)

const form = ref({
  supplierId: null,
  warehouseId: null,
  orderDate: new Date().toISOString().slice(0, 10),
  remark: '',
  items: [],
})

async function fetchBase() {
  const [sRes, wRes, pRes] = await Promise.all([
    request.get('/supplier/list'),
    request.get('/warehouse/list'),
    request.get('/product/list'),
  ])
  suppliers.value = sRes.data
  warehouses.value = wRes.data
  products.value = pRes.data
}

function addItem() {
  form.value.items.push({ productId: null, productName: '', quantity: 1, unitPrice: null, amount: null, batchNo: '', productionDate: '', expiryDate: '' })
}

function removeItem(index) {
  form.value.items.splice(index, 1)
}

function onProductChange(item) {
  const p = products.value.find(x => x.id === item.productId)
  if (p) {
    item.productName = p.name
    if (!item.unitPrice) item.unitPrice = p.purchasePrice
  }
}

function scanCode(index) {
  uni.scanCode({
    success: (res) => {
      const code = res.result
      const p = products.value.find(x => x.code === code)
      if (p) {
        const item = form.value.items[index]
        item.productId = p.id
        item.productName = p.name
        if (!item.unitPrice) item.unitPrice = p.purchasePrice
      } else {
        uni.showToast({ title: '未找到该商品', icon: 'none' })
      }
    },
    fail: () => { /* user cancelled */ },
  })
}

function calcAmount(item) {
  if (item.quantity && item.unitPrice != null) item.amount = item.quantity * item.unitPrice
}

async function handleSubmit() {
  if (!form.value.warehouseId || form.value.items.length === 0) {
    uni.showToast({ title: '请填写完整信息', icon: 'none' }); return
  }
  submitting.value = true
  try {
    const res = await request.post('/purchase-order', form.value)
    await request.put(`/purchase-order/${res.data}/submit`)
    uni.showToast({ title: '入库成功', icon: 'success' })
    uni.navigateBack()
  } finally { submitting.value = false }
}

onMounted(fetchBase)
</script>

<template>
  <view class="page">
    <!-- 基本信息 -->
    <view class="section">
      <view class="form-item">
        <text class="label">供应商</text>
        <picker @change="e => form.supplierId = suppliers[e.detail.value]?.id" :range="suppliers" range-key="name">
          <view class="picker">{{ suppliers.find(s => s.id === form.supplierId)?.name || '请选择' }}</view>
        </picker>
      </view>
      <view class="form-item">
        <text class="label">仓库 *</text>
        <picker @change="e => form.warehouseId = warehouses[e.detail.value]?.id" :range="warehouses" range-key="name">
          <view class="picker">{{ warehouses.find(w => w.id === form.warehouseId)?.name || '请选择' }}</view>
        </picker>
      </view>
      <view class="form-item">
        <text class="label">备注</text>
        <input v-model="form.remark" class="input" placeholder="备注信息" />
      </view>
    </view>

    <!-- 商品明细 -->
    <view class="section">
      <view class="section-header">
        <text class="section-title">商品明细</text>
        <text class="add-link" @click="addItem">+ 添加</text>
      </view>

      <view v-for="(item, index) in form.items" :key="index" class="item-card">
        <view class="item-header">
          <text>商品 {{ index + 1 }}</text>
          <text class="del-link" @click="removeItem(index)">删除</text>
        </view>
        <view class="ic-row">
          <picker @change="e => { item.productId = products[e.detail.value]?.id; onProductChange(item) }" :range="products" range-key="name" style="flex:1">
            <view class="picker">{{ products.find(p => p.id === item.productId)?.name || '选择商品' }}</view>
          </picker>
          <view class="scan-btn" @click="scanCode(index)">📷</view>
        </view>
        <view class="item-row">
          <view class="item-field">
            <text class="label-sm">数量</text>
            <input v-model="item.quantity" class="input-sm" type="number" @input="calcAmount(item)" />
          </view>
          <view class="item-field">
            <text class="label-sm">单价</text>
            <input v-model="item.unitPrice" class="input-sm" type="digit" @input="calcAmount(item)" />
          </view>
          <view class="item-field">
            <text class="label-sm">金额</text>
            <text class="amount">¥{{ (item.amount || 0).toFixed(2) }}</text>
          </view>
        </view>
        <view class="item-row">
          <view class="item-field">
            <text class="label-sm">批次号</text>
            <input v-model="item.batchNo" class="input-sm" placeholder="选填" />
          </view>
        </view>
      </view>
    </view>

    <button class="submit-btn" :loading="submitting" @click="handleSubmit">确认入库</button>
  </view>
</template>

<style scoped>
.page { padding: 16px; padding-bottom: 80px; }
.section { background: #fff; border-radius: 8px; padding: 16px; margin-bottom: 12px; }
.form-item { margin-bottom: 12px; }
.label { display: block; font-size: 14px; color: #333; margin-bottom: 4px; }
.picker { border: 1px solid #dcdfe6; border-radius: 6px; padding: 10px 12px; font-size: 14px; color: #333; background: #fff; }
.input { border: 1px solid #dcdfe6; border-radius: 6px; padding: 10px 12px; font-size: 14px; width: 100%; box-sizing: border-box; }
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.section-title { font-weight: bold; }
.add-link { color: #2e7d32; font-size: 14px; }
.item-card { background: #f8f9fa; border-radius: 6px; padding: 12px; margin-bottom: 8px; }
.item-header { display: flex; justify-content: space-between; margin-bottom: 8px; font-weight: bold; }
.del-link { color: #ff4d4f; }
.item-row { display: flex; gap: 8px; margin-top: 8px; }
.item-field { flex: 1; }
.label-sm { font-size: 12px; color: #666; }
.input-sm { border: 1px solid #dcdfe6; border-radius: 4px; padding: 6px 8px; font-size: 13px; width: 100%; box-sizing: border-box; }
.amount { font-size: 14px; color: #f56c6c; font-weight: bold; }
.submit-btn { position: fixed; bottom: 0; left: 0; right: 0; margin: 16px; background: #2e7d32; color: #fff; border: none; border-radius: 8px; height: 44px; line-height: 44px; font-size: 16px; }
.ic-row { display: flex; align-items: center; gap: 8px; }
.scan-btn { width: 40px; height: 40px; background: #e8f5e9; border-radius: 6px; display: flex; align-items: center; justify-content: center; font-size: 20px; flex-shrink: 0; }
</style>
