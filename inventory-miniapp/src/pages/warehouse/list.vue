<script setup>
import { ref, reactive, computed } from 'vue'
import { onShow, onPullDownRefresh } from '@dcloudio/uni-app'
import request from '@/api/request'
import FloatingHome from '@/components/FloatingHome'

const loading = ref(false)
const warehouseTree = ref([])
const keyword = ref('')
const expanded = reactive(new Set())

function toggle(id) { expanded.has(id) ? expanded.delete(id) : expanded.add(id) }
function goEdit(id) { uni.navigateTo({ url: `/pages/warehouse/edit?id=${id}` }) }
function goCreate() { uni.navigateTo({ url: '/pages/warehouse/edit' }) }

function pruneTree(nodes, kw) {
  if (!kw) return nodes
  const lower = kw.toLowerCase()
  function filter(ns) {
    return ns.filter(n => {
      const match = n.name?.toLowerCase().includes(lower) || n.code?.toLowerCase().includes(lower)
      const childMatch = n.children?.length ? filter(n.children) : []
      if (childMatch.length) n.children = childMatch
      return match || childMatch.length
    })
  }
  return filter(JSON.parse(JSON.stringify(nodes)))
}

const flatList = computed(() => {
  function buildNodes(nodes, depth) {
    const r = []
    for (const n of nodes) {
      r.push({ ...n, _depth: depth })
      if (n.children?.length && expanded.has(n.id)) {
        r.push(...buildNodes(n.children, depth + 1))
      }
    }
    return r
  }
  return buildNodes(pruneTree(warehouseTree.value, keyword.value), 0)
})

async function fetchData() {
  loading.value = true
  try {
    const res = await request.get('/warehouse/tree')
    warehouseTree.value = res.data || []
  } finally { loading.value = false }
}

function onSearch() {}
function onKeywordInput(e) { if (!e.detail.value) keyword.value = '' }

onShow(fetchData)
onPullDownRefresh(() => { fetchData(); uni.stopPullDownRefresh() })
</script>

<template>
  <view class="page">
    <view class="page-bar">
      <text class="page-title">仓库管理</text>
      <button class="add-btn" @click="goCreate">+ 新建</button>
    </view>
    <view class="search-bar" style="display:flex;gap:6px;">
      <input v-model="keyword" class="search-input" placeholder="搜索名称或编码" style="flex:1;" @confirm="onSearch" @input="onKeywordInput" />
      <view class="btn-reset" @click="keyword = ''">重置</view>
    </view>

    <view v-if="loading" class="loading">加载中...</view>
    <view v-else class="tree-wrap">
      <view v-for="node in flatList" :key="node.id" class="tree-row"
        :style="{ paddingLeft: (node._depth * 20 + 12) + 'px' }"
        @click="node.children?.length ? toggle(node.id) : goEdit(node.id)">
        <view class="node-row">
          <text class="toggle-icon">{{ node.children?.length ? (expanded.has(node.id) ? '▼' : '▶') : '  ' }}</text>
          <text class="node-name" :class="'lv' + node.level">{{ node.name }}</text>
          <text class="level-tag" :class="'level-' + node.level">{{ node.level }}级</text>
          <text v-if="node.code" class="node-code">{{ node.code }}</text>
          <text class="node-status" :class="node.status === 1 ? 'on' : 'off'">{{ node.status === 1 ? '启用' : '停用' }}</text>
        </view>
        <view v-if="node.children?.length" class="node-meta" :style="{ paddingLeft: '20px' }">
          <text>子仓 {{ node.children.length }} 个</text>
        </view>
        <view v-else class="node-meta" :style="{ paddingLeft: '20px' }">
          <text v-if="node.contact">{{ node.contact }}</text>
          <text v-if="node.phone">{{ node.phone }}</text>
          <text v-if="node.address" class="addr">{{ node.address }}</text>
        </view>
      </view>
      <view v-if="!flatList.length && !loading" class="empty">暂无仓库</view>
    </view>
    <FloatingHome />
  </view>
</template>

<style scoped>
.page { padding: 16px; }
.page-bar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.page-title { font-size: 18px; font-weight: bold; }
.add-btn { background: #2e7d32; color: #fff; border: none; border-radius: 6px; padding: 8px 16px; font-size: 14px; }
.search-bar { margin-bottom: 12px; }
.search-input { border: 1px solid #dcdfe6; border-radius: 10px; padding: 12px 14px; font-size: 13px; background: #fff; }
.btn-reset { background: #f5f5f5; color: #666; border-radius: 10px; padding: 0 16px; font-size: 13px; display: flex; align-items: center; white-space: nowrap; }
.loading, .empty { text-align: center; color: #999; padding: 40px 0; }

.tree-wrap { background: #fff; border-radius: 8px; overflow: hidden; box-shadow: 0 1px 4px rgba(0,0,0,0.08); }
.tree-row { padding: 8px 12px; border-bottom: 1px solid #f5f5f5; }
.tree-row:last-child { border-bottom: none; }
.tree-row:active { background: #f5f7f5; }
.node-row { display: flex; align-items: center; gap: 6px; min-height: 36px; }
.toggle-icon { font-size: 10px; color: #999; width: 14px; text-align: center; flex-shrink: 0; }
.node-name { font-weight: 600; font-size: 14px; color: #1a1a1a; }
.node-name.lv1 { font-size: 16px; color: #1b5e20; }
.node-name.lv2 { font-size: 15px; color: #2e7d32; }
.node-name.lv3 { font-size: 14px; }
.node-name.lv4 { font-size: 13px; color: #555; }
.level-tag { font-size: 10px; padding: 1px 6px; border-radius: 3px; background: #f0f4f0; color: #666; flex-shrink: 0; }
.level-tag.level-1 { background: #e8f5e9; color: #1b5e20; }
.level-tag.level-2 { background: #e3f2fd; color: #1565c0; }
.level-tag.level-3 { background: #fff3e0; color: #e65100; }
.level-tag.level-4 { background: #fce4ec; color: #c62828; }
.node-code { font-size: 11px; color: #999; }
.node-status { font-size: 11px; padding: 1px 6px; border-radius: 3px; }
.node-status.on { background: #e8f5e9; color: #2e7d32; }
.node-status.off { background: #f5f5f5; color: #999; }
.node-meta { display: flex; gap: 12px; font-size: 11px; color: #999; margin-top: 2px; }
.node-meta .addr { max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
</style>
