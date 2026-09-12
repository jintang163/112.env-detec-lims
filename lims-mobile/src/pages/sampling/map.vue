<template>
  <view class="map-page">
    <!-- H5: 百度地图 WebGL; App/小程序: 使用原生 map 组件(BD-09) -->
    <!-- #ifdef H5 -->
    <view :id="mapId" class="map"></view>
    <!-- #endif -->
    <!-- #ifndef H5 -->
    <map
      class="map"
      :latitude="lat"
      :longitude="lng"
      :markers="markers"
      :scale="16"
      show-location
      @markertap="onMarker"
      @click="onMapClick"
    />
    <!-- #endif -->

    <view class="panel">
      <view class="row between">
        <text style="font-weight: 700">采样点位采集</text>
        <text class="muted">坐标系: 百度 BD-09</text>
      </view>
      <view class="coord-row">
        <text class="muted">经度</text><text>{{ lng?.toFixed(6) || '-' }}</text>
        <text class="muted" style="margin-left: 20rpx">纬度</text><text>{{ lat?.toFixed(6) || '-' }}</text>
      </view>
      <input v-model="name" class="input" placeholder="点位名称,如 1#排气筒" />
      <input v-model="addrDesc" class="input" placeholder="位置描述(可选)" />
      <view class="row" style="gap: 16rpx; margin-top: 14rpx">
        <button class="btn-loc" @tap="locate">🎯 定位当前位置</button>
        <button class="btn-save" @tap="savePoint">存为离线点位</button>
        <button class="btn-sync" @tap="syncAll">同步({{ unsyncedCount }})</button>
      </view>
      <scroll-view scroll-y class="local-list">
        <view v-for="p in localPoints" :key="p.id" class="local-item">
          <view>
            <text>{{ p.name }}</text>
            <text :class="['tag', p.synced ? 'tag-green' : 'tag-orange']" style="margin-left: 10rpx">
              {{ p.synced ? '已同步' : '待同步' }}
            </text>
          </view>
          <view class="muted" style="font-size: 22rpx">
            {{ p.lng.toFixed(6) }}, {{ p.lat.toFixed(6) }}
          </view>
        </view>
        <view v-if="!localPoints.length" class="muted" style="text-align: center; padding: 20rpx">
          暂无离线点位
        </view>
      </scroll-view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad, onReady } from '@dcloudio/uni-app'
import { api } from '@/utils/request'
import { wgs84ToBd09 } from '@/utils/coord'
import { listLocalPoints, savePointLocal, markPointSynced, type LocalPoint } from '@/utils/db'

const orderId = ref(0)
const mapId = 'bmapContainer'
const lng = ref<number>()
const lat = ref<number>()
const name = ref('')
const addrDesc = ref('')
const localPoints = ref<LocalPoint[]>([])

let bmap: any = null
const markers = computed(() =>
  localPoints.value.map((p, i) => ({
    id: i + 1,
    latitude: p.lat,
    longitude: p.lng,
    title: p.name,
    width: 30,
    height: 30
  }))
)
const unsyncedCount = computed(() => localPoints.value.filter((p) => !p.synced).length)

onLoad((q: any) => {
  orderId.value = Number(q.id)
  refreshLocal()
})

onReady(() => {
  // #ifdef H5
  initBMapH5()
  // #endif
})

// #ifdef H5
function initBMapH5() {
  const ak = '' // 在 H5 可通过后端下发或在此填写百度 JS API AK
  if (!ak || !(window as any).BMapGL) {
    // 无AK时使用定位按钮即可
    locate()
    return
  }
  const script = document.createElement('script')
  script.src = `https://api.map.baidu.com/api?type=webgl&v=1.0&ak=${ak}&callback=__bmapCb`
  ;(window as any).__bmapCb = () => {
    const BMapGL = (window as any).BMapGL
    bmap = new BMapGL.Map(mapId)
    bmap.centerAndZoom(new BMapGL.Point(118.796, 32.06), 15)
    bmap.enableScrollWheelZoom(true)
    bmap.addEventListener('click', (e: any) => pick(e.latlng.lng, e.latlng.lat))
    locate()
  }
  document.head.appendChild(script)
}
// #endif

