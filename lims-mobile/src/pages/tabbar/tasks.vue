<template>
  <view class="container">
    <view class="filters row">
      <input v-model="query.keyword" class="search" placeholder="搜索委托单号/项目名" confirm-type="search" @confirm="reload" />
      <switch :checked="onlyUrgent" @change="(e: any) => (onlyUrgent = e.detail.value)" color="#f5222d" />
      <text class="muted" style="margin-left: 6rpx">加急</text>
    </view>

    <view v-for="t in tasks" :key="t.order.id" class="card task" @tap="openDetail(t.order.id)">
      <view class="row between">
        <text class="code">{{ t.order.code }}</text>
        <view>
          <text v-if="t.order.urgency === 'URGENT'" class="urgent">加急</text>
          <text :class="['tag', statusClass(t.order.status)]">{{ statusText(t.order.status) }}</text>
        </view>
      </view>
      <view class="title">{{ t.order.title }}</view>
      <view class="muted">{{ t.customerName }}</view>
      <view class="row between" style="margin-top: 10rpx">
        <text class="muted">采样地址: {{ t.order.samplingAddress || '待确认' }}</text>
      </view>
      <view class="row between" style="margin-top: 6rpx">
        <text class="muted">期望报告: {{ t.order.expectedReportDate }}</text>
        <text v-if="t.order.hasSubcontract" class="tag tag-orange">含分包</text>
      </view>
    </view>

    <view v-if="!loading && !tasks.length" class="empty muted">暂无采样任务</view>
    <view class="loadmore" @tap="reload">刷新</view>
  </view>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { onShow, onReachBottom } from '@dcloudio/uni-app'
import { api } from '@/utils/request'

const loading = ref(false)
const tasks = ref<any[]>([])
const onlyUrgent = ref(false)
const query = reactive({ keyword: '', current: 1, size: 20 })

const STATUS_MAP: Record<string, string> = {
  ACCEPTED: '已受理', SAMPLING: '采样中', TESTING: '检测中', REPORTING: '报告中', COMPLETED: '已完成'
}
function statusText(s: string) {
  return STATUS_MAP[s] || s
}
function statusClass(s: string) {
  return ({
    ACCEPTED: 'tag-blue', SAMPLING: 'tag-blue', TESTING: 'tag-blue',
    REPORTING: 'tag-orange', COMPLETED: 'tag-green'
  } as any)[s] || 'tag-gray'
}

async function reload() {
  loading.value = true
  try {
    const res: any = await api.samplingTasks({
      current: query.current,
      size: query.size,
      keyword: query.keyword,
      urgency: onlyUrgent.value ? 'URGENT' : undefined
    })
    tasks.value = res.records
  } finally {
    loading.value = false
  }
}

function openDetail(id: number) {
  uni.navigateTo({ url: `/pages/entrust/detail?id=${id}` })
}

onShow(reload)
onReachBottom(() => {
  query.current++
})
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
