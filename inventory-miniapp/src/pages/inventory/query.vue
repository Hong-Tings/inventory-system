<script setup>
import { ref, reactive, computed } from 'vue'
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app'
import request from '@/api/request'

const loading = ref(false)
const warehouseTree = ref([])
const allList = ref([])
const keyword = ref('')

// 级联仓库选择
const whCascade = ref([])
const whOptions = ref([])
const showWhPicker = ref(false)
const warehouseId = ref(null)

// 展开状态
const expanded = reactive(new Set())

function toggle(id) { expanded.has(id) ? expanded.delete(id) : expanded.add(id) }

// 库存按仓库分组
const invByWh = computed(() => {
  const m = new Map()
  for (const item of allList.value) {
    const wid = item.warehouseId
    if (!m.has(wid)) m.set(wid, [])
    m.get(wid).push(item)
  }
  return m
})

// 构建仓库路径映射：warehouseId → 完整路径字符串
const whPathMap = computed(() => {
  const m = new Map()
  function walk(nodes, parentPath) {
    for (const n of nodes) {
      const path = parentPath ? parentPath + ' > ' + n.name : n.name
      m.set(n.id, path)
      if (n.children?.length) walk(n.children, path)
    }
  }
  walk(warehouseTree.value, '')
  return m
})

// 搜索模式：按商品分组的库存列表
const searchResults = computed(() => {
  if (!keyword.value || !allList.value.length) return []
  const groups = new Map()
  for (const inv of allList.value) {
    const pid = inv.productId
    if (!groups.has(pid)) groups.set(pid, { productId: pid, productName: inv.productName || '', productCode: inv.productCode || '', items: [] })
    groups.get(pid).items.push({ ...inv, _path: whPathMap.value.get(inv.warehouseId) || inv.warehouseName || '' })
  }
  return [...groups.values()]
})
function buildNode(n, depth) {
  const invs = invByWh.value.get(n.id) || []
  const qty = invs.reduce((s, i) => s + (i.quantity || 0), 0)
  const amt = invs.reduce((s, i) => s + ((i.costPrice || 0) * (i.quantity || 0)), 0)
  const node = { ...n, _depth: depth, _pc: invs.length, _qty: qty, _amt: amt, _invs: invs }
  if (n.children?.length) node.children = n.children.map(c => buildNode(c, depth + 1))
  return node
}

// 选了仓库时只显示该节点子树
function pruneTree(nodes, targetId) {
  for (const n of nodes) {
    if (n.id === targetId) return [n]
    if (n.children?.length) { const f = pruneTree(n.children, targetId); if (f) return f }
  }
  return null
}
const displayTree = computed(() => {
  const tree = warehouseTree.value
  if (!warehouseId.value) return tree
  return pruneTree(tree, warehouseId.value) || tree
})

// 搜索时过滤掉无匹配库存的仓库节点
function filterEmptyNodes(nodes) {
  if (!keyword.value) return nodes
  return nodes.filter(n => {
    const hasInv = invByWh.value.has(n.id) && invByWh.value.get(n.id).length > 0
    const childMatch = n.children?.length ? filterEmptyNodes(n.children) : []
    if (childMatch.length) n.children = childMatch
    return hasInv || childMatch.length
  })
}

const flatList = computed(() => {
  function flatten(nodes) {
    const r = []
    for (const n of nodes) {
      r.push(n)
      if (n.children?.length && expanded.has(n.id)) r.push(...flatten(n.children))
    }
    return r
  }
  return flatten(filterEmptyNodes(displayTree.value.map(n => buildNode(n, 0))))
})

async function fetchData() {
  loading.value = true
  try {
    const params = { page: 1, size: 999 }
    if (warehouseId.value) params.warehouseId = warehouseId.value
    if (keyword.value) params.productName = keyword.value
    const res = await request.get('/inventory/page', { params })
    allList.value = res.data.records || []
  } finally { loading.value = false }
}

