<template>
  <view class="container">
    <view class="label-card">
      <view class="org">环境检测样品标识</view>
      <canvas canvas-id="qrCanvas" class="qr" />
      <view class="code">{{ code }}</view>
      <view class="name">{{ name }}</view>
      <view v-if="point" class="muted">点位: {{ point }}</view>
      <view class="muted">{{ time }}</view>
    </view>
    <view class="tips muted">二维码内容即样品编号, 贴于采样容器, 交接时扫码核验</view>
    <view class="row" style="gap: 20rpx; margin-top: 24rpx">
      <button class="btn ghost" @tap="saveImage">保存图片</button>
      <!-- #ifdef H5 -->
      <button class="btn primary" @tap="print">打印标签</button>
      <!-- #endif -->
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import qrcode from 'qrcode-generator'
import { onLoad } from '@dcloudio/uni-app'

const code = ref('')
const name = ref('')
const point = ref('')
const time = ref('')

function draw() {
  const qr = qrcode(0, 'M')
  qr.addData(code.value)
  qr.make()
  const count = qr.getModuleCount()
  const size = 240
  const cell = size / count
  const ctx = uni.createCanvasContext('qrCanvas')
  ctx.setFillStyle('#ffffff')
  ctx.fillRect(0, 0, size, size)
  ctx.setFillStyle('#000000')
  for (let r = 0; r < count; r++) {
    for (let c = 0; c < count; c++) {
      if (qr.isDark(r, c)) {
        ctx.fillRect(c * cell, r * cell, cell, cell)
      }
    }
  }
  ctx.draw()
}

function saveImage() {
  uni.canvasToTempFilePath({
    canvasId: 'qrCanvas',
    success: (r) => {
      uni.saveImageToPhotosAlbum({
        filePath: r.tempFilePath,
        success: () => uni.showToast({ title: '已保存到相册', icon: 'success' }),
        fail: () => uni.showToast({ title: '保存失败/无相册权限', icon: 'none' })
      })
    }
  })
}

function print() {
  window.print()
}

onLoad((q: any) => {
  code.value = decodeURIComponent(q.code || '')
  name.value = decodeURIComponent(q.name || '')
  point.value = decodeURIComponent(q.point || '')
  time.value = new Date().toLocaleString()
  setTimeout(draw, 100)
})
</script>

<style lang="scss" scoped>
.label-card {
  background: #fff; border-radius: 16rpx; padding: 40rpx;
  display: flex; flex-direction: column; align-items: center;
  border: 2rpx dashed #1677ff;
}
.org { font-size: 28rpx; color: #1677ff; font-weight: 700; margin-bottom: 24rpx; }
.qr { width: 480rpx; height: 480rpx; }
.code { font-size: 34rpx; font-weight: 700; margin-top: 20rpx; letter-spacing: 2rpx; }
.name { font-size: 28rpx; margin-top: 8rpx; }
.tips { text-align: center; font-size: 24rpx; margin-top: 24rpx; }
.btn { flex: 1; font-size: 28rpx; border-radius: 10rpx; line-height: 80rpx; height: 80rpx; margin: 0; }
.btn.primary { background: #1677ff; color: #fff; }
.btn.ghost { background: #f0f5ff; color: #1677ff; }
</style>
