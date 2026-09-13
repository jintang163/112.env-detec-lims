<template>
  <view class="container">
    <view v-if="task" class="card">
      <view class="row between">
        <text class="code">{{ task.code }}</text>
        <text class="tag tag-orange">待交接 {{ samples.length }} 个</text>
      </view>
      <view class="muted">{{ planTitle }}</view>
    </view>

    <view class="card">
      <view class="section-title">样品清点</view>
      <view v-for="s in samples" :key="s.id" class="kv">
        <text class="v">{{ s.sampleCode }} <text v-if="s.isQc === 1" class="tag tag-purple">质控</text></text>
        <text class="k">{{ s.pointName }} · {{ s.sampleName }}</text>
      </view>
      <view v-if="!samples.length" class="muted">该任务暂无可交接样品</view>
      <view class="field" style="margin-top: 16rpx">
        <text class="label">样品状态</text>
      </view>
      <radio-group @change="(e: any) => (form.sampleStatus = statusOptions[Number(e.detail.value)])">
        <label v-for="(t, i) in statusOptions" :key="i" class="radio-label">
          <radio :value="String(i)" :checked="form.sampleStatus === t" color="#1677ff" /> {{ t }}
        </label>
      </radio-group>
      <textarea v-model="form.remark" class="textarea" placeholder="异常情况说明(可选)" />
    </view>

    <view class="card">
      <view class="section-title">采样员签名</view>
      <canvas
        canvas-id="sigCanvas"
        class="sig"
        disable-scroll
        @touchstart="startDraw"
        @touchmove="onDraw"
        @touchend="endDraw"
      />
      <view class="row" style="margin-top: 12rpx; justify-content: flex-end">
        <text class="link danger" @tap="clearSig">清除重签</text>
      </view>
    </view>

    <view style="height: 160rpx"></view>
    <view class="footer">
      <button class="btn primary" :loading="submitting" :disabled="!samples.length" @tap="submit">
        确认移交({{ samples.length }}个样品)
      </button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { api, uploadFile } from '@/utils/request'

const taskId = ref(0)
const task = ref<any>(null)
const planTitle = ref('')
const samples = ref<any[]>([])
const submitting = ref(false)
const statusOptions = ['完好', '部分破损', '温度异常', '其他异常(备注说明)']
const form = reactive({ sampleStatus: '完好', remark: '' })

let ctx: any = null
let drawing = false
let hasSig = false

function initCanvas() {
  ctx = uni.createCanvasContext('sigCanvas')
  ctx.setStrokeStyle('#1677ff')
  ctx.setLineWidth(3)
  ctx.setLineCap('round')
  ctx.draw()
}
function touchPos(e: any) {
  const t = e.touches[0]
  return { x: t.x, y: t.y }
}
function startDraw(e: any) {
  drawing = true
  const p = touchPos(e)
  ctx.beginPath()
  ctx.moveTo(p.x, p.y)
}
function onDraw(e: any) {
  if (!drawing) return
  const p = touchPos(e)
  ctx.lineTo(p.x, p.y)
  ctx.stroke()
  ctx.draw(true)
  hasSig = true
}
function endDraw() {
  drawing = false
}
function clearSig() {
  ctx.draw()
  hasSig = false
}

async function submit() {
  if (!hasSig) return uni.showToast({ title: '请先签名确认', icon: 'none' })
  uni.showModal({
    title: '确认移交',
    content: `共 ${samples.value.length} 个样品, 提交后等待样品管理员接收`,
    success: async (r) => {
      if (!r.confirm) return
      submitting.value = true
      try {
        const sigFileId = await new Promise<any>((resolve, reject) => {
          uni.canvasToTempFilePath({
            canvasId: 'sigCanvas',
            success: async (res) => {
              try {
                const f: any = await uploadFile(res.tempFilePath, { bizType: 'HANDOVER_SIG' })
                resolve(f.id)
              } catch (e) { reject(e) }
            },
            fail: reject
          })
        })
        await api.createHandover({
          taskId: taskId.value,
          sampleStatus: form.sampleStatus + (form.remark ? ':' + form.remark : ''),
          sigFileId
        })
        uni.showToast({ title: '交接单已提交', icon: 'success' })
        setTimeout(() => uni.navigateBack(), 700)
      } catch (e) {
        uni.showToast({ title: '提交失败, 请检查网络', icon: 'none' })
      } finally {
        submitting.value = false
      }
    }
  })
}

onLoad(async (q: any) => {
  taskId.value = Number(q.taskId)
  const d: any = await api.taskDetail(taskId.value)
  task.value = d.task
  planTitle.value = d.plan?.title
  samples.value = (d.samples || [])
    .map((x: any) => x.sample)
    .filter((s: any) => s.status === 'COLLECTED')
  setTimeout(initCanvas, 100)
})
</script>

<style lang="scss" scoped>
.code { font-weight: 700; color: #1677ff; font-size: 28rpx; }
.section-title { font-weight: 700; font-size: 28rpx; margin-bottom: 12rpx; }
.kv { display: flex; justify-content: space-between; padding: 10rpx 0; font-size: 26rpx; }
.kv .k { color: #666; }
.field { display: flex; align-items: center; }
.label { color: #666; font-size: 26rpx; }
.radio-label { display: inline-flex; align-items: center; margin: 12rpx 24rpx 0 0; font-size: 26rpx; }
.textarea {
  width: 100%; box-sizing: border-box; background: #f7f8fa;
  border-radius: 8rpx; padding: 16rpx 20rpx; height: 120rpx;
  margin-top: 16rpx; font-size: 26rpx;
}
.sig { width: 100%; height: 300rpx; background: #fafafa; border: 2rpx dashed #d9d9d9; border-radius: 8rpx; }
.link { color: #1677ff; font-size: 26rpx; }
.link.danger { color: #f5222d; }
.tag-purple { background: #f9f0ff; color: #722ed1; }
.footer {
  position: fixed; left: 0; right: 0; bottom: 0;
  background: #fff; padding: 16rpx 20rpx;
  box-shadow: 0 -2rpx 12rpx rgba(0, 0, 0, 0.06);
}
.btn { font-size: 28rpx; border-radius: 10rpx; line-height: 84rpx; height: 84rpx; }
.btn.primary { background: #1677ff; color: #fff; }
</style>
