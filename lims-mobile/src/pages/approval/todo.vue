<template>
  <view class="container">
    <view v-for="t in todos" :key="t.id" class="card">
      <view class="row between">
        <text style="font-weight: 600">{{ t.title }}</text>
        <text class="tag tag-blue">{{ t.nodeName }}</text>
      </view>
      <view class="muted" style="margin: 8rpx 0">发起人: {{ t.initiatorName || '-' }} · {{ t.createTime }}</view>
      <view class="row" style="gap: 16rpx; margin-top: 10rpx">
        <button class="btn-ok" @tap="act(t, true)">通过</button>
        <button class="btn-no" @tap="act(t, false)">驳回</button>
      </view>
    </view>
    <view v-if="!todos.length" class="muted" style="text-align: center; padding: 100rpx 0">暂无待办</view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { api } from '@/utils/request'

const todos = ref<any[]>([])

async function load() {
  todos.value = (await api.approvals()) || []
}

function act(task: any, approve: boolean) {
  uni.showModal({
    title: approve ? '审批通过?' : '驳回?',
    editable: true,
    placeholderText: approve ? '审批意见(可选)' : '驳回原因(必填)',
    success: async (r) => {
      if (!r.confirm) return
      if (!approve && !r.content) {
        return uni.showToast({ title: '驳回需填写原因', icon: 'none' })
      }
      await api.approvalAct(task.id, approve, r.content || '')
      uni.showToast({ title: '已处理', icon: 'success' })
      load()
    }
  })
}

onShow(load)
</script>

<style lang="scss" scoped>
.btn-ok { background: #52c41a; color: #fff; border-radius: 30rpx; font-size: 26rpx; }
.btn-no { background: #fff1f0; color: #f5222d; border-radius: 30rpx; font-size: 26rpx; }
</style>
