<template>
  <view class="container">
    <input v-model="keyword" class="search" placeholder="搜索客户名称/联系人" confirm-type="search" @confirm="reload" />
    <view v-for="c in list" :key="c.id" class="card">
      <view class="row between">
        <text style="font-weight: 600">{{ c.name }}</text>
        <text :class="['tag', c.ownerUserId ? 'tag-blue' : 'tag-orange']">{{ c.ownerUserId ? c.ownerName : '公海' }}</text>
      </view>
      <view class="muted" style="margin-top: 8rpx">
        {{ c.contactPerson || '-' }} {{ c.contactPhone || '' }}
      </view>
      <view class="row between" style="margin-top: 8rpx">
        <text class="muted">{{ [c.province, c.city, c.district].filter(Boolean).join(' ') || '地址未填' }}</text>
        <text class="tag tag-gray">{{ c.customerLevel }}级 · 信用{{ c.creditScore }}</text>
      </view>
    </view>
    <view v-if="!list.length" class="muted" style="text-align: center; padding: 60rpx">暂无数据</view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { api } from '@/utils/request'

const keyword = ref('')
const list = ref<any[]>([])

async function reload() {
  const res: any = await api.customers({ current: 1, size: 50, keyword: keyword.value })
  list.value = res.records
}
onShow(reload)
</script>

<style lang="scss" scoped>
.search {
  background: #fff;
  border-radius: 30rpx;
  padding: 16rpx 28rpx;
  font-size: 26rpx;
  margin-bottom: 18rpx;
}
</style>
