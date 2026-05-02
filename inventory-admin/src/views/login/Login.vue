<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../store/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const form = ref({ username: '', password: '', rememberMe: false })
const loading = ref(false)
const inputFocused = ref(false)

const mx = ref(0), my = ref(0)
function onMouseMove(e: MouseEvent) {
  mx.value = (e.clientX / window.innerWidth - 0.5) * 2
  my.value = (e.clientY / window.innerHeight - 0.5) * 2
}

// 动态落叶
interface Leaf {
  id: number; x: number; y: number; size: number; rot: number; rotSpeed: number;
  color: string; speed: number; sway: number; swayOffset: number; opacity: number;
}
const floatLeaves = ref<Leaf[]>([])
function spawnLeaves() {
  const colors = ['#4caf50', '#66bb6a', '#81c784', '#a5d6a7', '#388e3c', '#558b2f']
  const leaves: Leaf[] = []
  for (let i = 0; i < 15; i++) {
    leaves.push({
      id: i,
      x: Math.random() * 100,
      y: Math.random() * 110 - 10,
      size: 12 + Math.random() * 14,
      rot: Math.random() * 360,
      rotSpeed: 0.05 + Math.random() * 0.1,
      color: colors[i % colors.length],
      speed: 0.01 + Math.random() * 0.03,
      sway: 0.3 + Math.random() * 0.5,
      swayOffset: Math.random() * Math.PI * 2,
      opacity: 0.08 + Math.random() * 0.07,
    })
  }
  floatLeaves.value = leaves
}

let animId = 0
let animTime = 0
function animateLeaves() {
  animTime += 0.01
  const leaves = floatLeaves.value
  for (const leaf of leaves) {
    leaf.y += leaf.speed
    leaf.x += Math.sin(animTime * 0.5 + leaf.swayOffset) * leaf.sway * 0.08
    leaf.rot += leaf.rotSpeed
    if (leaf.y > 112) { leaf.y = -8; leaf.x = Math.random() * 100; leaf.rot = Math.random() * 360 }
  }
  animId = requestAnimationFrame(animateLeaves)
}

onUnmounted(() => {
  window.removeEventListener('mousemove', onMouseMove)
  cancelAnimationFrame(animId)
})

// 从 localStorage 恢复记住的账号
onMounted(() => {
  const saved = localStorage.getItem('rememberedUser')
  if (saved) {
    try {
      const { username, password } = JSON.parse(saved)
      form.value.username = username
      form.value.password = password
      form.value.rememberMe = true
    } catch {}
  }
  window.addEventListener('mousemove', onMouseMove)
  spawnLeaves()
  animateLeaves()
})

function handleLogin() {
  if (!form.value.username || !form.value.password) { ElMessage.warning('请输入用户名和密码'); return }
  loading.value = true
  // 记住密码：保存到 localStorage；取消勾选则清除
  if (form.value.rememberMe) {
    localStorage.setItem('rememberedUser', JSON.stringify({ username: form.value.username, password: form.value.password }))
  } else {
    localStorage.removeItem('rememberedUser')
  }
  userStore.login(form.value.username, form.value.password, form.value.rememberMe).then(() => {
    ElMessage.success('登录成功')
    router.push('/')
  }).catch(() => {}).finally(() => { loading.value = false })
}
</script>

