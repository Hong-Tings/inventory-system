<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import request, { downloadFile } from '../../api/request'
import type { Inventory } from '../../types/api'

const router = useRouter()

const loading = ref(false)
const allList = ref<Inventory[]>([])
const warehouseTree = ref<any[]>([])

const query = ref({ productName: '', warehouseId: undefined as number | undefined })

async function fetchData() {
  loading.value = true
  try {
    const params: Record<string, any> = { page: 1, size: 999 }
    if (query.value.productName) params.productName = query.value.productName
    if (query.value.warehouseId !== undefined) params.warehouseId = query.value.warehouseId
    const res = await request.get('/inventory/page', { params })
    allList.value = res.data.data.records || []
  } finally { loading.value = false }
}

async function fetchWarehouseTree() {
  const res = await request.get('/warehouse/tree')
  warehouseTree.value = res.data.data || []
}

// 展开状态追踪
const expanded = reactive(new Set<number>())
function toggle(id: number) { expanded.has(id) ? expanded.delete(id) : expanded.add(id) }
function isExpanded(id: number) { return expanded.has(id) }

function handleSearch() { fetchData() }
function handleReset() { query.value = { productName: '', warehouseId: undefined }; fetchData(); expanded.clear() }
function handleExport() { downloadFile('/inventory/export', '库存查询.xlsx') }

// 从 inventory 数据构建商品名→库存的映射
const invByWarehouse = computed(() => {
  const map = new Map<number, Inventory[]>()
  for (const item of allList.value) {
    const wid = item.warehouseId
    if (!map.has(wid)) map.set(wid, [])
    map.get(wid)!.push(item)
  }
  return map
})

// 给仓库树注入库存数据
function buildTreeWithStock(nodes: any[]): any[] {
  return nodes.map(n => {
    const invs = invByWarehouse.value.get(n.id) || []
    const qty = invs.reduce((s: number, i: any) => s + (i.quantity || 0), 0)
    const amt = invs.reduce((s: number, i: any) => s + ((i.costPrice || 0) * (i.quantity || 0)), 0)
    const node = { ...n, _pc: invs.length, _qty: qty, _amt: amt }
    if (n.children?.length) {
      node.children = buildTreeWithStock(n.children)
    }
    return node
  })
}

// 当级联选择了仓库时，裁剪树只显示该仓库及其子节点
function pruneTree(nodes: any[], targetId: number): any[] | null {
  for (const n of nodes) {
    if (n.id === targetId) return [n]
    if (n.children?.length) {
      const found = pruneTree(n.children, targetId)
      if (found) return found
    }
  }
  return null
}

const displayTree = computed(() => {
  const tree = warehouseTree.value
  if (!query.value.warehouseId) return tree
  return pruneTree(tree, query.value.warehouseId) || tree
})

// 展平树为列表，depth 用于缩进；跳过收起分支
function flattenTree(nodes: any[], depth = 0): any[] {
  const result: any[] = []
  for (const n of nodes) {
    result.push({ ...n, depth })
    if (n.children?.length && isExpanded(n.id)) {
      result.push(...flattenTree(n.children, depth + 1))
    }
  }
  return result
}

const flatList = computed(() => flattenTree(buildTreeWithStock(displayTree.value)))

// 合计
const grandTotal = computed(() => {
  const qty = allList.value.reduce((s: number, i: any) => s + (i.quantity || 0), 0)
  const amt = allList.value.reduce((s: number, i: any) => s + ((i.costPrice || 0) * (i.quantity || 0)), 0)
  return { qty, amt }
})

onMounted(() => { fetchWarehouseTree(); fetchData() })
</script>

