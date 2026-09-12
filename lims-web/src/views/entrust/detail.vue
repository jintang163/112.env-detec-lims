<template>
  <div ref="rootEl">
    <a-page-header :title="detail?.order.code" @back="router.back()">
      <template #subTitle>
        <a-tag v-if="detail?.order.urgency === 'URGENT'" color="red" class="urgent-tag">加急</a-tag>
        <DictTag code="entrust_status" :value="detail?.order.status" />
      </template>
      <template #extra>
        <a-space>
          <a-button @click="exportPdf">导出PDF</a-button>
          <a-button
            v-if="['DRAFT', 'REVIEW_REJECTED'].includes(detail?.order.status)"
            type="primary"
            @click="router.push(`/entrust/edit/${id}`)"
          >编辑</a-button>
          <a-button
            v-if="['DRAFT', 'REVIEW_REJECTED'].includes(detail?.order.status)"
            type="primary"
            @click="submitReview"
          >提交合同评审</a-button>
          <a-button
            v-if="detail?.order.status === 'ACCEPTED'"
            type="primary"
            @click="progress('START_SAMPLING')"
          >开始采样</a-button>
          <a-button
            v-if="detail?.order.status === 'SAMPLING'"
            type="primary"
            @click="progress('START_TESTING')"
          >采样完成→检测</a-button>
          <a-button
            v-if="detail?.order.status === 'TESTING'"
            type="primary"
            @click="progress('START_REPORT')"
          >检测完成→报告</a-button>
          <a-button
            v-if="detail?.order.status === 'REPORTING'"
            type="primary"
            @click="progress('COMPLETE')"
          >完成归档</a-button>
        </a-space>
      </template>
    </a-page-header>

    <a-card v-if="detail" style="margin-top: 8px">
      <a-steps :current="stepIndex" size="small" :status="stepStatus" style="margin-bottom: 18px">
        <a-step v-for="s in STEPS" :key="s.key" :title="s.title" />
      </a-steps>

      <a-descriptions bordered :column="3" size="small">
        <a-descriptions-item label="项目名称" :span="2">{{ detail.order.title }}</a-descriptions-item>
        <a-descriptions-item label="委托类型">{{ entrustTypeText }}</a-descriptions-item>
        <a-descriptions-item label="客户">{{ detail.customerName }}</a-descriptions-item>
        <a-descriptions-item label="合同">
          <a v-if="detail.contractCode" @click="router.push(`/contract/detail/${detail.order.contractId}`)">
            {{ detail.contractCode }}
          </a>
          <span v-else>-</span>
        </a-descriptions-item>
        <a-descriptions-item label="报价单">{{ detail.quoteCode || '-' }}</a-descriptions-item>
        <a-descriptions-item label="现场联系人">{{ detail.order.contactPerson || '-' }} {{ detail.order.contactPhone || '' }}</a-descriptions-item>
        <a-descriptions-item label="采样地址" :span="2">
          {{ region }} {{ detail.order.samplingAddress || '' }}
          <span v-if="detail.order.lng" class="sub">
            ({{ Number(detail.order.lng).toFixed(5) }}, {{ Number(detail.order.lat).toFixed(5) }})
          </span>
        </a-descriptions-item>
        <a-descriptions-item label="计划采样">{{ detail.order.plannedSamplingTime || '-' }}</a-descriptions-item>
        <a-descriptions-item label="期望报告日">
          <span :style="nearDue ? 'color:#f5222d;font-weight:600' : ''">{{ detail.order.expectedReportDate }}</span>
        </a-descriptions-item>
        <a-descriptions-item label="应收金额">
          ¥{{ fmt(detail.order.totalAmount) }}
          <a-tag v-if="Number(detail.order.adjustedAmount) !== Number(detail.order.totalAmount)" color="orange">
            调账后 ¥{{ fmt(detail.order.adjustedAmount) }}
          </a-tag>
        </a-descriptions-item>
        <a-descriptions-item label="分包">
          <a-tag v-if="detail.order.hasSubcontract" color="orange">含分包项目</a-tag>
          <span v-else>无</span>
        </a-descriptions-item>
        <a-descriptions-item label="评审人">{{ detail.reviewerName || '-' }}</a-descriptions-item>
        <a-descriptions-item label="评审时间">{{ detail.order.reviewTime || '-' }}</a-descriptions-item>
        <a-descriptions-item label="评审意见" :span="3">{{ detail.order.reviewOpinion || '-' }}</a-descriptions-item>
      </a-descriptions>

      <a-space style="margin-top: 14px">
        <a-button size="small" @click="toggleUrgent">
          {{ detail.order.urgency === 'URGENT' ? '取消加急' : '设为加急' }}
        </a-button>
        <a-button size="small" @click="adjustOpen = true">调账申请</a-button>
        <a-button size="small" @click="subOpen = true">分包登记</a-button>
        <a-button size="small" danger @click="cancelOrder" v-if="detail.order.status !== 'COMPLETED'">取消委托</a-button>
      </a-space>
    </a-card>

    <a-card style="margin-top: 12px">
      <a-tabs v-model:activeKey="tab">
        <a-tab-pane key="items" :tab="`检测项目(${detail?.items.length || 0})`">
          <a-table :data-source="detail?.items || []" row-key="id" size="small" :pagination="false">
            <a-table-column title="#" :width="50">
              <template #default="{ index }">{{ index + 1 }}</template>
            </a-table-column>
            <a-table-column title="检测项目" data-index="itemName" />
            <a-table-column title="检测标准" key="std">
              <template #default="{ record }">
                {{ record.standardCode }}
                <span class="sub">{{ record.standardName }}</span>
              </template>
            </a-table-column>
            <a-table-column title="样品" data-index="sampleName" :width="110" />
            <a-table-column title="数量" data-index="qty" :width="70" />
            <a-table-column title="金额" key="amount" :width="110">
              <template #default="{ record }">¥{{ fmt(record.amount) }}</template>
            </a-table-column>
            <a-table-column title="分包" :width="80">
              <template #default="{ record }">
                <a-tag v-if="record.isSubcontract" color="orange">分包</a-tag>
                <span v-else>-</span>
              </template>
            </a-table-column>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="points" :tab="`采样点位(${detail?.points.length || 0})`">
          <a-table :data-source="detail?.points || []" row-key="id" size="small" :pagination="false">
            <a-table-column title="#" :width="50">
              <template #default="{ index }">{{ index + 1 }}</template>
            </a-table-column>
            <a-table-column title="点位名称" data-index="name" />
            <a-table-column title="经度 BD-09" data-index="lng" :width="150" />
            <a-table-column title="纬度 BD-09" data-index="lat" :width="150" />
            <a-table-column title="描述" data-index="addrDesc" />
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="timeline" tab="状态时间线">
          <a-timeline>
            <a-timeline-item v-for="l in logs" :key="l.id" :color="logColor(l.toStatus)">
              <div>{{ statusText(l.toStatus) }} <span class="sub">{{ l.action }}</span></div>
              <div class="sub">{{ l.operatorName }} · {{ l.createTime }} · {{ l.remark || '' }}</div>
            </a-timeline-item>
          </a-timeline>
        </a-tab-pane>

        <a-tab-pane key="adjust" tab="调账记录">
          <a-timeline>
            <a-timeline-item v-for="a in adjustments" :key="a.id" :color="adjColor(a.status)">
              <div>
                <a-tag :color="a.adjustType === 1 ? 'red' : 'green'">{{ a.adjustType === 1 ? '调增' : '调减' }}</a-tag>
                ¥{{ fmt(a.beforeAmount) }} → ¥{{ fmt(a.afterAmount) }}
                <a-tag :color="adjColor(a.status)">{{ adjText(a.status) }}</a-tag>
              </div>
              <div class="sub">{{ a.reason }} · {{ a.createTime }}</div>
            </a-timeline-item>
          </a-timeline>
        </a-tab-pane>

        <a-tab-pane key="sub" tab="分包记录">
          <a-timeline>
            <a-timeline-item v-for="s in subcontracts" :key="s.id" :color="adjColor(s.status)">
              <div>
                → {{ s.subcontractor }}
                <a-tag color="purple">CMA: {{ s.qualCert }}</a-tag>
                <a-tag :color="adjColor(s.status)">{{ adjText(s.status) }}</a-tag>
              </div>
              <div class="sub">原因: {{ s.reason }} · 费用 ¥{{ fmt(s.amount) }} · {{ s.createTime }}</div>
            </a-timeline-item>
          </a-timeline>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <!-- 调账 -->
    <a-modal v-model:open="adjustOpen" title="委托单调账" @ok="submitAdjust" destroy-on-close>
      <a-form layout="vertical">
        <a-form-item label="调账类型">
          <a-radio-group v-model:value="adjustForm.adjustType">
            <a-radio :value="1">调增</a-radio>
            <a-radio :value="2">调减/优惠</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="金额(元)">
          <a-input-number v-model:value="adjustForm.adjustAmount" :min="0" style="width: 100%" />
        </a-form-item>
        <a-form-item label="原因(必填)">
          <a-textarea v-model:value="adjustForm.reason" :rows="2" />
        </a-form-item>
        <a-alert message="调账需市场主管审批 + 财务复核,通过后生效" type="info" show-icon />
      </a-form>
    </a-modal>

    <!-- 分包 -->
    <a-modal v-model:open="subOpen" title="分包登记" @ok="submitSub" destroy-on-close width="560px">
      <a-form layout="vertical">
        <a-form-item label="分包方(合作实验室)" required>
          <a-input v-model:value="subForm.subcontractor" />
        </a-form-item>
        <a-form-item label="分包方CMA资质证书号" required>
          <a-input v-model:value="subForm.qualCert" />
        </a-form-item>
        <a-form-item label="分包费用(元)">
          <a-input-number v-model:value="subForm.amount" :min="0" style="width: 100%" />
        </a-form-item>
        <a-form-item label="分包原因" required>
          <a-textarea v-model:value="subForm.reason" :rows="2" placeholder="资质能力不足/设备占用/工期..." />
        </a-form-item>
        <a-alert message="需检测主管审批分包方资质后生效, 通过后自动标记委托单含分包" type="warning" show-icon />
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { message, Modal } from 'ant-design-vue'
import DictTag from '@/components/DictTag.vue'
import { entrustApi } from '@/api'
import { dictMap } from '@/composables/useDict'
import { exportElementToPdf } from '@/utils/pdf'

