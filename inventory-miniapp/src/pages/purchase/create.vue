<script setup>
import { ref, onMounted, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import request from '@/api/request'
import FloatingHome from '@/components/FloatingHome'

const editingId = ref(null)
const suppliers = ref([])
const products = ref([])
const warehouseStock = ref({})
const submitting = ref(false)
const showPicker = ref(false)
const pickerIndex = ref(0)
const searchKeyword = ref('')
const remarkFocused = ref(false)
const searchFocused = ref(false)

// 级联仓库选择
const warehouseTree = ref([])
const whCascade = ref([])     // 当前级联路径
const whStep = ref(0)                 // 0=未选, 1=选1级, 2=选2级...
const whOptions = ref([])      // 当前可选项
const showWhPicker = ref(false)

const form = ref({
  supplierId: null, warehouseId: null,
  orderDate: new Date().toISOString().slice(0, 10),
  remark: '', items: [],
})

onLoad((options) => {
  if (options?.id) editingId.value = Number(options.id)
})

onMounted(async () => {
  const [sRes, tRes, pRes] = await Promise.all([
    request.get('/supplier/list'), request.get('/warehouse/tree'), request.get('/product/list'),
  ])
  suppliers.value = sRes.data; warehouseTree.value = tRes.data || []; products.value = pRes.data

  if (editingId.value) {
    const res = await request.get(`/purchase-order/${editingId.value}`)
    const data = res.data
    form.value.supplierId = data.supplierId
    form.value.warehouseId = data.warehouseId
    form.value.orderDate = data.orderDate
    form.value.remark = data.remark || ''
    form.value.items = (data.items || []).map(i => ({
      productId: i.productId, productName: i.productName, spec: i.spec || '',
      quantity: i.quantity, unitPrice: i.unitPrice, amount: i.amount, batchNo: i.batchNo || '',
    }))
    if (data.warehouseId) loadStock(data.warehouseId)
  }
})

// 打开仓库选择器
function openWarehousePicker() {
  whCascade.value = []
  whStep.value = 1
  whOptions.value = warehouseTree.value || []
  showWhPicker.value = true
}
function selectWhLevel(item) {
  whCascade.value.push(item)
  if (item.children?.length) {
    whStep.value++
    whOptions.value = item.children
  } else {
    // 叶子节点，确认选择
    form.value.warehouseId = item.id
    showWhPicker.value = false
    // 加载库存
    loadStock(item.id)
  }
}
function goBackTo(index) {
  whCascade.value = whCascade.value.slice(0, index + 1)
  const parent = whCascade.value.length ? whCascade.value[whCascade.value.length - 1] : null
  whOptions.value = parent ? (parent.children || []) : (warehouseTree.value || [])
  whStep.value = index + 2
}
function goBackWhLevel() {
  whCascade.value.pop()
  const parent = whCascade.value.length ? whCascade.value[whCascade.value.length - 1] : null
  whOptions.value = parent ? (parent.children || []) : (warehouseTree.value || [])
  whStep.value--
}

// 仓库路径显示
const whDisplay = computed(() => {
  if (!form.value.warehouseId) return '请选择'
  // 从树中递归查找仓库名称
  function find(nodes, id) {
    for (const n of nodes) {
      if (n.id === id) return n.name
      if (n.children) { const r = find(n.children, id); if (r) return r }
    }
    return null
  }
  return find(warehouseTree.value, form.value.warehouseId) || '仓库' + form.value.warehouseId
})

async function loadStock(id) {
  warehouseStock.value = {}
  try {
    const res = await request.get('/inventory/page', { params: { warehouseId: id, page: 1, size: 999 } })
    const stock = {}
    for (const r of res.data.records || []) stock[r.productId] = (stock[r.productId] || 0) + (r.quantity || 0)
    warehouseStock.value = stock
  } catch { /* ignore */ }
}

const sortedProducts = computed(() => {
  const withStock = [], without = []
  for (const p of products.value) {
    const s = warehouseStock.value[p.id] || 0
    ;(s > 0 ? withStock : without).push({ ...p, stock: s })
  }
  withStock.sort((a, b) => b.stock - a.stock)
  return [...withStock, ...without]
})

const filteredProducts = computed(() => {
  const kw = searchKeyword.value.trim().toLowerCase()
  if (!kw) return sortedProducts.value
  return sortedProducts.value.filter(p =>
    p.name.toLowerCase().includes(kw) || (p.code && p.code.toLowerCase().includes(kw)))
})

function openPicker(idx) { pickerIndex.value = idx; searchKeyword.value = ''; showPicker.value = true }

function selectProduct(p) {
  const items = form.value.items
  const item = items[pickerIndex.value]
  if (!item) return
  item.productId = p.id; item.productName = p.name; item.spec = p.spec || ''
  if (!item.unitPrice) item.unitPrice = p.purchasePrice
  calcAmount(item)
  showPicker.value = false
}

function addItem() {
  form.value.items.push({ productId: null, productName: '', spec: '', quantity: 1, unitPrice: null, amount: null, batchNo: '' })
}

function removeItem(index) { form.value.items.splice(index, 1) }

function scanCode(index) {
  uni.scanCode({
    success: (res) => {
      const p = products.value.find(x => x.code === res.result)
      if (!p) { uni.showToast({ title: '未找到该商品', icon: 'none' }); return }
      const item = form.value.items[index]
      if (!item) return
      item.productId = p.id; item.productName = p.name; item.spec = p.spec || ''
      if (!item.unitPrice) item.unitPrice = p.purchasePrice
      calcAmount(item)
    },
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
    if (editingId.value) {
      await request.put(`/purchase-order/${editingId.value}/draft`, form.value)
      await request.put(`/purchase-order/${editingId.value}/submit`)
    } else {
      const res = await request.post('/purchase-order', form.value)
      await request.put(`/purchase-order/${res.data}/submit`)
    }
    uni.showToast({ title: '已提交审批', icon: 'success' })
    setTimeout(() => uni.switchTab({ url: '/pages/purchase/list' }), 300)
  } finally { submitting.value = false }
}
</script>

<template>
  <view class="page">
    <view class="section">
      <view class="form-item">
        <text class="label">供应商</text>
        <picker @change="e => form.supplierId = suppliers[e.detail.value]?.id" :range="suppliers" range-key="name">
          <view class="picker">{{ suppliers.find(s => s.id === form.supplierId)?.name || '请选择' }}</view>
        </picker>
      </view>
      <view class="form-item">
        <text class="label">仓库 *</text>
        <view class="picker picker-select" @click="openWarehousePicker">
          <text>{{ whDisplay }}</text>
          <text v-if="!form.warehouseId" style="color:#bbb;">请选择</text>
        </view>

        <!-- 级联仓库选择弹窗 -->
        <view v-if="showWhPicker" class="picker-overlay" @click="showWhPicker = false">
          <view class="picker-modal" @click.stop>
            <view class="picker-header">
              <text class="picker-cancel" @click="showWhPicker = false">取消</text>
              <text class="picker-title" style="font-weight:bold;">选择仓库</text>
              <view style="width:40px;"></view>
            </view>
            <view v-if="whCascade.length" class="wh-breadcrumb">
              <text v-for="(c, i) in whCascade" :key="i" class="wh-crumb" @click="goBackTo(i)">
                {{ c.name }}<text v-if="i < whCascade.length - 1" style="margin:0 4px;">›</text>
              </text>
            </view>
            <scroll-view scroll-y class="picker-list">
              <view v-for="item in whOptions" :key="item.id" class="picker-item" @click="selectWhLevel(item)">
                <text :style="{ fontWeight: item.children?.length ? 'bold' : 'normal' }">{{ item.name }}</text>
                <text style="font-size:11px;color:#999;">{{ item.level }}级</text>
                <text v-if="item.children?.length" style="margin-left:auto;color:#ccc;">›</text>
              </view>
              <view v-if="!whOptions.length" style="text-align:center;padding:30px 0;color:#999;">无下级仓库</view>
            </scroll-view>
          </view>
        </view>
      </view>
      <view class="form-item">
        <text class="label">备注</text>
        <input v-model="form.remark" class="input" :class="{ focused: remarkFocused }" @focus="remarkFocused = true" @blur="remarkFocused = false" placeholder="备注信息" />
      </view>
    </view>

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
          <view class="picker picker-select" @click="openPicker(index)" style="flex:1;overflow:hidden;">
            {{ item.productName || '选择商品' }}
          </view>
          <view class="scan-btn" @click="scanCode(index)">📷</view>
        </view>
        <view v-if="item.productId" class="selected-info">
          <text>规格: {{ item.spec || '-' }}</text>
          <text>仓库库存: {{ warehouseStock[item.productId] ?? '-' }}</text>
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
      </view>
    </view>

    <!-- 商品选择弹窗 -->
    <view v-if="showPicker" class="picker-overlay" @click="showPicker = false">
      <view class="picker-modal" @click.stop>
        <view class="picker-search">
          <input v-model="searchKeyword" class="search-input" :class="{ focused: searchFocused }" @focus="searchFocused = true" @blur="searchFocused = false" placeholder="搜索商品名称或编码" focus />
        </view>
        <scroll-view scroll-y class="picker-list">
          <view v-for="p in filteredProducts" :key="p.id" class="picker-item" @click="selectProduct(p)">
            <view style="flex:1;overflow:hidden;">
              <text style="font-size:14px;font-weight:500;">{{ p.name }}</text>
              <text style="font-size:11px;color:#999;margin-left:4px;">{{ p.code }}</text>
              <text style="font-size:11px;color:#666;display:block;">{{ p.spec || '-' }} | 采购价 ¥{{ p.purchasePrice }}</text>
            </view>
            <view class="stock-badge" :class="{ 'has-stock': (warehouseStock[p.id] || 0) > 0 }">
              库存 {{ warehouseStock[p.id] ?? '-' }}
            </view>
          </view>
          <view v-if="filteredProducts.length === 0" style="text-align:center;padding:20px;color:#999;">无匹配商品</view>
        </scroll-view>
      </view>
    </view>

    <button class="submit-btn" :loading="submitting" @click="handleSubmit">{{ editingId ? '保存并提交' : '确认入库' }}</button>
    <FloatingHome />
  </view>
</template>

<style scoped>
.page { padding: 16px; padding-bottom: 80px; }
.section { background: #fff; border-radius: 8px; padding: 16px; margin-bottom: 12px; }
.form-item { margin-bottom: 12px; }
.label { display: block; font-size: 14px; color: #333; margin-bottom: 4px; }
.picker { border: 1px solid #dcdfe6; border-radius: 6px; padding: 14px 16px; font-size: 14px; color: #333; background: #fff; }
.input { border: 1px solid #dcdfe6; border-radius: 4px; padding: 10px 12px; }
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.section-title { font-weight: bold; }
.add-link { color: #2e7d32; font-size: 14px; }
.item-card { background: #f8f9fa; border-radius: 6px; padding: 12px; margin-bottom: 8px; }
.item-header { display: flex; justify-content: space-between; margin-bottom: 8px; font-weight: bold; }
.del-link { color: #ff4d4f; }
.selected-info { display: flex; gap: 16px; font-size: 11px; color: #666; margin: 6px 0 2px; padding: 4px 8px; background: #e8f5e9; border-radius: 4px; }
.item-row { display: flex; gap: 8px; margin-top: 8px; }
.item-field { flex: 1; }
.label-sm { font-size: 12px; color: #666; }
.input-sm { border: 1px solid #dcdfe6; border-radius: 4px; padding: 10px 12px; }
.input.focused { border-color: #2e7d32; border-width: 1.5px; }
.picker-search .search-input.focused { border-color: #2e7d32; border-width: 1.5px; }
.picker-header { display: flex; align-items: center; justify-content: space-between; padding: 16px; border-bottom: 1px solid #eee; }
.picker-cancel { color: #666; font-size: 14px; }
.picker-title { font-size: 16px; }
.wh-breadcrumb { display: flex; flex-wrap: wrap; gap: 4px; padding: 10px 16px; background: #f5f7fa; font-size: 13px; }
.wh-crumb { color: #2e7d32; }
.amount { font-size: 14px; color: #f56c6c; font-weight: bold; }
.submit-btn { position: fixed; bottom: 0; left: 0; right: 0; margin: 16px; background: #2e7d32; color: #fff; border: none; border-radius: 8px; height: 44px; line-height: 44px; font-size: 16px; }
.ic-row { display: flex; align-items: center; gap: 8px; }
.scan-btn { width: 40px; height: 40px; background: #e8f5e9; border-radius: 6px; display: flex; align-items: center; justify-content: center; font-size: 20px; flex-shrink: 0; }
.picker-select:active { background: #e8f5e9; }
.picker-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.4); z-index: 999; display: flex; align-items: flex-end; }
.picker-modal { background: #fff; border-radius: 16px 16px 0 0; width: 100%; max-height: 70vh; display: flex; flex-direction: column; }
.picker-search { padding: 16px 16px 8px; }
.picker-search .search-input {
  border: 1px solid #dcdfe6; border-radius: 4px; padding: 10px 12px; }
.picker-list { padding: 0 16px 20px; max-height: 55vh; overflow-y: auto; }
.picker-item { display: flex; align-items: center; gap: 8px; padding: 12px 0; border-bottom: 1px solid #f5f5f5; }
.picker-item:active { background: #f5f7f5; }
.stock-badge { font-size: 11px; padding: 3px 8px; border-radius: 4px; background: #f5f5f5; color: #999; white-space: nowrap; flex-shrink: 0; }
.stock-badge.has-stock { background: #e8f5e9; color: #2e7d32; }
</style>