<template>
  <div class="login-page">
    <!-- 动态落叶 -->
    <div class="float-layer">
      <svg v-for="leaf in floatLeaves" :key="leaf.id" class="float-leaf"
        :style="{
          left: leaf.x + '%',
          top: leaf.y + '%',
          width: leaf.size + 'px',
          opacity: leaf.opacity,
          transform: 'rotate(' + leaf.rot + 'deg)',
        }"
        viewBox="0 0 30 40" fill="none">
        <!-- 叶片主体：柳叶形，一头尖 -->
        <path d="M15 2 C6 8 2 16 2 22 C2 30 8 36 15 38 C22 36 28 30 28 22 C28 16 24 8 15 2Z"
          :fill="leaf.color" />
        <!-- 叶脉 -->
        <path d="M15 2 L15 36" stroke="#fff" stroke-width="0.6" opacity="0.15" />
        <path d="M15 10 L8 6" stroke="#fff" stroke-width="0.4" opacity="0.1" />
        <path d="M15 10 L22 6" stroke="#fff" stroke-width="0.4" opacity="0.1" />
        <path d="M15 18 L6 16" stroke="#fff" stroke-width="0.4" opacity="0.1" />
        <path d="M15 18 L24 16" stroke="#fff" stroke-width="0.4" opacity="0.1" />
        <path d="M15 26 L8 28" stroke="#fff" stroke-width="0.4" opacity="0.1" />
        <path d="M15 26 L22 28" stroke="#fff" stroke-width="0.4" opacity="0.1" />
      </svg>
    </div>

    <!-- 背景大叶片（视差） -->
    <div class="plant-bg">
      <svg class="leaf leaf-left" viewBox="0 0 300 400" fill="none"
        :style="{ transform: `translate(${mx * 12}px, ${my * 8}px)` }">
        <path d="M260 380 C200 380 120 320 80 240 C40 160 20 80 60 30 C100 -20 180 10 220 70 C260 130 300 220 280 300 C260 380 260 380 260 380Z"
          fill="url(#lg1)" opacity="0.12" />
        <defs><linearGradient id="lg1"><stop offset="0%" stop-color="#2e7d32"/><stop offset="100%" stop-color="#81c784"/></linearGradient></defs>
      </svg>
      <svg class="leaf leaf-right" viewBox="0 0 200 280" fill="none"
        :style="{ transform: `translate(${mx * -8}px, ${my * -6}px)` }">
        <path d="M20 250 C50 250 110 200 140 140 C170 80 180 30 150 10 C120 -10 70 10 50 60 C30 110 0 180 10 230 C20 280 20 250 20 250Z"
          fill="url(#lg2)" opacity="0.08" />
        <defs><linearGradient id="lg2"><stop offset="0%" stop-color="#388e3c"/><stop offset="100%" stop-color="#a5d6a7"/></linearGradient></defs>
      </svg>
      <svg class="leaf leaf-btm" viewBox="0 0 160 120" fill="none"
        :style="{ transform: `translate(${mx * -15}px, ${my * 10}px)` }">
        <path d="M10 100 C40 110 90 100 120 70 C150 40 160 10 140 0 C120 -10 90 10 60 40 C30 70 0 90 10 100Z"
          fill="url(#lg3)" opacity="0.06" />
        <defs><linearGradient id="lg3"><stop offset="0%" stop-color="#2e7d32"/><stop offset="100%" stop-color="#66bb6a"/></linearGradient></defs>
      </svg>
    </div>

    <!-- 登录卡 -->
    <div class="login-card">
      <div class="card-header">
        <div class="logo-wrap" :class="{ 'leaf-sway': inputFocused }">
          <svg viewBox="0 0 64 64" width="64" height="64" fill="none">
            <path d="M20 52C20 48 20 46 22 44H42C44 46 44 48 44 52H20Z" fill="#a1887f" />
            <rect x="18" y="50" width="28" height="4" rx="2" fill="#8d6e63" />
            <path d="M32 44V28" stroke="#388e3c" stroke-width="3" stroke-linecap="round" />
            <path class="plant-leaf leaf-l" d="M32 36C26 36 18 32 16 26C20 24 28 26 32 36Z" fill="#4caf50" opacity="0.9" />
            <path class="plant-leaf leaf-r" d="M32 32C38 32 44 28 46 22C42 20 36 22 32 32Z" fill="#66bb6a" opacity="0.85" />
            <circle cx="32" cy="26" r="4" fill="#81c784" />
            <circle cx="32" cy="25" r="2.5" fill="#a5d6a7" />
          </svg>
        </div>
        <h1 class="title">进销存管理系统</h1>
        <p class="subtitle">INVENTORY MANAGEMENT SYSTEM</p>
      </div>
      <el-form @submit.prevent="handleLogin" label-width="0">
        <el-form-item>
          <div class="input-wrap">
            <el-icon class="input-icon"><User /></el-icon>
            <el-input v-model="form.username" placeholder="请输入用户名" size="large"
              @focus="inputFocused = true" @blur="inputFocused = false" />
          </div>
        </el-form-item>
        <el-form-item>
          <div class="input-wrap">
            <el-icon class="input-icon"><Lock /></el-icon>
            <el-input v-model="form.password" type="password" placeholder="请输入密码" size="large" show-password
              @focus="inputFocused = true" @blur="inputFocused = false" />
          </div>
        </el-form-item>
        <el-form-item>
          <div style="display:flex;justify-content:space-between;align-items:center;width:100%;">
            <el-checkbox v-model="form.rememberMe">记住密码</el-checkbox>
          </div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" class="login-btn" :loading="loading" @click="handleLogin">
            <span v-if="!loading">登&emsp;录</span>
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <p class="login-footer">© 2026 进销存管理系统 v2.0</p>
  </div>