function pick(plng: number, plat: number) {
  lng.value = plng
  lat.value = plat
}
function onMapClick(e: any) {
  // 原生 map 点击返回 GCJ-02, 需转 BD-09(App 若用百度地图模块返回已是 BD-09)
  const c = e.detail
  if (c?.longitude) {
    // 此处 uni 小程序 map 默认 GCJ-02;百度小程序为 BD-09
    // #ifdef MP-BAIDU
    pick(c.longitude, c.latitude)
    // #endif
    // #ifndef MP-BAIDU
    const bd = wgs84ToBd09(c.longitude, c.latitude)
    pick(bd.lng, bd.lat)
    // #endif
  }
}
function onMarker() {}

function locate() {
  uni.getLocation({
    // App 端配置百度定位并指定 'bd09' 可直接拿 BD-09; 此处演示 WGS84 转换
    type: 'wgs84',
    success: (res) => {
      const bd = wgs84ToBd09(res.longitude, res.latitude)
      pick(bd.lng, bd.lat)
      // #ifdef H5
      if (bmap && (window as any).BMapGL) {
        bmap.centerAndZoom(new (window as any).BMapGL.Point(bd.lng, bd.lat), 17)
      }
      // #endif
      uni.showToast({ title: '已定位(已转BD-09)', icon: 'none' })
    },
    fail: () => uni.showToast({ title: '定位失败,请检查授权', icon: 'none' })
  })
}

function savePoint() {
  if (lng.value == null || lat.value == null) {
    return uni.showToast({ title: '请先在地图上选点或定位', icon: 'none' })
  }
  if (!name.value) {
    return uni.showToast({ title: '请填写点位名称', icon: 'none' })
  }
  const p = savePointLocal({
    orderId: orderId.value,
    name: name.value,
    lng: Number(lng.value.toFixed(7)),
    lat: Number(lat.value.toFixed(7)),
    addrDesc: addrDesc.value
  })
  localPoints.value.unshift(p)
  name.value = ''
  addrDesc.value = ''
  uni.showToast({ title: '已离线保存,联网后同步', icon: 'success' })
}

async function syncAll() {
  const pending = localPoints.value.filter((p) => !p.synced && p.orderId === orderId.value)
  if (!pending.length) return uni.showToast({ title: '没有待同步点位', icon: 'none' })
  let ok = 0
  for (const p of pending) {
    try {
      await api.addPoint(p.orderId, { name: p.name, lng: p.lng, lat: p.lat, addrDesc: p.addrDesc })
      markPointSynced(p.id)
      ok++
    } catch (e) {
      uni.showToast({ title: '同步中断,请检查网络', icon: 'none' })
      break
    }
  }
  if (ok > 0) {
    uni.showToast({ title: `同步 ${ok} 个点位`, icon: 'success' })
    refreshLocal()
  }
}

async function refreshLocal() {
  localPoints.value = await listLocalPoints(orderId.value)
}
</script>

<style lang="scss" scoped>
.map-page { display: flex; flex-direction: column; height: 100vh; }
.map { flex: 1; width: 100%; min-height: 600rpx; }
.panel {
  background: #fff;
  border-radius: 24rpx 24rpx 0 0;
  padding: 24rpx;
  max-height: 52vh;
  display: flex;
  flex-direction: column;
}
.coord-row { display: flex; align-items: center; gap: 10rpx; margin: 12rpx 0; font-size: 26rpx; }
.input {
  background: #f5f6fa;
  border-radius: 10rpx;
  padding: 16rpx 20rpx;
  margin-top: 12rpx;
  font-size: 26rpx;
}
button { font-size: 24rpx; padding: 8rpx 20rpx; border-radius: 30rpx; }
.btn-loc { background: #e6f4ff; color: #1677ff; }
.btn-save { background: #fff7e6; color: #fa8c16; }
.btn-sync { background: #1677ff; color: #fff; }
.local-list { margin-top: 16rpx; max-height: 220rpx; }
.local-item { padding: 12rpx 0; border-bottom: 1rpx solid #f5f5f5; }
</style>
