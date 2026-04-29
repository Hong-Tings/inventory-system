<script setup>
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/store/user'
import request from '@/api/request'

const userStore = useUserStore()
const userName = ref(userStore.userInfo?.realName || '管理员')
const alertCount = ref(0)

function goPage(url) { uni.navigateTo({ url }) }
function switchTab(url) { uni.switchTab({ url }) }
function logout() { userStore.logout() }

onMounted(async () => {
  try {
    const res = await request.get('/dashboard/stats')
    alertCount.value = res.data?.alertCount || 0
  } catch { /* ignore */ }
})
</script>

<template>
  <view class="page">
    <view class="profile-header">
      <view class="avatar">管</view>
      <text class="name">{{ userName }}</text>
      <text class="role">系统管理员</text>
    </view>

    <view class="menu-list">
      <view class="menu-item" @click="goPage('/pages/product/list')">
        <text class="menu-lbl">商品列表</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" @click="goPage('/pages/alert/list')">
        <text class="menu-lbl">库存预警</text>
        <text style="color:#c62828;font-size:13px;">{{ alertCount }}</text>
      </view>
      <view class="menu-item">
        <text class="menu-lbl">修改密码</text>
        <text class="menu-arrow">›</text>
      </view>
      <view class="menu-item" style="border:none;">
        <text class="menu-lbl">关于系统 v1.0.0</text>
        <text class="menu-arrow">›</text>
      </view>
    </view>

    <button class="logout-btn" @click="logout">退出登录</button>
  </view>
</template>

<style scoped>
.page { padding: 12px; background: #f5f7f5; min-height: 100vh; }
.profile-header { background: #fff; border-radius: 8px; padding: 24px 16px; text-align: center; margin-bottom: 10px; }
.avatar { width: 56px; height: 56px; border-radius: 50%; background: #2e7d32; color: #fff; display: flex; align-items: center; justify-content: center; font-size: 22px; font-weight: 700; margin: 0 auto 10px; }
.name { font-size: 16px; font-weight: 600; color: #333; display: block; }
.role { font-size: 12px; color: #999; margin-top: 4px; display: block; }
.menu-list { background: #fff; border-radius: 8px; margin-bottom: 20px; }
.menu-item { display: flex; justify-content: space-between; align-items: center; padding: 14px 16px; border-bottom: 1px solid #f5f5f5; font-size: 14px; }
.menu-lbl { color: #333; }
.menu-arrow { color: #ccc; font-size: 16px; }
.logout-btn { width: 100%; height: 42px; line-height: 42px; background: #fff; color: #c62828; border: 1px solid #eee; border-radius: 8px; font-size: 15px; text-align: center; }
</style>