async function fetchWarehouseTree() {
  const res = await request.get('/warehouse/tree')
  warehouseTree.value = res.data || []
}

function onSearch() { fetchData() }
function handleReset() { keyword.value = ''; warehouseId.value = null; expanded.clear(); fetchData() }

// 级联
function openWarehousePicker() { whCascade.value = []; whOptions.value = warehouseTree.value || []; showWhPicker.value = true }
function selectWhLevel(item) {
  whCascade.value.push(item)
  if (item.children?.length) { whOptions.value = item.children }
  else { warehouseId.value = item.id; showWhPicker.value = false; fetchData() }
}
function goBackTo(index) {
  whCascade.value = whCascade.value.slice(0, index + 1)
  const parent = whCascade.value.length ? whCascade.value[whCascade.value.length - 1] : null
  whOptions.value = parent ? (parent.children || []) : (warehouseTree.value || [])
}
const whLabel = computed(() => {
  if (!warehouseId.value) return '全部仓库'
  function find(nodes, id) { for (const n of nodes) { if (n.id === id) return n.name; if (n.children) { const r = find(n.children, id); if (r) return r } } return null }
  return find(warehouseTree.value, warehouseId.value) || '全部仓库'
})

// 合计
const grandTotal = computed(() => {
  const qty = allList.value.reduce((s, i) => s + (i.quantity || 0), 0)
  const amt = allList.value.reduce((s, i) => s + ((i.costPrice || 0) * (i.quantity || 0)), 0)
  return { qty, amt }
})

onShow(() => {
  if (!warehouseTree.value.length) fetchWarehouseTree()
  fetchData()
})
onPullDownRefresh(() => { fetchData(); uni.stopPullDownRefresh() })
</script>

