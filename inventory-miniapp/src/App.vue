<script setup>
import { onLaunch } from '@dcloudio/uni-app'
import { useUserStore } from './store/user'

onLaunch(() => {
  const userStore = useUserStore()
  const token = uni.getStorageSync('token')
  if (token) {
    userStore.token = token
  }
})
</script>

<template>
  <view />
</template>

<style>
page {
  background-color: #f5f7f5;
  font-size: 14px;
  color: #333;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Helvetica Neue', sans-serif;
}
</style>
