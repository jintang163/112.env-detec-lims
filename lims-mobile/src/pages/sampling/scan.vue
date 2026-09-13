<template>
  <view class="container">
    <view class="card">
      <view class="row" style="gap: 16rpx">
        <input v-model="codeInput" class="search" placeholder="输入或扫描样品编号" confirm-type="search" @confirm="lookup" />
        <button class="btn ghost" @tap="scanCode">扫码</button>
      </view>
    </view>

    <view v-if="loading" class="muted" style="text-align: center; padding: 40rpx">核验中...</view>

    <view v-if="result" class="card">
      <view class="row between">
        <text class="code">{{ result.sample.sampleCode }}</text>
        <text :class="['tag', result.sample.status === 'RECEIVED' ? 'tag-green' : 'tag-cyan']">
          {{ result.sample.status === 'RECEIVED' ? '已接收' : '已采集' }}
        </text>
      </view>
      <view v-if="result.sample.isQc === 1" class="tag tag-purple" style="margin-top: 10rpx">
        质控样: {{ qcText(result.sample.qcType) }}
      </view>
      <view class="kv"><text class="k">点位</text><text class="v">{{ result.sample.pointName || '-' }}</text></view>
      <view class="kv"><text class="k">样品/项目</text><text class="v">{{ result.sample.sampleName }} / {{ result.sample.itemName }}</text></view>
      <view class="kv"><text class="k">采样时间</text><text class="v">{{ fmt(result.sample.samplingTime) }}</text></view>
      <view class="kv"><text class="k">坐标</text><text class="v">{{ result.sample.lng }}, {{ result.sample.lat }}</text></view>
      <view class="kv"><text class="k">温度/pH</text><text class="v">{{ result.sample.temperature ?? '-' }}℃ / {{ result.sample.ph ?? '-' }}</text></view>
      <view class="kv"><text class="k">容器</text><text class="v">{{ result.sample.container || '-' }}</text></view>
      <view class="kv"><text class="k">保存条件</text><text class="v">{{ result.sample.storageCondition || '-' }}</text></view>
      <view class="kv"><text class="k">采样员</text><text class="v">{{ result.sample.samplerName }}</text></view>

      <view v-if="extraParams.length" class="card-inner">
        <view class="section-title">其他现场参数</view>
        <view v-for="(p, i) in extraParams" :key="i" class="kv">
          <text class="k">{{ p[0] }}</text><text class="v">{{ p[1] }}</text>
        </view>
      </view>

      <view v-if="result.photos?.length" class="photos">
        <image
          v-for="p in result.photos"
          :key="p.id"
          :src="p.url"
          class="photo"
          mode="aspectFill"
          @tap="preview(p.url)"
        />
      </view>
    </view>

    <view v-else-if="notFound" class="card" style="text-align: center">
      <text class="tag tag-red">未找到该样品</text>
      <view class="muted" style="margin-top: 16rpx">编号: {{ notFound }}</view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { api } from '@/utils/request'

const codeInput = ref('')
const result = ref<any>(null)
const notFound = ref('')
const loading = ref(false)

const extraParams = computed<Array<[string, string]>>(() => {
  try {
    return Object.entries(JSON.parse(result.value?.sample?.paramsJson || '{}'))
  } catch (e) {
    return []
  }
})

function qcText(t?: string) {
  return ({ BLANK: '全程序空白', PARALLEL: '平行样', SPIKE: '加标样' } as any)[t || ''] || t
}
function fmt(t: string) {
  return (t || '').replace('T', ' ').slice(0, 19)
}

async function lookup(code?: string) {
  const c = (code || codeInput.value || '').trim()
  if (!c) return
  loading.value = true
  result.value = null
  notFound.value = ''
  codeInput.value = c
  try {
    const r: any = await api.sampleByCode(encodeURIComponent(c))
    if (r) result.value = r
    else notFound.value = c
  } finally {
    loading.value = false
  }
}

function scanCode() {
  uni.scanCode({
    success: (r) => lookup(r.result),
    fail: () => uni.showToast({ title: '不支持扫码, 请手动输入编号', icon: 'none' })
  })
}

function preview(url: string) {
  uni.previewImage({ urls: result.value.photos.map((p: any) => p.url), current: url })
}

onLoad((q: any) => {
  if (q.code) lookup(decodeURIComponent(q.code))
})
</script>

<style lang="scss" scoped>
.search { flex: 1; background: #fff; border-radius: 30rpx; padding: 14rpx 28rpx; font-size: 26rpx; }
.btn { font-size: 26rpx; border-radius: 30rpx; line-height: 64rpx; height: 64rpx; padding: 0 30rpx; margin: 0; }
.btn.ghost { background: #f0f5ff; color: #1677ff; }
.code { font-weight: 700; color: #1677ff; font-size: 30rpx; }
.kv { display: flex; justify-content: space-between; padding: 10rpx 0; font-size: 26rpx; }
.kv .k { color: #666; }
.card-inner { background: #fafafa; border-radius: 8rpx; padding: 16rpx; margin-top: 12rpx; }
.section-title { font-weight: 700; font-size: 26rpx; margin-bottom: 8rpx; }
.photos { display: flex; flex-wrap: wrap; gap: 12rpx; margin-top: 16rpx; }
.photo { width: 200rpx; height: 200rpx; border-radius: 12rpx; }
.tag-purple { background: #f9f0ff; color: #722ed1; }
.tag-red { background: #fff1f0; color: #f5222d; }
</style>
