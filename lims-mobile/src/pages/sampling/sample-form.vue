<template>
  <view class="container">
    <view class="card">
      <view class="section-title">点位与样品</view>
      <view class="field">
        <text class="label">采样点位</text>
        <picker :range="pointNames" @change="onPointChange">
          <view class="picker">{{ form.pointName || '请选择点位' }}</view>
        </picker>
      </view>
      <view class="field">
        <text class="label">检测项目</text>
        <picker :range="itemNames" @change="onItemChange">
          <view class="picker">{{ form.itemName || '请选择检测项目' }}</view>
        </picker>
      </view>
      <view class="field">
        <text class="label">样品名称</text>
        <input v-model="form.sampleName" class="input" placeholder="如 废水/有组织废气" />
      </view>
    </view>

    <view class="card">
      <view class="row between">
        <text class="section-title">GPS 定位 (BD-09)</text>
        <text class="link" @tap="locate">重新定位</text>
      </view>
      <view v-if="locating" class="muted">定位中...</view>
      <view v-else-if="form.lng" class="kv">
        <text class="k">{{ form.addrDesc || '当前位置' }}</text>
        <text class="v muted">{{ form.lng?.toFixed(6) }}, {{ form.lat?.toFixed(6) }}</text>
      </view>
      <view v-else class="muted">尚未获取位置, 点击"重新定位"</view>
      <view class="field" style="margin-top: 12rpx">
        <text class="label">采样时间</text>
        <input v-model="form.samplingTime" class="input" />
      </view>
    </view>

    <view class="card">
      <view class="section-title">现场参数</view>
      <view class="row" style="gap: 16rpx">
        <view class="field" style="flex: 1">
          <text class="label">温度(℃)</text>
          <input v-model.number="form.temperature" type="digit" class="input" placeholder="如 24.6" />
        </view>
        <view class="field" style="flex: 1">
          <text class="label">pH</text>
          <input v-model.number="form.ph" type="digit" class="input" placeholder="如 7.2" />
        </view>
      </view>
      <view v-for="(p, i) in extraParams" :key="i" class="row" style="gap: 12rpx; margin-bottom: 12rpx">
        <input v-model="p.key" class="input" style="flex: 1" placeholder="参数名(如 溶解氧)" />
        <input v-model="p.value" class="input" style="flex: 1" placeholder="参数值" />
        <text class="link danger" @tap="extraParams.splice(i, 1)">删</text>
      </view>
      <text class="link" @tap="extraParams.push({ key: '', value: '' })">+ 添加现场参数</text>
    </view>

    <view class="card">
      <view class="section-title">容器与保存</view>
      <view class="field">
        <text class="label">采样容器</text>
        <input v-model="form.container" class="input" placeholder="如 棕色玻璃瓶/聚乙烯瓶" />
      </view>
      <view class="field">
        <text class="label">保存条件</text>
        <picker :range="storageLabels" @change="(e: any) => (form.storageCondition = storageValues[e.detail.value])">
          <view class="picker">{{ storageLabel(form.storageCondition) || '请选择保存条件' }}</view>
        </picker>
      </view>
      <view class="row between field">
        <text class="label">质控样标记</text>
        <switch :checked="form.isQc === 1" color="#722ed1" @change="(e: any) => (form.isQc = e.detail.value ? 1 : 0)" />
      </view>
      <view v-if="form.isQc === 1" class="field">
        <text class="label">质控类型</text>
        <picker :range="qcLabels" @change="(e: any) => (form.qcType = ['BLANK', 'PARALLEL', 'SPIKE'][e.detail.value])">
          <view class="picker">{{ qcLabel(form.qcType) || '请选择质控类型' }}</view>
        </picker>
      </view>
      <view class="field">
        <text class="label">备注</text>
        <textarea v-model="form.remark" class="textarea" placeholder="异常情况/工况等" />
      </view>
    </view>

    <view class="card">
      <view class="section-title">现场照片</view>
      <view class="photos">
        <image
          v-for="(p, i) in photos"
          :key="i"
          :src="p"
          class="photo"
          mode="aspectFill"
          @tap="preview(i)"
          @longpress="photos.splice(i, 1)"
        />
        <view v-if="photos.length < 6" class="photo add" @tap="choosePhoto">+</view>
      </view>
      <view class="muted" style="font-size: 22rpx">长按照片可删除, 最多6张</view>
    </view>

    <view style="height: 160rpx"></view>
    <view class="footer">
      <view class="row" style="gap: 16rpx">
        <button class="btn ghost" @tap="save(true)">存离线</button>
        <button class="btn primary" :loading="saving" @tap="save(false)">保存样品</button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { api, uploadFile } from '@/utils/request'