<template>
  <view class="page">
    <view class="page-bar">
      <text class="page-title">库存查询</text>
      <text class="nav-link" @click="uni.navigateTo({ url: '/pages/inventory/log' })">流水 ›</text>
    </view>
    <view class="search-bar" style="display:flex;gap:6px;flex-wrap:wrap;">
      <view style="display:flex;gap:6px;flex:1;min-width:200px;">
        <input v-model="keyword" class="search-input" placeholder="商品名称/编码" style="flex:1;" @confirm="onSearch" />
        <view class="btn-search" @click="onSearch">搜索</view>
        <view class="btn-reset" @click="handleReset">重置</view>
      </view>
      <view class="filter-pill picker-select" @click="openWarehousePicker">{{ whLabel }}</view>
      <view v-if="showWhPicker" class="picker-overlay" @click="showWhPicker = false">
        <view class="picker-modal" @click.stop>
          <view class="picker-header">
            <text class="picker-cancel" @click="showWhPicker = false">取消</text>
            <text style="font-weight:bold;">选择仓库</text>
            <view style="width:40px;"></view>
          </view>
          <view v-if="whCascade.length" class="wh-breadcrumb">
            <text v-for="(c, i) in whCascade" :key="i" class="wh-crumb" @click="goBackTo(i)">{{ c.name }}<text v-if="i < whCascade.length - 1"> ›</text></text>
          </view>
          <scroll-view scroll-y class="picker-list">
            <view v-for="item in whOptions" :key="item.id" class="picker-item" @click="selectWhLevel(item)">
              <text :style="{ fontWeight: item.children?.length ? 'bold' : 'normal' }">{{ item.name }}</text>
              <text style="font-size:11px;color:#999;margin-left:4px;">{{ item.level }}级</text>
              <text v-if="item.children?.length" style="margin-left:auto;color:#ccc;">›</text>
            </view>
            <view v-if="!whOptions.length" style="text-align:center;padding:30px 0;color:#999;">无下级仓库</view>
          </scroll-view>
        </view>
      </view>
    </view>

    <view v-if="loading" class="loading">加载中...</view>
    <view v-else-if="keyword" class="result-wrap">
      <!-- 搜索模式：按商品分组显示 -->
      <view v-for="group in searchResults" :key="group.productId" class="prod-group">
        <view class="prod-group-header">
          <text class="prod-group-name">{{ group.productName }}</text>
          <text v-if="group.productCode" class="prod-group-code">{{ group.productCode }}</text>
          <text class="prod-group-count">{{ group.items.length }}仓</text>
        </view>
        <view v-for="inv in group.items" :key="inv.id || inv.productId" class="inv-card">
          <view class="inv-path">{{ inv._path }}</view>
          <view class="inv-row">
            <view class="inv-info">
              <text class="inv-qty" :class="{ low: (inv.quantity || 0) <= 5 }">{{ inv.quantity }}</text>
              <text>批次: {{ inv.batchNo || '-' }}</text>
            </view>
            <text>仓码: {{ inv.warehouseCode || '-' }}</text>
            <text>均价 ¥{{ (inv.costPrice || 0).toFixed(2) }}</text>
            <text>¥{{ ((inv.costPrice || 0) * (inv.quantity || 0)).toFixed(2) }}</text>
          </view>
        </view>
      </view>
      <view v-if="!searchResults.length && !loading" class="empty">未找到匹配商品</view>
      <view v-if="searchResults.length" class="grand-total">总计 {{ grandTotal.qty }} 件 · ¥{{ grandTotal.amt.toFixed(2) }}</view>
    </view>
    <view v-else class="tree-wrap">
      <!-- 树形模式 -->
      <view v-for="node in flatList" :key="node.id" class="tree-row" :style="{ paddingLeft: (node._depth * 20 + 12) + 'px' }">
        <view v-if="!node._invs?.length || node.children?.length" class="node-row" @click="node.children?.length && toggle(node.id)">
          <text class="toggle-icon">{{ node.children?.length ? (expanded.has(node.id) ? '▼' : '▶') : '  ' }}</text>
          <text class="node-name" :class="'lv' + node.level">{{ node.name }}</text>
          <text class="level-tag">{{ node.level }}级</text>
          <text v-if="node._pc" class="node-stats">{{ node._pc }}种 · {{ node._qty }}件</text>
        </view>
        <view v-if="!node.children?.length && node._invs?.length">
          <view class="node-row leaf-header" @click="toggle(node.id)">
            <text class="toggle-icon">{{ expanded.has(node.id) ? '▼' : '▶' }}</text>
            <text class="node-name lv4">{{ node.name }}</text>
            <text class="level-tag">{{ node.level }}级</text>
            <text v-if="node.code" style="font-size:11px;color:#999;">{{ node.code }}</text>
            <text class="node-stats">{{ node._pc }}种 · {{ node._qty }}件</text>
          </view>
          <view v-if="expanded.has(node.id)" class="invs-wrap">
            <view v-for="inv in node._invs" :key="inv.productId" class="inv-card">
              <view class="inv-row">
                <view class="inv-info">
                  <text class="inv-name">{{ inv.productName }}</text>
                  <text class="inv-code">{{ inv.productCode }}</text>
                </view>
                <text class="inv-qty" :class="{ low: (inv.quantity || 0) <= 5 }">{{ inv.quantity }}</text>
              </view>
              <view class="inv-footer">
                <text>批次: {{ inv.batchNo || '-' }}</text>
                <text>仓码: {{ inv.warehouseCode || '-' }}</text>
                <text>均价 ¥{{ (inv.costPrice || 0).toFixed(2) }}</text>
                <text>¥{{ ((inv.costPrice || 0) * (inv.quantity || 0)).toFixed(2) }}</text>
              </view>
            </view>
          </view>
        </view>
      </view>
      <view v-if="!flatList.length && !loading" class="empty">暂无库存数据</view>
      <view v-if="flatList.length" class="grand-total">总计 {{ grandTotal.qty }} 件 · ¥{{ grandTotal.amt.toFixed(2) }}</view>
    </view>
  </view>
</template>

