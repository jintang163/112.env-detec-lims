<template>
  <view class="login-page">
    <view class="brand">
      <view class="logo">LIMS</view>
      <view class="title">环境检测移动作业端</view>
      <view class="sub">委托 · 采样 · 审批</view>
    </view>
    <view class="form card">
      <input v-model="form.username" class="input" placeholder="用户名" />
      <input v-model="form.password" class="input" placeholder="密码" password />
      <button class="btn" @tap="onLogin" :loading="loading">登 录</button>
      <view class="tips">演示: sampler/admin，密码 admin123</view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { api } from '@/utils/request'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const loading = ref(false)
const form = reactive({ username: 'sampler', password: 'admin123' })

async function onLogin() {
  if (!form.username || !form.password) {
    uni.showToast({ title: '请输入账号密码', icon: 'none' })
    return
  }
  loading.value = true
  try {
    const data: any = await api.login(form.username, form.password)
    userStore.setLogin(data)
    uni.showToast({ title: '登录成功', icon: 'success' })
    uni.switchTab({ url: '/pages/tabbar/workbench' })
  } catch (e) {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}
</script>

<style lang="scss" scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(160deg, #1677ff, #0a3d62);
  padding: 160rpx 48rpx 0;
  box-sizing: border-box;
}
.brand { text-align: center; color: #fff; margin-bottom: 60rpx; }
.logo {
  display: inline-block;
  background: rgba(255, 255, 255, 0.2);
  padding: 10rpx 30rpx;
  border-radius: 12rpx;
  font-size: 40rpx;
  font-weight: 700;
}
.title { font-size: 38rpx; margin-top: 24rpx; }
.sub { font-size: 26rpx; opacity: 0.8; margin-top: 8rpx; }
.form { padding: 48rpx 36rpx; }
.input {
  border: 1rpx solid #e5e5e5;
  border-radius: 12rpx;
  padding: 22rpx 24rpx;
  margin-bottom: 28rpx;
  font-size: 30rpx;
}
.btn { background: #1677ff; color: #fff; border-radius: 12rpx; margin-top: 10rpx; }
.tips { text-align: center; color: #999; font-size: 22rpx; margin-top: 24rpx; }
</style>