const route = useRoute()
const router = useRouter()
const id = Number(route.params.id)
const rootEl = ref<HTMLElement>()
const detail = ref<any>(null)
const logs = ref<any[]>([])
const adjustments = ref<any[]>([])
const subcontracts = ref<any[]>([])
const tab = ref('items')
const adjustOpen = ref(false)
const subOpen = ref(false)
const statusMap = ref(new Map<string, string>())

const adjustForm = reactive<any>({ adjustType: 2, adjustAmount: 0, reason: '' })
const subForm = reactive<any>({ subcontractor: '', qualCert: '', amount: 0, reason: '' })

const STEPS = [
  { key: 'DRAFT', title: '草稿' },
  { key: 'REVIEWING', title: '合同评审' },
  { key: 'ACCEPTED', title: '已受理' },
  { key: 'SAMPLING', title: '采样' },
  { key: 'TESTING', title: '检测' },
  { key: 'REPORTING', title: '报告' },
  { key: 'COMPLETED', title: '完成' }
]

const stepIndex = computed(() => {
  const s = detail.value?.order.status
  if (s === 'REVIEW_REJECTED') return 1
  if (s === 'CANCELLED') return 0
  const i = STEPS.findIndex((x) => x.key === s)
  return i < 0 ? 0 : i
})
const stepStatus = computed(() =>
  detail.value?.order.status === 'REVIEW_REJECTED' ? 'error' : 'finish'
)
const nearDue = computed(() => {
  const d = detail.value?.order.expectedReportDate
  return d && dayjs(d).diff(dayjs(), 'day') <= 3 && detail.value?.order.status !== 'COMPLETED'
})
const entrustTypeText = computed(
  () => ({ ENTRUST: '委托检测', SUPERVISION: '监督检测', SPOT: '抽查检测' } as any)[detail.value?.order.entrustType]
)
const region = computed(() =>
  [detail.value?.order.province, detail.value?.order.city, detail.value?.order.district].filter(Boolean).join(' ')
)

