<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
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

function onWarehouseChange(val: number | undefined) {
  query.value.warehouseId = val
  handleSearch()
}
function handleSearch() { fetchData() }
function handleReset() { query.value = { productName: '', warehouseId: undefined }; fetchData() }
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
    const qty = invs.reduce((s, i: any) => s + (i.quantity || 0), 0)
    const amt = invs.reduce((s, i: any) => s + ((i.costPrice || 0) * (i.quantity || 0)), 0)
    const node = { ...n, _productCount: invs.length, _totalQty: qty, _totalAmt: amt }
    if (n.children?.length) {
      node.children = buildTreeWithStock(n.children)
    }
    return node
  })
}

const treeWithStock = computed(() => buildTreeWithStock(warehouseTree.value))

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
      <el-cascader
        v-model="query.warehouseId"
        :options="warehouseTree"
        :props="{ value: 'id', label: 'name', children: 'children', emitPath: false, checkStrictly: true }"
        placeholder="全部仓库"
        clearable
        style="width:200px"
        @change="onWarehouseChange"
      />
      <el-button type="primary" @click="handleSearch">查询</el-button>
      <el-button @click="handleReset">重置</el-button>
    </div>

    <div v-loading="loading">
      <div v-if="!treeWithStock.length && !loading" style="text-align:center;padding:60px 0;color:#999;">暂无库存数据</div>

      <div v-for="root in treeWithStock" :key="root.id" class="tree-root">
        <div class="tree-node tree-level-1" @click="root._expanded = !root._expanded">
          <span class="toggle-icon">{{ root._expanded ? '▼' : '▶' }}</span>
          <span class="node-name">{{ root.name }}</span>
          <el-tag size="small" type="primary">{{ root.level }}级</el-tag>
          <span class="node-stats" v-if="root._productCount">{{ root._productCount }} 种 · {{ root._totalQty }} 件 · ¥{{ root._totalAmt.toFixed(2) }}</span>
        </div>
        <div v-show="root._expanded" style="padding-left:24px;">
          <template v-for="child in root.children" :key="child.id">
            <div v-if="child.children?.length" class="tree-node tree-level-2" @click="child._expanded = !child._expanded">
              <span class="toggle-icon">{{ child._expanded ? '▼' : '▶' }}</span>
              <span class="node-name">{{ child.name }}</span>
              <el-tag size="small" type="success">{{ child.level }}级</el-tag>
              <span class="node-stats" v-if="child._productCount">{{ child._productCount }} 种 · {{ child._totalQty }} 件 · ¥{{ child._totalAmt.toFixed(2) }}</span>
            </div>
            <div v-show="child._expanded || !child.children?.length" style="padding-left:24px;">
              <template v-if="child.children?.length">
                <div v-for="sub in child.children" :key="sub.id">
                  <div v-if="sub.children?.length" class="tree-node tree-level-3" @click="sub._expanded = !sub._expanded">
                    <span class="toggle-icon">{{ sub._expanded ? '▼' : '▶' }}</span>
                    <span class="node-name">{{ sub.name }}</span>
                    <el-tag size="small" type="warning">{{ sub.level }}级</el-tag>
                    <span class="node-stats" v-if="sub._productCount">{{ sub._productCount }} 种 · {{ sub._totalQty }} 件 · ¥{{ sub._totalAmt.toFixed(2) }}</span>
                  </div>
                  <div v-show="sub._expanded || !sub.children?.length" style="padding-left:24px;">
                    <div v-for="leaf in sub.children" :key="leaf.id">
                      <div class="tree-node tree-level-4">
                        <span class="node-name">{{ leaf.name }}</span>
                        <el-tag size="small" type="info">{{ leaf.level }}级</el-tag>
                        <span class="node-stats" v-if="leaf._productCount">{{ leaf._productCount }} 种 · {{ leaf._totalQty }} 件 · ¥{{ leaf._totalAmt.toFixed(2) }}</span>
                      </div>
                      <!-- 叶子节点展开显示实际库存 -->
                      <div v-if="leaf._productCount" class="leaf-inventory">
                        <el-table :data="invByWarehouse.get(leaf.id) || []" stripe border size="small">
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
                  </div>
                </div>
              </template>
              <template v-else>
                <div class="tree-node tree-level-4">
                  <span class="node-name">{{ child.name }}</span>
                  <el-tag size="small" type="info">{{ child.level }}级</el-tag>
                  <span class="node-stats" v-if="child._productCount">{{ child._productCount }} 种 · {{ child._totalQty }} 件 · ¥{{ child._totalAmt.toFixed(2) }}</span>
                </div>
                <div v-if="child._productCount" class="leaf-inventory">
                  <el-table :data="invByWarehouse.get(child.id) || []" stripe border size="small">
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
              </template>
            </div>
          </template>
        </div>
      </div>

      <div class="grand-total" v-if="treeWithStock.length">
        <span>总计：{{ grandTotal.qty }} 件 · 金额 ¥{{ grandTotal.amt.toFixed(2) }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.tree-root { margin-bottom: 4px; border: 1px solid #e4e7ed; border-radius: 6px; overflow: hidden; background: #fff; }
.tree-node {
  display: flex; align-items: center; gap: 8px; padding: 10px 16px;
  cursor: pointer; user-select: none; transition: background .15s;
  border-bottom: 1px solid #f0f0f0;
}
.tree-node:hover { background: #f5f7fa; }
.toggle-icon { font-size: 10px; color: #909399; width: 12px; text-align: center; flex-shrink: 0; }
.node-name { font-weight: 600; color: #303133; white-space: nowrap; }
.node-stats { font-size: 12px; color: #909399; margin-left: auto; white-space: nowrap; }
.tree-level-1 { background: #ecf5ff; font-size: 15px; }
.tree-level-2 { font-size: 14px; }
.tree-level-3 { font-size: 13px; }
.tree-level-4 { cursor: default; font-size: 13px; }
.leaf-inventory { margin: 8px 16px; }
.grand-total {
  text-align: right; padding: 14px 20px; margin-top: 8px;
  font-size: 15px; font-weight: 600; color: #303133;
  background: #f5f7fa; border-radius: 6px;
}
</style>
