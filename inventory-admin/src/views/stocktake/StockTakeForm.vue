<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import request from '../../api/request'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'

const router = useRouter()
const submitting = ref(false)
const warehouseTree = ref<any[]>([])

const form = reactive({
  warehouseId: undefined as number | undefined,
  takeType: 0 as number,
  orderDate: new Date().toISOString().slice(0, 10),
  remark: '',
})

async function fetchWarehouseTree() {
  const res = await request.get('/warehouse/tree')
  warehouseTree.value = res.data.data
}

function disabledDate(time: Date) {
  return time.getTime() > Date.now()
}

async function handleCreate() {
  if (!form.warehouseId) { ElMessage.warning('请选择仓库'); return }
  submitting.value = true
  try {
    const res = await request.post('/stock-take', form)
    ElMessage.success('盘点单创建成功')
    router.push(`/stocktake/${res.data.data}`)
  } finally { submitting.value = false }
}

onMounted(fetchWarehouseTree)
</script>

<template>
  <div>
    <div class="page-header"><h2>新建盘点单</h2></div>
    <div class="detail-card" style="max-width:600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="仓库" required>
          <el-cascader
            v-model="form.warehouseId"
            :options="warehouseTree"
            :props="{ value: 'id', label: 'name', children: 'children', emitPath: false, checkStrictly: false }"
            placeholder="选择仓库"
            filterable
            clearable
            style="width:100%"
          />
        </el-form-item>
        <el-form-item label="盘点日期">
          <el-date-picker v-model="form.orderDate" type="date" value-format="YYYY-MM-DD" style="width:100%" :disabled-date="disabledDate" />
        </el-form-item>
        <el-form-item label="盘点方式">
          <el-radio-group v-model="form.takeType">
            <el-radio :value="0">全盘（盘点所有商品）</el-radio>
            <el-radio :value="1">抽盘（仅盘点录入的商品）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" />
        </el-form-item>
      </el-form>
      <div style="display:flex;gap:12px;justify-content:flex-end;margin-top:16px">
        <el-button @click="router.push('/stocktake')">返回</el-button>
        <el-button type="primary" :loading="submitting" @click="handleCreate">创建盘点单</el-button>
      </div>
    </div>
  </div>
</template>
