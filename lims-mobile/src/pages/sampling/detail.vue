<template>
  <view class="container">
    <view v-if="detail" class="card">
      <view class="row between">
        <text class="code">{{ detail.task.code }}</text>
        <text :class="['tag', statusClass(detail.task.status)]">{{ statusText(detail.task.status) }}</text>
      </view>
      <view class="title">{{ detail.plan.title }}</view>
      <view class="kv"><text class="k">委托单</text><text class="v">{{ detail.orderCode }}</text></view>
      <view class="kv"><text class="k">客户</text><text class="v">{{ detail.plan.customerName }}</text></view>
      <view class="kv"><text class="k">采样日期</text><text class="v">{{ detail.plan.planDate }}</text></view>
      <view class="kv"><text class="k">联系人</text><text class="v">{{ detail.plan.contactPerson }} {{ detail.plan.contactPhone }}</text></view>
      <view class="kv"><text class="k">地址</text><text class="v">{{ detail.plan.address }}</text></view>
      <view class="kv"><text class="k">天气</text><text class="v">{{ detail.plan.weather || '-' }}</text></view>
    </view>

    <view v-if="offline" class="offline-bar">当前为离线数据(已下载任务包)</view>

    <view class="card">
      <view class="section-title">采样点位 ({{ detail?.points.length || 0 }})</view>
      <view v-for="(p, i) in detail?.points || []" :key="p.id || i" class="kv">
        <text class="k">{{ p.sortNo || i + 1 }}. {{ p.name }}</text>
        <text class="v muted">{{ p.addrDesc }}</text>
      </view>
    </view>

    <view class="card">
      <view class="section-title">检测项 / 样品要求</view>
      <view v-for="(it, i) in detail?.items || []" :key="it.id || i" class="kv">
        <text class="v">{{ it.itemName }} <text v-if="it.qcRequired === 1" class="tag tag-purple">质控</text></text>
        <text class="k">{{ it.sampleName || '' }} ×{{ it.sampleQty || '-' }} {{ it.container || '' }}</text>
      </view>
    </view>

    <view class="card">
      <view class="section-title">携带设备 ({{ detail?.equipments.length || 0 }})</view>
      <view v-for="(e, i) in detail?.equipments || []" :key="e.id || i" class="kv">
        <text class="v">{{ e.equipmentName }}</text>
        <text class="k">×{{ e.qty }}</text>
      </view>
    </view>

    <view class="card">
      <view class="row between">
        <text class="section-title">现场样品 ({{ samples.length }})</text>
        <text v-if="pendingCount" class="tag tag-orange">待同步 {{ pendingCount }}</text>
      </view>
      <view v-for="s in samples" :key="s.clientUuid || s.id" class="sample" @tap="openLabel(s)">
        <view class="row between">
          <text class="code">{{ s.sampleCode || '未编号' }}</text>
          <view>
            <text v-if="s.offline" class="tag tag-orange">待同步</text>
            <text v-if="s.isQc === 1" class="tag tag-purple">质控{{ qcText(s.qcType) }}</text>
          </view>
        </view>
        <view class="muted">{{ s.pointName }} · {{ s.sampleName }}/{{ s.itemName }}</view>
        <view class="muted">{{ s.samplingTime ? fmt(s.samplingTime) : '' }} {{ s.temperature != null ? s.temperature + '℃' : '' }} {{ s.ph != null ? 'pH' + s.ph : '' }}</view>
      </view>
      <view v-if="!samples.length" class="muted" style="padding: 16rpx 0">尚未采集样品</view>
    </view>

    <view style="height: 180rpx"></view>

    <view class="footer">
      <view class="row" style="gap: 12rpx">
        <button class="btn ghost" @tap="download">离线下载</button>
        <button class="btn ghost" @tap="scan">扫码</button>
        <button class="btn ghost" @tap="goHandover">交接</button>
        <button v-if="pendingCount" class="btn warn" @tap="syncAll">同步({{ pendingCount }})</button>
        <button class="btn primary" @tap="goSample">现场采样</button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { api } from '@/utils/request'
import { getTaskBundle, listPendingSamples, markSampleSynced, saveTaskBundle } from '@/utils/db'
import { uploadFile } from '@/utils/request'

const taskId = ref(0)
const detail = ref<any>(null)
const offline = ref(false)
const pendingCount = ref(0)
const localSamples = ref<any[]>([])

const samples = computed(() => {
  const remote = detail.value?.samples?.map((x: any) => x.sample) || []
  // 待同步本地样品(带 offline 标记)合并展示
  const local = localSamples.value.map((row) => ({ ...row.json, offline: true }))
  return local.concat(remote)
})

const STATUS_MAP: Record<string, string> = {
  ASSIGNED: '已分配', SUBMITTED: '待交接', HANDED: '已交接', CANCELLED: '已取消'
}
function statusText(s: string) { return STATUS_MAP[s] || s }
function statusClass(s: string) {
  return ({ ASSIGNED: 'tag-blue', SUBMITTED: 'tag-orange', HANDED: 'tag-green' } as any)[s] || 'tag-gray'
}
function qcText(t?: string) {
  return ({ BLANK: '·空白', PARALLEL: '·平行', SPIKE: '·加标' } as any)[t || ''] || ''
}
function fmt(t: string) { return (t || '').replace('T', ' ').slice(5, 16) }