</template>

<style scoped>
.login-page {
  display: flex; align-items: center; justify-content: center;
  height: 100vh;
  background: linear-gradient(135deg, #e8f5e9 0%, #f1f8e9 40%, #f9fbe7 100%);
  position: relative; overflow: hidden;
}

/* 动态小叶片 */
.float-layer { position: absolute; inset: 0; pointer-events: none; z-index: 0; }
.float-leaf {
  position: absolute;
  transition: none;
  will-change: transform;
}

/* 背景大叶片 */
.plant-bg { position: absolute; inset: 0; pointer-events: none; z-index: 0; }
.leaf { position: absolute; transition: transform 0.15s ease-out; }
.leaf-left { width: 260px; height: 360px; top: 40px; left: -20px; }
.leaf-right { width: 160px; height: 240px; bottom: 180px; right: 0; }
.leaf-btm { width: 140px; height: 100px; bottom: 140px; left: 180px; }

/* 登录卡 */
.login-card {
  position: relative;
  width: 380px; padding: 36px 34px 28px;
  background: rgba(255, 255, 255, 0.92);
  backdrop-filter: blur(20px) saturate(1.2);
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.08), 0 1px 3px rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.6);
  z-index: 2;
  animation: cardIn 0.5s ease-out;
}
@keyframes cardIn {
  from { opacity: 0; transform: translateY(20px); }
  to { opacity: 1; transform: translateY(0); }
}
.card-header { text-align: center; margin-bottom: 24px; }
.logo-wrap { margin-bottom: 10px; }
.plant-leaf { transform-origin: bottom center; transition: transform 0.3s ease; }
.leaf-sway .leaf-l { animation: swayL 0.6s ease-in-out infinite alternate; }
.leaf-sway .leaf-r { animation: swayR 0.6s ease-in-out infinite alternate; }
@keyframes swayL { 0% { transform: rotate(0deg); } 100% { transform: rotate(-6deg); } }
@keyframes swayR { 0% { transform: rotate(0deg); } 100% { transform: rotate(6deg); } }
.title { font-size: 22px; font-weight: 700; color: #1a1a1a; letter-spacing: 2px; }
.subtitle { font-size: 10px; color: #aaa; letter-spacing: 3px; }

.input-wrap { width: 100%; position: relative; display: flex; align-items: center; }
.input-icon { position: absolute; left: 14px; color: #aaa; font-size: 16px; z-index: 2; pointer-events: none; }
:deep(.el-input__wrapper) {
  padding-left: 42px; border-radius: 12px; height: 44px;
  background: #f8faf8; border: 1.5px solid #e8ece8;
  box-shadow: none !important; transition: all 0.25s;
}
:deep(.el-input__wrapper:hover) { border-color: #a5d6a7; background: #f5faf5; }
:deep(.el-input__wrapper.is-focus) {
  border-color: #2e7d32; background: #fff;
  box-shadow: 0 0 0 4px rgba(46, 125, 50, 0.08) !important;
}
:deep(.el-input__inner) { height: 42px; font-size: 14px; }
:deep(.el-input__inner::placeholder) { color: #bbb; }
:deep(.el-input__suffix) { padding-right: 8px; }

.login-btn {
  width: 100%; height: 44px; border-radius: 12px;
  font-size: 15px; font-weight: 600; letter-spacing: 4px;
  background: linear-gradient(135deg, #2e7d32, #1b5e20) !important;
  border: none !important; transition: all 0.3s;
}
.login-btn:hover { transform: translateY(-1px); box-shadow: 0 8px 28px rgba(46, 125, 50, 0.35) !important; }
.login-footer { position: absolute; bottom: 16px; color: #bbb; font-size: 11px; z-index: 2; }
</style>

