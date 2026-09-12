<template>
  <view class="container" v-if="detail">
    <view class="card">
      <view class="row between">
        <text style="font-weight: 700">{{ detail.order.code }}</text>
        <view>
          <text v-if="detail.order.urgency === 'URGENT'" class="urgent">加急</text>
          <text :class="['tag', tagClass]">{{ statusText }}</text>
        </view>
      </view>
      <view style="font-size: 32rpx; font-weight: 600; margin: 14rpx 0">{{ detail.order.title }}</view>
      <view class="muted">{{ detail.customerName }}</view>
    </view>

    <view class="card">
      <view class="kv"><text class="muted">采样地址</text><text>{{ region }} {{ detail.order.samplingAddress || '-' }}</text></view>
      <view class="kv">
        <text class="muted">BD-09坐标</text>
        <text v-if="detail.order.lng">{{ Number(detail.order.lng).toFixed(6) }}, {{ Number(detail.order.lat).toFixed(6) }}</text>
        <text v-else>-</text>
      </view>
      <view class="kv"><text class="muted">联系人</text><text>{{ detail.order.contactPerson }} {{ detail.order.contactPhone }}</text></view>
      <view class="kv"><text class="muted">计划采样</text><text>{{ detail.order.plannedSamplingTime || '-' }}</text></view>
      <view class="kv"><text class="muted">期望报告</text><text style="color: #f5222d">{{ detail.order.expectedReportDate }}</text></view>
    </view>

    <view class="card">
      <view class="row between" style="margin-bottom: 12rpx">
        <text style="font-weight: 600">检测项目 ({{ detail.items.length }})</text>
      </view>
      <view v-for="(it, i) in detail.items" :key="it.id || i" class="item">
        <view class="row between">
          <text>{{ it.itemName }} <text v-if="it.isSubcontract" class="tag tag-orange">分包</text></text>
          <text class="muted">×{{ it.qty }}</text>
        </view>
        <view class="muted" style="font-size: 24rpx">{{ it.standardCode }} {{ it.standardName }}</view>
      </view>
    </view>

    <view class="card">
      <view class="row between">
        <text style="font-weight: 600">采样点位 ({{ detail.points.length }})</text>
        <text class="link" @tap="openMap">📍 地图打点采集</text>
      </view>
      <view v-for="(p, i) in detail.points" :key="p.id || i" class="kv">
        <text>{{ i + 1 }}. {{ p.name }}</text>
        <text class="muted">{{ Number(p.lng).toFixed(6) }}, {{ Number(p.lat).toFixed(6) }}</text>
      </view>
      <view v-if="!detail.points.length" class="muted" style="padding: 12rpx 0">暂无点位,点击右上角现场采集</view>
    </view>

    <view v-if="detail.order.status === 'SAMPLING'" class="footer">
      <button class="btn-finish" @tap="finish">采样完成,样品交接 → 检测</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { api } from '@/utils/request'

const detail = ref<any>(null)
const id = ref(0)

const STATUS: Record<string, string> = {
  ACCEPTED: '已受理', SAMPLING: '采样中', TESTING: '检测中', REPORTING: '报告中', COMPLETED: '已完成'
}
const statusText = computed(() => STATUS[detail.value?.order.status] || detail.value?.order.status)
const tagClass = computed(() =>
  detail.value?.order.status === 'COMPLETED' ? 'tag-green' : 'tag-blue')
const region = computed(() =>
  [detail.value?.order.province, detail.value?.order.city, detail.value?.order.district].filter(Boolean).join(' '))

async function load() {
  detail.value = await api.entrustDetail(id.value)
}

function openMap() {
  uni.navigateTo({ url: `/pages/sampling/map?id=${id.value}` })
}

function finish() {
  uni.showModal({
    title: '确认采样完成?',
    content: '确认后委托单进入检测环节',
    success: async (r) => {
      if (r.confirm) {
        await api.finishSampling(id.value)
        uni.showToast({ title: '已提交', icon: 'success' })
        load()
      }
    }
  })
}

onLoad((q: any) => {
  id.value = Number(q.id)
  load()
})
</script>

<style lang="scss" scoped>
.kv { display: flex; justify-content: space-between; padding: 10rpx 0; font-size: 26rpx; }
.item { padding: 12rpx 0; border-bottom: 1rpx solid #f5f5f5; }
.item:last-child { border: none; }
.link { color: #1677ff; font-size: 26rpx; }
.footer { position: fixed; left: 0; right: 0; bottom: 0; padding: 16rpx 24rpx; background: #fff; }
.btn-finish { background: #1677ff; color: #fff; border-radius: 44rpx; }
</style>
