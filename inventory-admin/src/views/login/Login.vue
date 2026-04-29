<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../store/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const form = reactive({ username: '', password: '' })
const loading = ref(false)

async function handleLogin() {
  if (!form.username || !form.password) { ElMessage.warning('请输入用户名和密码'); return }
  loading.value = true
  try {
    await userStore.login(form.username, form.password)
    ElMessage.success('登录成功')
    router.push('/')
  } catch { /* handled */ } finally { loading.value = false }
}
</script>

<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <div class="login-logo">🌿</div>
        <h2>进销存管理</h2>
        <p class="login-desc">请使用您的账号登录系统</p>
      </div>
      <el-form @submit.prevent="handleLogin" label-width="0">
        <el-form-item>
          <el-input v-model="form.username" placeholder="用户名" size="large" prefix-icon="User" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="密码" size="large" prefix-icon="Lock" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" class="login-btn" :loading="loading" @click="handleLogin">登录</el-button>
        </el-form-item>
      </el-form>
      <p class="login-tip">演示账号：admin / 123456</p>
    </div>
  </div>
</template>

<style scoped>
.login-container {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100vh;
  background: #f5f7f5;
}
.login-card {
  width: 400px;
  padding: 48px 36px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 24px rgba(0,0,0,0.06);
  border: 1px solid #e8ece8;
}
.login-header { text-align: center; margin-bottom: 32px; }
.login-logo { font-size: 48px; margin-bottom: 8px; }
.login-header h2 { font-size: 22px; font-weight: 700; color: #333; margin-bottom: 4px; }
.login-desc { font-size: 14px; color: #999; }
:deep(.el-input__wrapper) { border-radius: 8px; padding: 4px 14px; }
:deep(.el-input__inner) { height: 44px; }
.login-btn { width: 100%; height: 44px; border-radius: 8px; font-size: 15px; margin-top: 8px; }
.login-tip { text-align: center; font-size: 12px; color: #ccc; margin-top: 16px; }
</style>