import { getTaskBundle, queueSample } from '@/utils/db'
import { wgs84ToBd09 } from '@/utils/coord'

const taskId = ref(0)
const task = ref<any>(null)
const saving = ref(false)
const locating = ref(false)
const photos = ref<string[]>([])
const extraParams = ref<Array<{ key: string; value: string }>>([])
const storageValues = ref<string[]>([])
const storageLabels = ref<string[]>([])
const qcLabels = ['全程序空白', '平行样', '加标样']

const form = reactive<any>({
  clientUuid: '',
  pointId: null,
  pointName: '',
  entrustItemId: null,
  itemName: '',
  sampleName: '',
  samplingTime: now(),
  lng: null,
  lat: null,
  addrDesc: '',
  temperature: null,
  ph: null,
  container: '',
  storageCondition: '',
  isQc: 0,
  qcType: '',
  remark: ''
})

function now() {
  const d = new Date()
  const p = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

const pointNames = ref<string[]>([])
const itemNames = ref<string[]>([])

function onPointChange(e: any) {
  const p = task.value?.points?.[e.detail.value]
  if (p) {
    form.pointId = p.id
    form.pointName = p.name
    form.addrDesc = p.addrDesc || p.name
    if (!form.lng) {
      form.lng = Number(p.lng)
      form.lat = Number(p.lat)
    }
  }
}
function onItemChange(e: any) {
  const it = task.value?.items?.[e.detail.value]
  if (it) {
    form.entrustItemId = it.orderItemId
    form.itemName = it.itemName
    form.sampleName = form.sampleName || it.sampleName || ''
    form.container = form.container || it.container || ''
  }
}
function storageLabel(v: string) {
  const i = storageValues.value.indexOf(v)
  return i >= 0 ? storageLabels.value[i] : v
}
function qcLabel(v: string) {
  return qcLabels[['BLANK', 'PARALLEL', 'SPIKE'].indexOf(v)] || ''
}

function locate() {
  locating.value = true
  uni.getLocation({
    type: 'wgs84',
    success: (r) => {
      const bd = wgs84ToBd09(r.longitude, r.latitude)
      form.lng = Number(bd.lng.toFixed(7))
      form.lat = Number(bd.lat.toFixed(7))
      uni.showToast({ title: '定位成功(BD-09)', icon: 'none' })
    },
    fail: () => uni.showToast({ title: '定位失败, 请检查定位权限', icon: 'none' }),
    complete: () => (locating.value = false)
  })
}

function choosePhoto() {
  uni.chooseImage({
    count: 6 - photos.value.length,
    sourceType: ['camera', 'album'],
    success: (r) => {
      photos.value = photos.value.concat(r.tempFilePaths || [])
    }
  })
}
function preview(i: number) {
  uni.previewImage({ current: i, urls: photos.value })
}

function genUuid() {
  return 's' + Date.now() + '' + Math.floor(Math.random() * 1e6)
}

function buildPayload() {
  const params: Record<string, string> = {}
  extraParams.value.forEach((p) => {
    if (p.key) params[p.key] = p.value
  })
  const seq = (task.value?.samples?.length || 0) + 1
  return {
    clientUuid: form.clientUuid,
    taskId: taskId.value,
    sampleCode: task.value?.task?.code + '-' + String(seq).padStart(2, '0'),
    pointId: form.pointId,
    pointName: form.pointName,
    entrustItemId: form.entrustItemId,
    itemName: form.itemName,
    sampleName: form.sampleName,
    samplingTime: form.samplingTime,
    lng: form.lng,
    lat: form.lat,
    addrDesc: form.addrDesc,
    temperature: form.temperature,
    ph: form.ph,
    params,
    container: form.container,
    storageCondition: form.storageCondition,
    isQc: form.isQc,
    qcType: form.isQc === 1 ? form.qcType : null,
    remark: form.remark
  }
}

async function save(offlineOnly: boolean) {
  if (!form.pointName) return uni.showToast({ title: '请选择采样点位', icon: 'none' })
  if (!form.itemName) return uni.showToast({ title: '请选择检测项目', icon: 'none' })
  form.clientUuid = form.clientUuid || genUuid()
  const payload = buildPayload()

  if (offlineOnly) {
    queueSample({ clientUuid: form.clientUuid, taskId: taskId.value, json: payload, photos: photos.value })
    uni.showToast({ title: '已保存到本机, 联网后可同步', icon: 'none' })
    setTimeout(() => uni.navigateBack(), 600)
    return
  }

  saving.value = true
  try {
    const photoIds: any[] = []
    for (const path of photos.value) {
      const f: any = await uploadFile(path, { bizType: 'SAMPLE_PHOTO' })
      photoIds.push(f.id)
    }
    await api.submitSample({ ...payload, photoFileIds: photoIds })
    uni.showToast({ title: '样品已保存', icon: 'success' })
    setTimeout(() => uni.navigateBack(), 600)
  } catch (e) {
    // 网络失败自动转离线队列
    queueSample({ clientUuid: form.clientUuid, taskId: taskId.value, json: payload, photos: photos.value })
    uni.showToast({ title: '网络异常, 已存离线队列', icon: 'none' })
  } finally {
    saving.value = false
  }
}

onLoad(async (q: any) => {
  taskId.value = Number(q.taskId)
  try {
    task.value = await api.taskDetail(taskId.value)
  } catch (e) {
    task.value = await getTaskBundle(taskId.value)
  }
  pointNames.value = (task.value?.points || []).map((p: any) => p.name)
  itemNames.value = (task.value?.items || []).map((it: any) => it.itemName)
  const dict: any[] = await api.dict('storage_condition').catch(() => [])
  storageValues.value = dict.map((d) => d.itemValue)
  storageLabels.value = dict.map((d) => d.itemLabel)
  locate()
})
</script>

<style lang="scss" scoped>
.section-title { font-weight: 700; font-size: 28rpx; margin-bottom: 12rpx; }
.field { display: flex; align-items: center; margin-bottom: 16rpx; }
.label { width: 160rpx; color: #666; font-size: 26rpx; flex-shrink: 0; }
.input, .picker, .textarea {
  flex: 1; background: #f7f8fa; border-radius: 8rpx;
  padding: 14rpx 20rpx; font-size: 26rpx; min-height: 40rpx;
}
.textarea { height: 120rpx; }
.kv { display: flex; justify-content: space-between; font-size: 26rpx; }
.link { color: #1677ff; font-size: 26rpx; }
.link.danger { color: #f5222d; }
.photos { display: flex; flex-wrap: wrap; gap: 16rpx; }
.photo {
  width: 180rpx; height: 180rpx; border-radius: 12rpx; background: #f0f0f0;
}
.photo.add {
  display: flex; align-items: center; justify-content: center;
  color: #999; font-size: 60rpx; border: 2rpx dashed #d9d9d9; background: #fafafa;
}
.footer {
  position: fixed; left: 0; right: 0; bottom: 0;
  background: #fff; padding: 16rpx 20rpx;
  box-shadow: 0 -2rpx 12rpx rgba(0, 0, 0, 0.06);
}
.btn { flex: 1; font-size: 28rpx; border-radius: 10rpx; line-height: 80rpx; height: 80rpx; margin: 0; }
.btn.primary { background: #1677ff; color: #fff; }
.btn.ghost { background: #f0f5ff; color: #1677ff; }
</style>