<style scoped>
.page { padding: 16px; }
.page-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.page-title { font-size: 18px; font-weight: bold; }
.nav-link { color: #2e7d32; font-size: 14px; }
.search-input { border: 1px solid #dcdfe6; border-radius: 10px; padding: 12px 14px; font-size: 13px; background: #fff; }
.filter-pill { background: #fff; border-radius: 10px; padding: 12px 14px; font-size: 13px; white-space: nowrap; box-shadow: 0 1px 4px rgba(0,0,0,0.04); }
.btn-search { background: #2e7d32; color: #fff; border-radius: 10px; padding: 12px 16px; font-size: 13px; white-space: nowrap; }
.btn-reset { background: #f5f5f5; color: #666; border-radius: 10px; padding: 12px 16px; font-size: 13px; white-space: nowrap; }
.loading, .empty { text-align: center; color: #999; padding: 40px 0; }
.tree-wrap { background: #fff; border-radius: 8px; overflow: hidden; }
.tree-row { border-bottom: 1px solid #f5f5f5; }
.tree-row:last-child { border-bottom: none; }
.node-row { display: flex; align-items: center; gap: 6px; padding: 12px 0; min-height: 44px; }
.toggle-icon { font-size: 10px; color: #999; width: 14px; text-align: center; flex-shrink: 0; }
.node-name { font-weight: 600; font-size: 14px; color: #1a1a1a; }
.node-name.lv1 { font-size: 15px; }
.level-tag { font-size: 10px; padding: 2px 6px; border-radius: 3px; background: #f0f4f0; color: #666; flex-shrink: 0; }
.node-stats { font-size: 11px; color: #999; margin-left: auto; white-space: nowrap; }
.leaf-header { cursor: pointer; }
.invs-wrap { padding: 0 8px 8px; }
.inv-card { background: #f8f9fa; border-radius: 6px; padding: 10px 12px; margin-bottom: 6px; }
.inv-row { display: flex; justify-content: space-between; align-items: center; }
.inv-info { flex: 1; min-width: 0; }
.inv-name { font-size: 14px; font-weight: 500; }
.inv-code { font-size: 11px; color: #999; margin-left: 6px; }
.inv-qty { font-size: 20px; font-weight: 700; color: #2e7d32; }
.inv-qty.low { color: #c62828; }
.inv-footer { display: flex; gap: 12px; margin-top: 6px; font-size: 11px; color: #aaa; }
.grand-total { text-align: right; padding: 14px 16px; font-size: 14px; font-weight: 600; color: #303133; }
.result-wrap { padding-bottom: 4px; }
.prod-group { margin-bottom: 12px; }
.prod-group-header { display: flex; align-items: center; gap: 8px; padding: 10px 12px; background: #2e7d32; border-radius: 6px 6px 0 0; }
.prod-group-name { font-size: 14px; font-weight: 600; color: #fff; }
.prod-group-code { font-size: 11px; color: #c8e6c9; }
.prod-group-count { font-size: 11px; color: #c8e6c9; margin-left: auto; white-space: nowrap; }
.inv-path { font-size: 11px; color: #2e7d32; margin-bottom: 4px; padding: 2px 6px; background: #e8f5e9; border-radius: 3px; display: inline-block; }
.picker-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.4); z-index: 999; display: flex; align-items: flex-end; }
.picker-modal { background: #fff; border-radius: 16px 16px 0 0; width: 100%; max-height: 70vh; display: flex; flex-direction: column; }
.picker-header { display: flex; align-items: center; justify-content: space-between; padding: 16px; border-bottom: 1px solid #eee; }
.picker-cancel { color: #666; font-size: 14px; }
.wh-breadcrumb { display: flex; flex-wrap: wrap; gap: 4px; padding: 10px 16px; background: #f5f7fa; font-size: 13px; }
.wh-crumb { color: #2e7d32; }
.picker-list { padding: 0 16px 20px; max-height: 55vh; overflow-y: auto; }
.picker-item { display: flex; align-items: center; gap: 8px; padding: 12px 0; border-bottom: 1px solid #f5f5f5; }
.picker-item:active { background: #f5f7f5; }
</style>