function fmt(v: any) {
  return Number(v || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}
function statusText(s: string) {
  return statusMap.value.get(s) || s
}
function logColor(s: string) {
  return ({ COMPLETED: 'green', CANCELLED: 'gray', REVIEW_REJECTED: 'red' } as any)[s] || 'blue'
}
function adjColor(s: string) {
  return ({ APPROVED: 'green', REJECTED: 'red', APPROVING: 'processing' } as any)[s] || 'default'
}
function adjText(s: string) {
  return ({ APPROVED: '已通过', REJECTED: '已驳回', APPROVING: '审批中' } as any)[s]
}

async function load() {
  detail.value = await entrustApi.detail(id)
  logs.value = (await entrustApi.timeline(id)) as any
  adjustments.value = (await entrustApi.adjustments(id)) as any
  subcontracts.value = (await entrustApi.subcontracts(id)) as any
}

async function submitReview() {
  await entrustApi.submitReview(id)
  message.success('已提交合同评审')
  load()
}
async function progress(action: string) {
  await entrustApi.progress(id, action)
  message.success('状态已推进')
  load()
}
function toggleUrgent() {
  const urgent = detail.value.order.urgency !== 'URGENT'
  Modal.confirm({
    title: urgent ? '设为加急单?' : '取消加急?',
    onOk: async () => {
      await entrustApi.urgent(id, urgent)
      message.success('已更新')
      load()
    }
  })
}
function cancelOrder() {
  let reason = ''
  Modal.confirm({
    title: '确认取消该委托单?',
    content: (h: any) =>
      h('textarea', {
        class: 'ant-input',
        rows: 2,
        placeholder: '取消原因',
        onInput: (e: any) => (reason = e.target.value)
      }),
    onOk: async () => {
      await entrustApi.cancel(id, reason || '客户取消')
      message.success('已取消')
      load()
    }
  })
}
async function submitAdjust() {
  if (!adjustForm.reason) return message.warning('请填写原因')
  await entrustApi.applyAdjustment({ orderId: id, ...adjustForm })
  message.success('调账已提交审批(主管+财务)')
  adjustOpen.value = false
  load()
}
async function submitSub() {
  if (!subForm.subcontractor || !subForm.qualCert || !subForm.reason) {
    return message.warning('请完善分包方、资质号与原因')
  }
  await entrustApi.applySubcontract({ orderId: id, ...subForm })
  message.success('分包已提交资质审批')
  subOpen.value = false
  load()
}
async function exportPdf() {
  if (rootEl.value) {
    await exportElementToPdf(rootEl.value, `${detail.value?.order.code || '委托单'}.pdf`)
  }
}

onMounted(async () => {
  statusMap.value = await dictMap('entrust_status')
  load()
})
</script>
<style scoped>
.sub { color: #999; font-size: 12px; margin-left: 6px; }
</style>
