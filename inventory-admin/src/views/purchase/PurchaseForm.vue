<script setup lang="ts">
import { ref, onMounted, reactive, watch, computed } from 'vue'
import request from '../../api/request'
import type { Warehouse, Supplier, Product } from '../../types/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'

const router = useRouter()
const route = useRoute()
const isEdit = ref(false)
const orderId = ref<number | null>(null)
const loading = ref(false)
const submitting = ref(false)
const warehouses = ref<Warehouse[]>([])
const suppliers = ref<Supplier[]>([])
const products = ref<Product[]>([])

const selectedSupplier = computed(() =>
  suppliers.value.find(s => s.id === form.supplierId)
)
const selectedWarehouse = computed(() =>
  warehouses.value.find(w => w.id === form.warehouseId)
)
const warehouseStock = ref<Record<number, number>>({})
const dirty = ref(false)
const saved = ref(false)

const form = reactive({
  supplierId: undefined as number | undefined,
  warehouseId: undefined as number | undefined,
  orderDate: new Date().toISOString().slice(0, 10),
  remark: '',
  items: [] as Array<{
    productId: number | undefined
    productName: string
    quantity: number
    unitPrice: number | null
    amount: number | null
    batchNo: string
    productionDate: string
    expiryDate: string
    remark: string
  }>,
})

async function fetchBaseData() {
  const [wRes, sRes, pRes] = await Promise.all([
    request.get('/warehouse/list'),
    request.get('/supplier/list'),
    request.get('/product/list'),
  ])
  warehouses.value = wRes.data.data
  suppliers.value = sRes.data.data
  products.value = pRes.data.data
}

function addItem() {
  form.items.push({
    productId: undefined,
    productName: '',
    quantity: 1,
    unitPrice: null,
    amount: null,
    batchNo: '',
    productionDate: '',
    expiryDate: '',
    remark: '',
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

  const product = products.value.find(p => p.id === productId)
  if (product && product.purchasePrice != null) {
    item.unitPrice = product.purchasePrice
  }
  calcAmount(index)
}

function calcAmount(index: number) {
  const item = form.items[index]
  if (item.quantity && item.unitPrice != null) {
    item.amount = item.quantity * item.unitPrice
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
      await request.put(`/purchase-order/${orderId.value}/draft`, form)
      ElMessage.success('更新成功')
    } else {
      await request.post('/purchase-order', form)
      ElMessage.success('保存成功（草稿）')
    }
    saved.value = true; router.push('/purchase')
  } finally { submitting.value = false }
}

async function handleSubmit() {
  if (!form.warehouseId || form.items.length === 0) { ElMessage.warning('请填写完整信息'); return }
  // 检查明细行完整性
  for (let i = 0; i < form.items.length; i++) {
    const item = form.items[i]
    if (!item.productId) { ElMessage.warning(`第 ${i + 1} 行请选择商品`); return }
    if (!item.quantity || item.quantity <= 0) { ElMessage.warning(`第 ${i + 1} 行数量必须大于0`); return }
    if (item.unitPrice == null || item.unitPrice < 0) { ElMessage.warning(`第 ${i + 1} 行请输入有效单价`); return }
  }
  try { await ElMessageBox.confirm(`确认入库？共 ${form.items.length} 种商品，总金额 ¥${form.items.reduce((s, i) => s + (i.amount || 0), 0).toFixed(2)}`, '确认入库', { type: 'warning' }) } catch { return }
  submitting.value = true
  try {
    let id = orderId.value
    if (isEdit.value && id) {
      await request.put(`/purchase-order/${id}/draft`, form)
    } else {
      const res = await request.post('/purchase-order', form)
      id = res.data.data
    }
    await request.put(`/purchase-order/${id}/submit`)
    ElMessage.success('入库成功')
    saved.value = true; router.push('/purchase')
  } finally { submitting.value = false }
}

onBeforeRouteLeave(async (to, from, next) => {
  if (!dirty.value || saved.value) { next(); return }
  try { await ElMessageBox.confirm('表单尚未保存，确定离开？', '未保存的更改', { type: 'warning' }); next() } catch { next(false) }
})

watch(form, () => { if (!saved.value) dirty.value = true }, { deep: true })

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
  await fetchBaseData()
  const editId = route.query.edit
  if (editId) {
    try {
      const res = await request.get(`/purchase-order/${editId}`)
      const data = res.data.data
      if (data.status === 0) {
        isEdit.value = true
        orderId.value = data.id
        form.supplierId = data.supplierId
        form.warehouseId = data.warehouseId
        form.orderDate = data.orderDate || new Date().toISOString().slice(0, 10)
        form.remark = data.remark || ''
        form.items = (data.items || []).map((item: any) => ({
          productId: item.productId,
          productName: item.productName || '',
          quantity: item.quantity,
          unitPrice: item.unitPrice,
          amount: item.amount,
          batchNo: item.batchNo || '',
          productionDate: item.productionDate || '',
          expiryDate: item.expiryDate || '',
          remark: item.remark || '',
        }))
      }
    } catch { /* 非草稿或不存在 */ }
  }
})
</script>

