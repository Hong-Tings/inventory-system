<script setup>
import { ref, onMounted } from 'vue'
import request from '@/api/request'

const warehouses = ref([])
const products = ref([])
const submitting = ref(false)

const form = ref({
  fromWarehouseId: null,
  toWarehouseId: null,
  remark: '',
  items: [],
})

async function fetchBase() {
  const [wRes, pRes] = await Promise.all([
    request.get('/warehouse/list'),
    request.get('/product/list'),
  ])
  warehouses.value = wRes.data
  products.value = pRes.data
}

function addItem() {
  form.value.items.push({ productId: null, quantity: 1, batchNo: '' })
}

function removeItem(index) { form.value.items.splice(index, 1) }

function scanCode(index) {
  uni.scanCode({
    success: (res) => {
      const p = products.value.find(x => x.code === res.result)
      if (p) {
        form.value.items[index].productId = p.id
      } else {
        uni.showToast({ title: '未找到该商品', icon: 'none' })
      }
    }
  })
}

async function handleSubmit() {
  if (!form.value.fromWarehouseId || !form.value.toWarehouseId) {
    uni.showToast({ title: '请选择仓库', icon: 'none' }); return
  }
  if (form.value.fromWarehouseId === form.value.toWarehouseId) {
    uni.showToast({ title: '调出和调入不能相同', icon: 'none' }); return
  }
  if (form.value.items.length === 0) {
    uni.showToast({ title: '请添加商品', icon: 'none' }); return
  }
  submitting.value = true
  try {
    const res = await request.post('/transfer', form.value)
    await request.put(`/transfer/${res.data}/submit`)
    uni.showToast({ title: '调拨成功', icon: 'success' })
    uni.navigateBack()
  } finally { submitting.value = false }
}

onMounted(fetchBase)
</script>

<template>
  <view class="page">
    <view class="section">
      <view class="form-item">
        <text class="label">调出仓库 *</text>
        <picker @change="e => form.fromWarehouseId = warehouses[e.detail.value]?.id" :range="warehouses" range-key="name">
          <view class="picker">{{ warehouses.find(w => w.id === form.fromWarehouseId)?.name || '请选择' }}</view>
        </picker>
      </view>
      <view class="form-item">
        <text class="label">调入仓库 *</text>
        <picker @change="e => form.toWarehouseId = warehouses[e.detail.value]?.id" :range="warehouses" range-key="name">
          <view class="picker">{{ warehouses.find(w => w.id === form.toWarehouseId)?.name || '请选择' }}</view>
        </picker>
      </view>
    </view>

    <view class="section">
      <view class="section-header">
        <text class="section-title">调拨商品</text>
        <text class="add-link" @click="addItem">+ 添加</text>
      </view>
      <view v-for="(item, index) in form.items" :key="index" class="item-card">
        <view class="item-header">
          <text>商品 {{ index + 1 }}</text>
          <text class="del-link" @click="removeItem(index)">删除</text>
        </view>
        <view class="ic-row">
          <picker @change="e => { item.productId = products[e.detail.value]?.id }" :range="products" range-key="name" style="flex:1">
            <view class="picker">{{ products.find(p => p.id === item.productId)?.name || '选择商品' }}</view>
          </picker>
          <view class="scan-btn" @click="scanCode(index)">📷</view>
        </view>
        <view class="item-row">
          <view class="item-field"><text class="label-sm">数量</text><input v-model="item.quantity" class="input-sm" type="number" /></view>
          <view class="item-field"><text class="label-sm">批次号</text><input v-model="item.batchNo" class="input-sm" placeholder="选填" /></view>
        </view>
      </view>
    </view>

    <button class="submit-btn" :loading="submitting" @click="handleSubmit">确认调拨</button>
  </view>
</template>

<style scoped>
.page { padding: 16px; padding-bottom: 80px; background: #f5f7f5; min-height: 100vh; }
.section { background: #fff; border-radius: 8px; padding: 16px; margin-bottom: 12px; }
.form-item { margin-bottom: 12px; }
.label { display: block; font-size: 14px; color: #333; margin-bottom: 4px; }
.picker { border: 1px solid #dcdfe6; border-radius: 6px; padding: 10px 12px; font-size: 14px; color: #333; background: #fff; }
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.section-title { font-weight: bold; }
.add-link { color: #2e7d32; font-size: 14px; }
.item-card { background: #f8f9fa; border-radius: 6px; padding: 12px; margin-bottom: 8px; }
.item-header { display: flex; justify-content: space-between; margin-bottom: 8px; font-weight: bold; }
.del-link { color: #ff4d4f; }
.ic-row { display: flex; gap: 8px; margin-bottom: 8px; }
.scan-btn { width: 40px; height: 40px; background: #e8f5e9; border-radius: 6px; display: flex; align-items: center; justify-content: center; font-size: 20px; }
.item-row { display: flex; gap: 8px; }
.item-field { flex: 1; }
.label-sm { font-size: 12px; color: #666; }
.input-sm { border: 1px solid #dcdfe6; border-radius: 4px; padding: 6px 8px; font-size: 13px; width: 100%; box-sizing: border-box; }
.submit-btn { position: fixed; bottom: 0; left: 0; right: 0; margin: 16px; background: #2e7d32; color: #fff; border: none; border-radius: 8px; height: 44px; line-height: 44px; font-size: 16px; }
</style>
