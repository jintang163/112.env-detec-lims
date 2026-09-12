<template>
  <view class="container">
    <view class="hello card">
      <view>
        <view style="font-size: 34rpx; font-weight: 600">你好，{{ user?.realName || '同事' }}</view>
        <view class="muted">{{ today }} · 采样作业平安顺利</view>
      </view>
      <view class="tag tag-blue">{{ roleText }}</view>
    </view>

    <view class="stat-grid">
      <view class="stat card" @tap="goTasks">
        <view class="num" style="color: #1677ff">{{ summary.entrustMonth ?? '-' }}</view>
        <view class="muted">本月委托</view>
      </view>
      <view class="stat card" @tap="goTasks?.(true)">
        <view class="num" style="color: #f5222d">{{ summary.entrustUrgent ?? '-' }}</view>
        <view class="muted">加急在途</view>
      </view>
      <view class="stat card">
        <view class="num" style="color: #52c41a">{{ summary.contractExecuting ?? '-' }}</view>
        <view class="muted">履约合同</view>
      </view>
      <view class="stat card" @tap="goApproval">
        <view class="num" style="color: #fa8c16">{{ todoCount }}</view>
        <view class="muted">审批待办</view>
      </view>
    </view>

    <view class="card">
      <view style="font-weight: 600; margin-bottom: 12rpx">委托单状态分布</view>
      <!-- uCharts 图表(H5/小程序/App 通用 canvas) -->
      <canvas
        canvas-id="statusChart"
        id="statusChart"
        style="width: 100%; height: 380rpx"
        @touchstart="touchChart"
      />
    </view>

    <view class="card">
      <view class="row between" style="margin-bottom: 12rpx">
        <text style="font-weight: 600">快捷入口</text>
      </view>
      <view class="quick-grid">
        <view class="quick" @tap="goTasks"><view class="qi">🧪</view>采样任务</view>
        <view class="quick" @tap="goApproval"><view class="qi">✅</view>审批待办</view>
        <view class="quick" @tap="goCustomers"><view class="qi">👥</view>客户查询</view>
        <view class="quick" @tap="goOffline"><view class="qi">📴</view>离线点位</view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { api } from '@/utils/request'
import { useUserStore } from '@/stores/user'
import { listLocalPoints } from '@/utils/db'

// uCharts 仅在客户端环境动态加载
let uChartsLib: any = null

const userStore = useUserStore()
const user = computed(() => userStore.info)
const roleText = computed(() => (user.value?.roles || []).join(' / ') || '作业人员')
const today = new Date().toLocaleDateString('zh-CN')
const summary = ref<any>({})
const todoCount = ref(0)

function touchChart() {}

async function loadChart() {
  const rows: any = await api.entrustStatus()
  if (!rows?.length) return
  try {
    if (!uChartsLib) {
      // @ts-ignore 由依赖提供
      uChartsLib = (await import('@qiun/ucharts')).default
    }
    new uChartsLib({
      type: 'pie',
      canvas2d: false,
      context: (uni as any).createCanvasContext ? (uni as any).createCanvasContext('statusChart') : undefined,
      canvasId: 'statusChart',
      background: '#ffffff',
      series: rows.map((r: any) => ({ name: r.status, data: Number(r.cnt) })),
      width: (uni.getSystemInfoSync().windowWidth || 375) - 80,
      height: 190,
      legend: { show: true, fontSize: 11, position: 'right' },
      extra: { pie: { activeOpacity: 0.6, activeRadius: 8 } }
    })
  } catch (e) {
    // 图表初始化失败不影响业务
    console.warn('uCharts init failed', e)
  }
}

async function load() {
  if (!userStore.isLogin) {
    uni.reLaunch({ url: '/pages/login/login' })
    return
  }
  try {
    summary.value = await api.dashboard()
  } catch (e) {}
  try {
    const todos: any = await api.approvals()
    todoCount.value = todos?.length || 0
  } catch (e) {}
  loadChart()
}

onShow(load)

function goTasks(urgent?: boolean) {
  uni.switchTab({ url: '/pages/tabbar/tasks' })
}
function goApproval() {
  uni.navigateTo({ url: '/pages/approval/todo' })
}
function goCustomers() {
  uni.switchTab({ url: '/pages/tabbar/customers' })
}
async function goOffline() {
  const pts = await listLocalPoints()
  const unsynced = pts.filter((p) => !p.synced).length
  uni.showToast({ title: `本地点位 ${pts.length} 条,待同步 ${unsynced} 条`, icon: 'none' })
}
</script>

<style lang="scss" scoped>
.hello { display: flex; justify-content: space-between; align-items: center; }
.stat-grid { display: flex; flex-wrap: wrap; justify-content: space-between; }
.stat { width: 48.5%; box-sizing: border-box; margin-bottom: 16rpx; }
.num { font-size: 48rpx; font-weight: 700; }
.quick-grid { display: flex; flex-wrap: wrap; }
.quick { width: 25%; text-align: center; font-size: 24rpx; color: #555; }
.qi { font-size: 48rpx; margin-bottom: 8rpx; }
</style>
