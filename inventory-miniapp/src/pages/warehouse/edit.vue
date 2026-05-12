<script setup>
import { ref, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import request from '@/api/request'
import FloatingHome from '@/components/FloatingHome'

const isEdit = ref(false)
const loading = ref(false)
const submitting = ref(false)

const form = ref({
  name: '', code: '', contact: '', phone: '', address: '', remark: '',
  level: 4, parentId: null,
})

onLoad((query) => {
  if (query?.id) {
    isEdit.value = true
    fetchDetail(query.id)
  }
})

async function fetchDetail(id) {
  loading.value = true
  try {
    const res = await request.get(`/warehouse/${id}`)
    form.value = res.data
  } finally { loading.value = false }
}

async function handleSubmit() {
  if (!form.value.name) {
    uni.showToast({ title: '请输入仓库名称', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    if (isEdit.value) {
      await request.put(`/warehouse/${form.value.id}`, form.value)
      uni.showToast({ title: '更新成功', icon: 'success' })
    } else {
      await request.post('/warehouse', form.value)
      uni.showToast({ title: '创建成功', icon: 'success' })
    }
    setTimeout(() => uni.navigateBack(), 300)
  } finally { submitting.value = false }
}
</script>

<template>
  <view class="page">
    <view class="section">
      <view class="form-item">
        <text class="label">仓库名称 *</text>
        <input v-model="form.name" class="input" placeholder="请输入仓库名称" />
      </view>
      <view class="form-item">
        <text class="label">仓库编码</text>
        <input v-model="form.code" class="input" placeholder="自动生成" :disabled="!form.code" />
      </view>
      <view class="form-item">
        <text class="label">层级</text>
        <picker @change="e => form.level = [1,2,3,4][e.detail.value]" :range="['1级-大区','2级-区域','3级-城市','4级-仓库']">
          <view class="picker">{{ ['','1级-大区','2级-区域','3级-城市','4级-仓库'][form.level] || '请选择层级' }}</view>
        </picker>
      </view>
      <view class="form-item">
        <text class="label">联系人</text>
        <input v-model="form.contact" class="input" placeholder="请输入联系人" />
      </view>
      <view class="form-item">
        <text class="label">联系电话</text>
        <input v-model="form.phone" class="input" placeholder="请输入联系电话" />
      </view>
      <view class="form-item">
        <text class="label">地址</text>
        <input v-model="form.address" class="input" placeholder="请输入地址" />
      </view>
      <view class="form-item">
        <text class="label">备注</text>
        <input v-model="form.remark" class="input" placeholder="备注信息" />
      </view>
    </view>

    <button class="submit-btn" :loading="submitting" @click="handleSubmit">
      {{ isEdit ? '保存修改' : '创建仓库' }}
    </button>
    <FloatingHome />
  </view>
</template>

<style scoped>
.page { padding: 16px; padding-bottom: 80px; background: #f5f7f5; min-height: 100vh; }
.section { background: #fff; border-radius: 8px; padding: 16px; margin-bottom: 12px; }
.form-item { margin-bottom: 12px; }
.label { display: block; font-size: 14px; color: #333; margin-bottom: 4px; }
.input { border: 1px solid #dcdfe6; border-radius: 6px; padding: 10px 12px; font-size: 14px; background: #fff; }
.submit-btn { position: fixed; bottom: 0; left: 0; right: 0; margin: 16px; background: #2e7d32; color: #fff; border: none; border-radius: 8px; height: 44px; line-height: 44px; font-size: 16px; }
</style>
