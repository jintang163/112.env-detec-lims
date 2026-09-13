<template>
  <view class="container">
    <view class="card row" style="gap: 20rpx">
      <view class="avatar">{{ user?.realName?.[0] || 'U' }}</view>
      <view>
        <view style="font-size: 34rpx; font-weight: 600">{{ user?.realName }}</view>
        <view class="muted">@{{ user?.username }} · {{ (user?.roles || []).join(',') }}</view>
      </view>
    </view>

    <view class="card menu">
      <view class="menu-item" @tap="goApproval"><text>✅ 审批待办</text><text class="muted">›</text></view>
      <view class="menu-item" @tap="goTasks"><text>📋 我的采样任务</text><text class="muted">›</text></view>
      <view class="menu-item" @tap="goTasks"><text>📴 离线任务包</text><text class="muted">{{ bundleCount }} 个 ›</text></view>
      <view class="menu-item" @tap="goTasks">
        <text>⏳ 待同步样品</text>
        <text :class="pendingSamples ? 'warn' : 'muted'">{{ pendingSamples }} 条 ›</text>
      </view>
      <view class="menu-item" @tap="checkOffline"><text>📍 离线缓存点位</text><text class="muted">{{ pointCount }} 条 ›</text></view>
      <view class="menu-item" @tap="logout"><text style="color: #f5222d">🚪 退出登录</text><text class="muted">›</text></view>
    </view>
    <view class="muted" style="text-align: center; margin-top: 40rpx; font-size: 22rpx">
      LIMS 移动作业端 v1.1 · 现场采样 · 坐标 BD-09
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useUserStore } from '@/stores/user'
import { listLocalPoints, listPendingSamples, listTaskBundles } from '@/utils/db'

const userStore = useUserStore()
const user = computed(() => userStore.info)
const pointCount = ref(0)
const bundleCount = ref(0)
const pendingSamples = ref(0)

async function refreshStats() {
  pointCount.value = (await listLocalPoints()).length
  bundleCount.value = (await listTaskBundles()).length
  pendingSamples.value = (await listPendingSamples()).length
}

function goApproval() {
  uni.navigateTo({ url: '/pages/approval/todo' })
}
function goTasks() {
  uni.switchTab({ url: '/pages/tabbar/tasks' })
}
async function checkOffline() {
  uni.showToast({ title: `已缓存点位 ${pointCount.value} 条`, icon: 'none' })
}
function logout() {
  uni.showModal({
    title: '提示',
    content: '确认退出登录?',
    success: (r) => {
      if (r.confirm) {
        userStore.logout()
        uni.reLaunch({ url: '/pages/login/login' })
      }
    }
  })
}

onShow(refreshStats)
</script>

<style lang="scss" scoped>
.avatar {
  width: 88rpx;
  height: 88rpx;
  border-radius: 50%;
  background: #1677ff;
  color: #fff;
  text-align: center;
  line-height: 88rpx;
  font-size: 40rpx;
}
.menu-item {
  display: flex;
  justify-content: space-between;
  padding: 30rpx 10rpx;
  border-bottom: 1rpx solid #f0f0f0;
}
.menu-item:last-child { border-bottom: none; }
.warn { color: #fa8c16; font-weight: 700; }
</style>
