<template>
  <div>
    <div class="picker-bar">
      <a-input-group compact>
        <a-input
          v-model:value="keyword"
          placeholder="输入地址后搜索, 或直接点击地图选点"
          style="width: calc(100% - 90px)"
          @press-enter="doSearch"
        />
        <a-button type="primary" style="width: 90px" @click="doSearch">搜索</a-button>
      </a-input-group>
      <div class="coord">
        <a-input-number v-model:value="lng" :precision="7" placeholder="经度 BD-09" @change="syncMarker" />
        <a-input-number v-model:value="lat" :precision="7" placeholder="纬度 BD-09" @change="syncMarker" />
        <a-button size="small" @click="useCurrent">使用当前位置</a-button>
      </div>
    </div>
    <div ref="mapEl" class="map-el"></div>
    <div v-if="!akReady" class="map-tip">
      未配置百度地图 AK（.env.development 的 VITE_BMAP_AK），可直接在上方手工输入 BD-09 经纬度
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { loadBMap, gcj02ToBd09 } from '@/utils/bmap'

const props = defineProps<{ modelValue?: { lng?: number; lat?: number; address?: string } }>()
const emit = defineEmits(['update:modelValue'])

const mapEl = ref<HTMLElement>()
const keyword = ref(props.modelValue?.address || '')
const lng = ref<number | null>(props.modelValue?.lng ?? null)
const lat = ref<number | null>(props.modelValue?.lat ?? null)
const akReady = ref(true)
let map: any
let marker: any

watch(
  () => props.modelValue,
  (v) => {
    lng.value = v?.lng ?? null
    lat.value = v?.lng ?? null
    keyword.value = v?.address || ''
  }
)

onMounted(async () => {
  try {
    const BMapGL = await loadBMap()
    const center =
      lng.value && lat.value
        ? new BMapGL.Point(lng.value, lat.value)
        : new BMapGL.Point(118.796877, 32.060255) // 南京
    map = new BMapGL.Map(mapEl.value)
    map.centerAndZoom(center, 15)
    map.enableScrollWheelZoom(true)
    marker = new BMapGL.Marker(center, { enableDragging: true })
    if (lng.value && lat.value) map.addOverlay(marker)
    map.addEventListener('click', (e: any) => pick(e.latlng.lng, e.latlng.lat))
    marker.addEventListener('dragend', (e: any) => pick(e.latlng.lng, e.latlng.lat))
  } catch (e) {
    akReady.value = false
  }
})

function pick(plng: number, plat: number) {
  const BMapGL = window.BMapGL
  lng.value = Number(plng.toFixed(7))
  lat.value = Number(plat.toFixed(7))
  if (!marker) marker = new BMapGL.Marker(new BMapGL.Point(plng, plat), { enableDragging: true })
  marker.setPosition(new BMapGL.Point(plng, plat))
  map.addOverlay(marker)
  emitChange()
  // 逆地址解析
  const geo = new BMapGL.Geocoder()
  geo.getLocation(new BMapGL.Point(plng, plat), (res: any) => {
    if (res?.address) {
      keyword.value = res.address
      emitChange(res.address)
    }
  })
}

function syncMarker() {
  if (map && window.BMapGL && lng.value && lat.value) {
    map.centerAndZoom(new window.BMapGL.Point(lng.value, lat.value), 15)
    emitChange()
  }
}

function doSearch() {
  if (!window.BMapGL || !keyword.value) return
  const local = new window.BMapGL.LocalSearch(map, {
    onSearchComplete: (res: any) => {
      const first = res?.getPoI?.(0) || (res?.getPoi && res.getPoi(0))
      // 兼容不同版本返回
      try {
        const poi = res.getPoi(0)
        if (poi?.point) pick(poi.point.lng, poi.point.lat)
      } catch (e) {
        // ignore
      }
    }
  })
  local.search(keyword.value)
}

function useCurrent() {
  navigator.geolocation.getCurrentPosition(
    (pos) => {
      // H5 定位为 WGS84, 这里先转 GCJ02 再转 BD09(简化处理, 生产建议用百度定位SDK)
      const gcj = wgs84ToGcj02(pos.coords.longitude, pos.coords.latitude)
      const bd = gcj02ToBd09(gcj.lng, gcj.lat)
      pick(bd.lng, bd.lat)
    },
    () => message.warn('定位失败或未授权')
  )
}

import { message } from 'ant-design-vue'

function wgs84ToGcj02(lng: number, lat: number) {
  // 标准 WGS84->GCJ02(国测局)转换
  const a = 6378245.0
  const ee = 0.00669342162296594323
  let dLat = transformLat(lng - 105.0, lat - 35.0)
  let dLng = transformLng(lng - 105.0, lat - 35.0)
  const radLat = (lat / 180.0) * Math.PI
  let magic = Math.sin(radLat)
  magic = 1 - ee * magic * magic
  const sqrtMagic = Math.sqrt(magic)
  dLat = (dLat * 180.0) / (((a * (1 - ee)) / (magic * sqrtMagic)) * Math.PI)
  dLng = (dLng * 180.0) / (a / sqrtMagic * Math.cos(radLat) * Math.PI)
  return { lng: lng + dLng, lat: lat + dLat }
}
function transformLat(lng: number, lat: number) {
  let ret = -100.0 + 2.0 * lng + 3.0 * lat + 0.2 * lat * lat + 0.1 * lng * lat + 0.2 * Math.sqrt(Math.abs(lng))
  ret += ((20.0 * Math.sin(6.0 * lng * Math.PI) + 20.0 * Math.sin(2.0 * lng * Math.PI)) * 2.0) / 3.0
  ret += ((20.0 * Math.sin(lat * Math.PI) + 40.0 * Math.sin((lat / 3.0) * Math.PI)) * 2.0) / 3.0
  ret += ((160.0 * Math.sin((lat / 12.0) * Math.PI) + 320 * Math.sin((lat * Math.PI) / 30.0)) * 2.0) / 3.0
  return ret
}
function transformLng(lng: number, lat: number) {
  let ret = 300.0 + lng + 2.0 * lat + 0.1 * lng * lng + 0.1 * lng * lat + 0.1 * Math.sqrt(Math.abs(lng))
  ret += ((20.0 * Math.sin(6.0 * lng * Math.PI) + 20.0 * Math.sin(2.0 * lng * Math.PI)) * 2.0) / 3.0
  ret += ((20.0 * Math.sin(lng * Math.PI) + 40.0 * Math.sin((lng / 3.0) * Math.PI)) * 2.0) / 3.0
  ret += ((150.0 * Math.sin((lng / 12.0) * Math.PI) + 300.0 * Math.sin((lng / 30.0) * Math.PI)) * 2.0) / 3.0
  return ret
}

function emitChange(address?: string) {
  emit('update:modelValue', {
    lng: lng.value,
    lat: lat.value,
    address: address ?? keyword.value
  })
}
</script>

<style scoped>
.picker-bar { margin-bottom: 8px; }
.coord {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}
.map-el {
  width: 100%;
  height: 340px;
  border: 1px solid #d9d9d9;
  border-radius: 6px;
  overflow: hidden;
}
.map-tip {
  margin-top: 6px;
  color: #faad14;
  font-size: 12px;
}
</style>
