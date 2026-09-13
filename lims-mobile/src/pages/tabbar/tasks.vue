<template>
  <view class="container">
    <view class="filters row">
      <input v-model="query.keyword" class="search" placeholder="搜索任务编号/计划名" confirm-type="search" @confirm="reload" />
    </view>

    <view v-for="t in tasks" :key="t.id" class="card task" @tap="openDetail(t.id)">
      <view class="row between">
        <text class="code">{{ t.code }}</text>
        <text :class="['tag', statusClass(t.status)]">{{ statusText(t.status) }}</text>
      </view>
      <view class="title">{{ t.planTitle }}</view>
      <view class="muted">委托单: {{ t.orderCode }}</view>
      <view class="row between" style="margin-top: 10rpx">
        <text class="muted">派工时间: {{ fmt(t.assignedAt) }}</text>
        <text class="tag tag-cyan">样品 {{ t.sampleCount || 0 }}</text>
      </view>
      <view v-if="t.downloadedAt" class="muted" style="margin-top: 6rpx">已离线下载: {{ fmt(t.downloadedAt) }}</view>
    </view>

    <view v-if="!loading && !tasks.length" class="empty muted">暂无派给我的采样任务</view>
    <view class="loadmore" @tap="reload">刷新</view>
  </view>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { api } from '@/utils/request'

const loading = ref(false)
const tasks = ref<any[]>([])
const query = reactive({ keyword: '', current: 1, size: 50 })

const STATUS_MAP: Record<string, string> = {
  ASSIGNED: '已分配',
  SUBMITTED: '待交接',
  HANDED: '已交接',
  CANCELLED: '已取消'
}
function statusText(s: string) {
  return STATUS_MAP[s] || s
}
function statusClass(s: string) {
  return ({
    ASSIGNED: 'tag-blue',
    SUBMITTED: 'tag-orange',
    HANDED: 'tag-green',
    CANCELLED: 'tag-gray'
  } as any)[s] || 'tag-gray'
}
function fmt(t?: string) {
  return t ? t.replace('T', ' ').slice(0, 16) : '-'
}

async function reload() {
  loading.value = true
  try {
    const res: any = await api.myTasks({
      current: query.current,
      size: query.size,
      keyword: query.keyword
    })
    tasks.value = res.records
  } finally {
    loading.value = false
  }
}

function openDetail(id: number) {
  uni.navigateTo({ url: `/pages/sampling/detail?id=${id}` })
}

onShow(reload)
</script>

<style lang="scss" scoped>
.filters { margin-bottom: 16rpx; gap: 12rpx; }
.search {
  flex: 1;
  background: #fff;
  border-radius: 30rpx;
  padding: 14rpx 28rpx;
  font-size: 26rpx;
}
.task .code { font-weight: 700; color: #1677ff; font-size: 26rpx; }
.task .title { font-size: 30rpx; margin: 12rpx 0 6rpx; font-weight: 600; }
.empty { text-align: center; padding: 80rpx 0; }
.loadmore { text-align: center; color: #1677ff; padding: 20rpx; }
</style>