<template>
  <div>
    <div class="page-header"><h2>{{ isEdit ? '编辑入库单' : '新建入库单' }}</h2></div>
    <div class="detail-card">
      <el-form :model="form" label-width="100px">
        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="供应商" required>
              <div style="display:flex;gap:8px;width:100%;">
                <el-select v-model="form.supplierId" filterable style="flex:1" placeholder="选择供应商">
                  <el-option v-for="s in suppliers" :key="s.id" :label="s.name" :value="s.id" />
                </el-select>
                <div v-if="form.supplierId" style="display:flex;align-items:center;gap:8px;white-space:nowrap;font-size:13px;color:#666;line-height:32px;">
                  <span>{{ selectedSupplier?.contact || '-' }}</span>
                  <span style="color:#999;">|</span>
                  <span>{{ selectedSupplier?.phone || '-' }}</span>
                </div>
              </div>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="仓库" required>
              <div style="display:flex;gap:8px;width:100%;">
                <el-select v-model="form.warehouseId" style="flex:1">
                  <el-option v-for="w in warehouses" :key="w.id" :label="w.name" :value="w.id" />
                </el-select>
                <div v-if="form.warehouseId" style="display:flex;align-items:center;gap:8px;white-space:nowrap;font-size:13px;color:#666;line-height:32px;">
                  <span>{{ selectedWarehouse?.contact || '-' }}</span>
                  <span style="color:#999;">|</span>
                  <span>{{ selectedWarehouse?.phone || '-' }}</span>
                </div>
              </div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="24">
          <el-col :span="8">
            <el-form-item label="入库日期">
              <el-date-picker v-model="form.orderDate" type="date" value-format="YYYY-MM-DD" style="width:100%" :disabled-date="disabledDate" />
            </el-form-item>
          </el-col>
          <el-col :span="16">
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
      <div style="color:#f56c6c;font-size:13px;margin-bottom:12px;">已自动填充采购价，支持手动修改，修改后请务必核实单价</div>
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
        <el-table-column label="数量" width="180">
          <template #default="{ $index }">
            <el-input-number v-model="form.items[$index].quantity" :min="1" style="width:100%" @change="calcAmount($index)" />
          </template>
        </el-table-column>
        <el-table-column label="单价" width="180">
          <template #default="{ $index }">
            <el-input-number v-model="form.items[$index].unitPrice" :precision="2" :min="0" style="width:100%" @change="calcAmount($index)" />
          </template>
        </el-table-column>
        <el-table-column label="金额" width="180">
          <template #default="{ $index }">¥{{ (form.items[$index].amount ?? 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="批次号" width="140">
          <template #default="{ $index }"><el-input v-model="form.items[$index].batchNo" size="small" /></template>
        </el-table-column>
        <el-table-column label="生产日期" width="140">
          <template #default="{ $index }"><el-date-picker v-model="form.items[$index].productionDate" type="date" value-format="YYYY-MM-DD" size="small" style="width:100%" /></template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ $index }">
            <el-button size="small" type="danger" @click="removeItem($index)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div style="display:flex;gap:12px;justify-content:flex-end">
      <el-button @click="router.push('/purchase')">返回</el-button>
      <el-button :loading="submitting" @click="handleSave">保存草稿</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">确认入库</el-button>
    </div>
  </div>
</template>
