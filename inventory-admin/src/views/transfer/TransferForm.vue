<script setup lang="ts">
import { ref, onMounted, reactive, watch } from 'vue'
import request from '../../api/request'
import type { Warehouse, Product } from '../../types/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'

const router = useRouter()
const route = useRoute()
const isEdit = ref(false)
const orderId = ref<number | null>(null)
const loading = ref(false)
const submitting = ref(false)
const warehouses = ref<Warehouse[]>([])
const products = ref<Product[]>([])
const warehouseStock = ref<Record<number, number>>({})

const form = reactive({
  outWarehouseId: undefined as number | undefined,
  inWarehouseId: undefined as number | undefined,
  orderDate: new Date().toISOString().slice(0, 10),
  remark: '',
  items: [] as Array<{
    productId: number | undefined
    productName: string
    quantity: number
  }>,
})

async function fetchBaseData() {
  const [wRes, pRes] = await Promise.all([
    request.get('/warehouse/list'),
    request.get('/product/list'),
  ])
  warehouses.value = wRes.data.data
  products.value = pRes.data.data
}

function addItem() {
  form.items.push({
    productId: undefined,
    productName: '',
    quantity: 1,
  })
}

function removeItem(index: number) {
  form.items.splice(index, 1)
}

async function onProductSelect(productId: number, index: number) {
  if (index < 0 || index >= form.items.length) return
  form.items[index].productId = productId
  const item = form.items[index]

  // 检查是否与其他行重复
  const dupIndex = form.items.findIndex((it, i) => i !== index && it.productId === productId)
  if (dupIndex !== -1) {
    try {
      await ElMessageBox.confirm('该商品已在明细中，确定再次添加？', '重复商品', { type: 'warning' })
    } catch {
      form.items[index].productId = undefined
      return
    }
  }
}

function disabledDate(time: Date) {
  return time.getTime() > Date.now()
}

async function handleSave() {
  if (!form.outWarehouseId || !form.inWarehouseId || form.items.length === 0) {
    ElMessage.warning('请填写完整信息')
    return
  }
  if (form.outWarehouseId === form.inWarehouseId) {
    ElMessage.warning('调出仓库和调入仓库不能相同')
    return
  }
  submitting.value = true
  try {
    if (isEdit.value && orderId.value) {
      await request.put(`/transfer/${orderId.value}/draft`, form)
      ElMessage.success('更新成功')
    } else {
      await request.post('/transfer', form)
      ElMessage.success('保存成功（草稿）')
    }
    router.push('/transfer')
  } finally { submitting.value = false }
}

async function handleSubmit() {
  if (!form.outWarehouseId || !form.inWarehouseId || form.items.length === 0) {
    ElMessage.warning('请填写完整信息')
    return
  }
  if (form.outWarehouseId === form.inWarehouseId) {
    ElMessage.warning('调出仓库和调入仓库不能相同')
    return
  }
  submitting.value = true
  try {
    let id = orderId.value
    if (isEdit.value && id) {
      await request.put(`/transfer/${id}/draft`, form)
    } else {
      const res = await request.post('/transfer', form)
      id = res.data.data
    }
    await request.put(`/transfer/${id}/submit`)
    ElMessage.success('调拨成功')
    router.push('/transfer')
  } finally { submitting.value = false }
}

onMounted(async () => {
  await fetchBaseData()
  const editId = route.query.edit
  if (editId) {
    try {
      const res = await request.get(`/transfer/${editId}`)
      const data = res.data.data
      if (data.status === 0) {
        isEdit.value = true
        orderId.value = data.id
        form.outWarehouseId = data.fromWarehouseId || data.outWarehouseId
        form.inWarehouseId = data.toWarehouseId || data.inWarehouseId
        form.orderDate = data.orderDate || new Date().toISOString().slice(0, 10)
        form.remark = data.remark || ''
        form.items = (data.items || []).map((item: any) => ({
          productId: item.productId,
          productName: item.productName || '',
          quantity: item.quantity,
        }))
      }
    } catch { /* 非草稿或不存在 */ }
  }
})

watch(() => form.outWarehouseId, async (whId) => {
  if (whId) {
    try {
      const res = await request.get('/inventory/page', { params: { warehouseId: whId, page: 1, size: 1000 } })
      const records = res.data.data.records || []
      const stock: Record<number, number> = {}
      for (const r of records) {
        const pid = r.productId
        stock[pid] = (stock[pid] || 0) + (r.quantity || 0)
      }
      warehouseStock.value = stock
    } catch { /* ignore */ }
  } else {
    warehouseStock.value = {}
  }
})
</script>

<template>
  <div>
    <div class="page-header"><h2>{{ isEdit ? '编辑调拨单' : '新建调拨单' }}</h2></div>
    <div class="detail-card">
      <el-form :model="form" label-width="100px">
        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="调出仓库" required>
              <el-select v-model="form.outWarehouseId" filterable style="width:100%" placeholder="选择调出仓库">
                <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="调入仓库" required>
              <el-select v-model="form.inWarehouseId" filterable style="width:100%" placeholder="选择调入仓库">
                <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="调拨日期">
              <el-date-picker v-model="form.orderDate" type="date" value-format="YYYY-MM-DD" style="width:100%" :disabled-date="disabledDate" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="备注">
              <el-input v-model="form.remark" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
    </div>

    <div class="detail-card">
      <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:16px">
        <h3>商品明细</h3>
        <el-button type="primary" size="small" @click="addItem">+ 添加商品</el-button>
      </div>
      <el-table :data="form.items" border stripe>
        <el-table-column label="商品" min-width="140">
          <template #default="{ $index }">
            <el-select v-model="form.items[$index].productId" filterable placeholder="选择商品" style="width:100%" @change="(val) => onProductSelect(val, $index)">
              <el-option v-for="p in products" :key="p.id" :value="p.id" :label="p.name + ' (' + p.code + ')'">
                <span style="display:flex;justify-content:space-between;width:100%;">
                  <span>{{ p.name }} ({{ p.code }})</span>
                  <span :style="{ color: (warehouseStock[p.id] || 0) > 0 ? '#2e7d32' : '#f56c6c', fontWeight: 'bold' }">
                    库存: {{ warehouseStock[p.id] ?? '-' }}
                  </span>
                </span>
              </el-option>
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="数量" width="140">
          <template #default="{ $index }">
            <el-input-number v-model="form.items[$index].quantity" :min="1" style="width:100%" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ $index }">
            <el-button size="small" type="danger" @click="removeItem($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div style="display:flex;gap:12px;justify-content:flex-end">
      <el-button @click="router.push('/transfer')">返回</el-button>
      <el-button :loading="submitting" @click="handleSave">保存草稿</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">确认调拨</el-button>
    </div>
  </div>
</template>