async function refreshPending() {
  const list = await listPendingSamples(taskId.value)
  pendingCount.value = list.length
  localSamples.value = list
}

async function load(fromCache = false) {
  try {
    const d: any = await api.taskDetail(taskId.value)
    detail.value = d
    offline.value = false
  } catch (e) {
    const bundle = await getTaskBundle(taskId.value)
    if (bundle) {
      detail.value = bundle
      offline.value = true
    } else if (!fromCache) {
      uni.showToast({ title: '加载失败且无离线数据', icon: 'none' })
    }
  }
  await refreshPending()
}

async function download() {
  uni.showLoading({ title: '下载中' })
  try {
    const d: any = await api.taskDetail(taskId.value)
    saveTaskBundle(taskId.value, d)
    await api.markDownloaded(taskId.value).catch(() => {})
    detail.value = d
    uni.showToast({ title: '已保存到本机,可离线作业', icon: 'none' })
  } catch (e) {
    uni.showToast({ title: '下载失败,请检查网络', icon: 'none' })
  } finally {
    uni.hideLoading()
  }
}

function goSample() {
  uni.navigateTo({ url: `/pages/sampling/sample-form?taskId=${taskId.value}` })
}

function openLabel(s: any) {
  uni.navigateTo({
    url: `/pages/sampling/label?code=${encodeURIComponent(s.sampleCode)}&name=${encodeURIComponent(s.sampleName || s.itemName || '')}&point=${encodeURIComponent(s.pointName || '')}`
  })
}

function scan() {
  uni.scanCode({
    onlyFromCamera: false,
    success: (r) => {
      uni.navigateTo({ url: `/pages/sampling/scan?code=${encodeURIComponent(r.result)}` })
    },
    fail: () => {
      // H5/不支持扫码时, 手动输入
      uni.navigateTo({ url: '/pages/sampling/scan' })
    }
  })
}

function goHandover() {
  uni.navigateTo({ url: `/pages/sampling/handover?taskId=${taskId.value}` })
}

/** 一键同步: 先传照片再提交样品(clientUuid 幂等) */
async function syncAll() {
  const pending = await listPendingSamples(taskId.value)
  if (!pending.length) return
  uni.showLoading({ title: `同步 0/${pending.length}` })
  let ok = 0
  for (const row of pending) {
    try {
      const photoIds: number[] = []
      for (const path of row.photos || []) {
        // 已上传过的路径直接带 id
        if (/^\d+$/.test(path)) { photoIds.push(Number(path)); continue }
        const f: any = await uploadFile(path, { bizType: 'SAMPLE_PHOTO' })
        photoIds.push(f.id)
      }
      await api.submitSample({ ...row.json, photoFileIds: photoIds })
      markSampleSynced(row.clientUuid)
      ok++
      uni.showLoading({ title: `同步 ${ok}/${pending.length}` })
    } catch (e) {
      break
    }
  }
  uni.hideLoading()
  uni.showToast({ title: ok === pending.length ? '全部同步完成' : `已同步 ${ok} 条,其余保留待重试`, icon: 'none' })
  await load(true)
}

onLoad((q: any) => {
  taskId.value = Number(q.id)
})
onShow(() => {
  if (taskId.value) load()
})
</script>

<style lang="scss" scoped>
.code { font-weight: 700; color: #1677ff; font-size: 28rpx; }
.title { font-size: 32rpx; font-weight: 700; margin: 12rpx 0; }
.kv { display: flex; justify-content: space-between; padding: 8rpx 0; font-size: 26rpx; }
.kv .k { color: #666; }
.kv .v { text-align: right; }
.section-title { font-weight: 700; font-size: 28rpx; margin-bottom: 8rpx; }
.offline-bar {
  background: #fff7e6; color: #d46b08; font-size: 24rpx;
  padding: 12rpx 24rpx; border-radius: 12rpx; margin-bottom: 16rpx;
}
.sample { padding: 16rpx 0; border-bottom: 1rpx solid #f0f0f0; }
.footer {
  position: fixed; left: 0; right: 0; bottom: 0;
  background: #fff; padding: 16rpx 20rpx;
  box-shadow: 0 -2rpx 12rpx rgba(0, 0, 0, 0.06);
}
.btn {
  flex: 1; font-size: 26rpx; padding: 0 10rpx; margin: 0;
  border-radius: 10rpx; line-height: 76rpx; height: 76rpx;
}
.btn.primary { background: #1677ff; color: #fff; }
.btn.warn { background: #fa8c16; color: #fff; }
.btn.ghost { background: #f0f5ff; color: #1677ff; }
.tag-purple { background: #f9f0ff; color: #722ed1; }
</style>
