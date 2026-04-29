<script setup>
import { ref } from 'vue'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const username = ref('')
const password = ref('')
const loading = ref(false)

async function handleLogin() {
  if (!username.value || !password.value) {
    uni.showToast({ title: '请输入用户名和密码', icon: 'none' })
    return
  }
  loading.value = true
  try {
    await userStore.login(username.value, password.value)
    uni.showToast({ title: '登录成功', icon: 'success' })
    uni.switchTab({ url: '/pages/home/home' })
  } catch { /* handled */ } finally { loading.value = false }
}
</script>

<template>
  <view class="login-container">
    <view class="login-logo">存</view>
    <text class="login-title">进销存管理</text>
    <text class="login-desc">采购 · 销售 · 库存 · 盘点</text>

    <view class="login-card">
      <view class="form-item">
        <input v-model="username" class="input" placeholder="用户名" />
      </view>
      <view class="form-item">
        <input v-model="password" class="input" type="password" password placeholder="密码" />
      </view>
      <button class="login-btn" :loading="loading" @click="handleLogin">登录</button>
      <text class="login-tip">admin / 123456</text>
    </view>
  </view>
</template>

<style scoped>
.login-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: #fff;
  padding: 0 32px;
}
.login-logo {
  width: 56px; height: 56px; border-radius: 14px;
  background: #2e7d32; color: #fff;
  display: flex; align-items: center; justify-content: center;
  font-size: 22px; font-weight: 700; margin-bottom: 16px;
}
.login-title { font-size: 22px; font-weight: 700; color: #333; margin-bottom: 4px; }
.login-desc { font-size: 13px; color: #999; margin-bottom: 40px; }
.login-card { width: 100%; }
.form-item { margin-bottom: 14px; }
.input {
  width: 100%; height: 44px;
  border: 1px solid #e0e0e0; border-radius: 8px;
  padding: 0 14px; font-size: 15px; outline: none;
  box-sizing: border-box;
}
.input:focus { border-color: #2e7d32; }
.login-btn {
  width: 100%; height: 44px; line-height: 44px;
  background: #2e7d32; color: #fff;
  border: none; border-radius: 8px;
  font-size: 16px; font-weight: 600; margin-top: 4px;
}
.login-tip { display: block; text-align: center; font-size: 12px; color: #ccc; margin-top: 12px; }
</style>
