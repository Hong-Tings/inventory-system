<script setup lang="ts">
import { ref, onMounted, reactive, watch } from 'vue'
import request from '../../api/request'
import type { Warehouse, Customer, Product } from '../../types/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute, useRouter } from 'vue-router'

const router = useRouter()
const route = useRoute()
const isEdit = ref(false)
const orderId = ref<number | null>(null)
const submitting = ref(false)
const warehouses = ref<Warehouse[]>([])
const customers = ref<Customer[]>([])
const products = ref<Product[]>([])
const batchInventory = ref<Record<number, Array<{batchNo: string; quantity: number}>>>({})
const productStock = ref<Record<number, number>>({})
const warehouseStock = ref<Record<number, number>>({})

const form = reactive({
  customerId: undefined as number | undefined,
  warehouseId: undefined as number | undefined,
  salesman: '',
  externalOrderNo: '',
  orderDate: new Date().toISOString().slice(0, 10),
  remark: '',
  items: [] as Array<{
    productId: number | undefined
    productName: string
    quantity: number
    unitPrice: number | null
    amount: number | null
    batchNo: string
    remark: string
  }>,
})

async function fetchBase() {
  const [wRes, cRes, pRes] = await Promise.all([
    request.get('/warehouse/list'),
    request.get('/customer/list'),
    request.get('/product/list'),
  ])
  warehouses.value = wRes.data.data
  customers.value = cRes.data.data
  products.value = pRes.data.data
}

function addItem() {
  form.items.push({ productId: undefined, productName: '', quantity: 1, unitPrice: null, amount: null, batchNo: '', remark: '' })
}
function removeItem(index: number) { form.items.splice(index, 1) }
function calcAmount(index: number) {
  const item = form.items[index]
  if (item.quantity && item.unitPrice != null) item.amount = item.quantity * item.unitPrice
}

async function fetchBatchInventory(productId: number, warehouseId: number) {
  if (!productId || !warehouseId) return
  try {
    const res = await request.get('/inventory/page', {
      params: { productId, warehouseId, page: 1, size: 100 }
    })
    batchInventory.value[productId] = (res.data.data.records || [])
      .filter((r: any) => r.batchNo && r.quantity > 0)
      .map((r: any) => ({ batchNo: r.batchNo, quantity: r.quantity }))
  } catch { /* ignore */ }
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

  const product = products.value.find(p => p.id === productId)
  if (product && product.salePrice != null) {
    item.unitPrice = product.salePrice
  }
  calcAmount(index)
  if (item.productId && form.warehouseId) {
    fetchBatchInventory(item.productId, form.warehouseId)
    // 查询总库存
    try {
      const res = await request.get('/inventory/page', {
        params: { productId: item.productId, warehouseId: form.warehouseId, page: 1, size: 1 }
      })
      const records = res.data.data.records || []
      const total = records.reduce((sum: number, r: any) => sum + (r.quantity || 0), 0)
      productStock.value[item.productId] = total
    } catch { /* ignore */ }
  }
}

function disabledDate(time: Date) {
  return time.getTime() > Date.now()
}

async function handleSave() {
  if (!form.warehouseId || form.items.length === 0) { ElMessage.warning('请填写完整信息'); return }
  submitting.value = true
  try {
    if (isEdit.value && orderId.value) {
      await request.put(`/sales-order/${orderId.value}/draft`, form)
      ElMessage.success('更新成功')
    } else {
      await request.post('/sales-order', form)
      ElMessage.success('保存成功（草稿）')
    }
    router.push('/sales')
  } finally { submitting.value = false }
}

async function handleSubmit() {
  if (!form.warehouseId || form.items.length === 0) { ElMessage.warning('请填写完整信息'); return }
  submitting.value = true
  try {
    let id = orderId.value
    if (isEdit.value && id) {
      await request.put(`/sales-order/${id}/draft`, form)
    } else {
      const res = await request.post('/sales-order', form)
      id = res.data.data
    }
    await request.put(`/sales-order/${id}/submit`)
    ElMessage.success('出库成功'); router.push('/sales')
  } finally { submitting.value = false }
}

watch(() => form.warehouseId, async (whId) => {
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

onMounted(async () => {
  await fetchBase()
  const editId = route.query.edit
  if (editId) {
    try {
      const res = await request.get(`/sales-order/${editId}`)
      const data = res.data.data
      if (data.status === 0) {
        isEdit.value = true
        orderId.value = data.id
        form.customerId = data.customerId
        form.warehouseId = data.warehouseId
        form.salesman = data.salesman || ''
        form.externalOrderNo = data.externalOrderNo || ''
        form.orderDate = data.orderDate || new Date().toISOString().slice(0, 10)
        form.remark = data.remark || ''
        form.items = (data.items || []).map((item: any) => ({
          productId: item.productId,
          productName: item.productName || '',
          quantity: item.quantity,
          unitPrice: item.unitPrice,
          amount: item.amount,
          batchNo: item.batchNo || '',
          remark: item.remark || '',
        }))
      }
    } catch { /* 非草稿或不存在 */ }
  }
})
</script>

<template>
  <div>
    <div class="page-header"><h2>{{ isEdit ? '编辑出库单' : '新建出库单' }}</h2></div>
    <div class="detail-card">
      <el-form :model="form" label-width="100px">
        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="客户" required>
              <el-select v-model="form.customerId" filterable style="width:100%">
                <el-option v-for="c in customers" :key="c.id" :label="c.name" :value="c.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="仓库" required>
              <el-select v-model="form.warehouseId" style="width:100%">
                <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="出库日期">
              <el-date-picker v-model="form.orderDate" type="date" value-format="YYYY-MM-DD" style="width:100%" :disabled-date="disabledDate" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="销售员"><el-input v-model="form.salesman" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="外部单号"><el-input v-model="form.externalOrderNo" /></el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="备注"><el-input v-model="form.remark" /></el-form-item>
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
        <el-table-column label="商品" min-width="160">
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
            <el-input-number v-model="form.items[$index].quantity" :min="1" style="width:100%" @change="calcAmount($index)" />
          </template>
        </el-table-column>
        <el-table-column label="售价" width="140">
          <template #default="{ $index }">
            <el-input-number v-model="form.items[$index].unitPrice" :precision="2" :min="0" style="width:100%" @change="calcAmount($index)" />
          </template>
        </el-table-column>
        <el-table-column label="金额" width="120">
          <template #default="{ $index }">¥{{ (form.items[$index].amount ?? 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="批次/库存" width="200">
          <template #default="{ $index }">
            <el-select v-model="form.items[$index].batchNo" filterable allow-create clearable placeholder="自动FIFO" style="width:100%">
              <el-option
                v-for="b in (batchInventory[form.items[$index].productId ?? 0] || [])"
                :key="b.batchNo"
                :label="`${b.batchNo} (余${b.quantity})`"
                :value="b.batchNo"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ $index }"><el-button size="small" type="danger" @click="removeItem($index)">删除</el-button></template>
        </el-table-column>
      </el-table>
    </div>
    <div style="display:flex;gap:12px;justify-content:flex-end">
      <el-button @click="router.push('/sales')">返回</el-button>
      <el-button :loading="submitting" @click="handleSave">保存草稿</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">确认出库</el-button>
    </div>
  </div>
</template>