<template>
  <div>
    <div class="page-header">
      <h2>库存查询</h2>
      <div>
        <el-button @click="router.push('/inventory/log')">库存流水</el-button>
        <el-button @click="handleExport">导出Excel</el-button>
      </div>
    </div>

    <div class="search-bar">
      <el-input v-model="query.productName" placeholder="商品名称/编码" clearable style="width:200px" @keyup.enter="handleSearch" @clear="handleSearch" />
      <el-tree-select
        v-model="query.warehouseId"
        :data="warehouseTree"
        :props="{ label: 'name', children: 'children' }"
        node-key="id"
        placeholder="全部仓库"
        clearable
        filterable
        style="width:220px"
        @change="fetchData"
      />
      <el-button type="primary" @click="handleSearch">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <div v-loading="loading">
      <div v-if="!flatList.length && !loading" style="text-align:center;padding:60px 0;color:#999;">暂无库存数据</div>

      <div v-for="node in flatList" :key="node.id" class="tree-row" :style="{ paddingLeft: (node.depth * 24 + 16) + 'px' }">
        <div class="tree-node" :class="'level-' + node.level" @click="node.children?.length && toggle(node.id)">
          <span v-if="node.children?.length" class="toggle-icon">{{ isExpanded(node.id) ? '▼' : '▶' }}</span>
          <span v-else class="toggle-icon" style="visibility:hidden;">▶</span>
          <span class="node-name">{{ node.name }}</span>
          <el-tag size="small" :type="['primary','success','warning','info'][node.level-1] || 'info'">{{ node.level }}级</el-tag>
          <span class="node-stats" v-if="node._pc">{{ node._pc }} 种 · {{ node._qty }} 件 · ¥{{ node._amt.toFixed(2) }}</span>
        </div>
        <!-- 叶子节点：展示库存表格 -->
        <div v-if="!node.children?.length && node._pc" class="leaf-inventory">
          <el-table :data="invByWarehouse.get(node.id) || []" stripe border size="small">
            <el-table-column prop="productCode" label="编码" width="100" />
            <el-table-column prop="productName" label="商品" min-width="130" />
            <el-table-column prop="batchNo" label="批次" width="80" />
            <el-table-column prop="quantity" label="数量" width="70" align="center">
              <template #default="{ row }"><span style="font-weight:600;">{{ row.quantity }}</span></template>
            </el-table-column>
            <el-table-column label="均价" width="100" align="right">
              <template #default="{ row }">¥{{ (row.costPrice || 0).toFixed(2) }}</template>
            </el-table-column>
            <el-table-column label="金额" width="100" align="right">
              <template #default="{ row }">¥{{ ((row.costPrice || 0) * (row.quantity || 0)).toFixed(2) }}</template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <div class="grand-total" v-if="flatList.length">
        <span>总计：{{ grandTotal.qty }} 件 · 金额 ¥{{ grandTotal.amt.toFixed(2) }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.tree-row { border-bottom: 1px solid #f0f0f0; }
.tree-row:last-child { border-bottom: none; }
.tree-row:hover { background: #fafafa; }
.tree-node {
  display: flex; align-items: center; gap: 8px; padding: 10px 0;
  cursor: pointer; user-select: none;
}
.tree-node.level-1 { background: #ecf5ff; margin: -1px -16px; padding: 10px 16px; border-radius: 6px 6px 0 0; font-size: 15px; font-weight: 700; }
.tree-node.level-1:hover { background: #d9ecff; }
.tree-node.level-2 { font-size: 14px; }
.tree-node.level-3 { font-size: 13px; }
.tree-node.level-4 { cursor: default; font-size: 13px; }
.toggle-icon { font-size: 10px; color: #909399; width: 12px; text-align: center; flex-shrink: 0; }
.node-name { color: #303133; font-weight: 600; white-space: nowrap; }
.node-stats { font-size: 12px; color: #909399; margin-left: auto; white-space: nowrap; }
.leaf-inventory { margin: 4px 0 12px 0; }
.grand-total {
  text-align: right; padding: 14px 20px; margin-top: 8px;
  font-size: 15px; font-weight: 600; color: #303133;
  background: #f5f7fa; border-radius: 6px;
}
</style>
