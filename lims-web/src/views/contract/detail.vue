<template>
  <div ref="rootEl">
    <a-page-header :title="contract?.code" sub-title="合同详情" @back="router.back()">
      <template #extra>
        <a-space>
          <a-tag :color="statusColor">{{ statusLabel }}</a-tag>
          <a-button @click="exportPdf">导出PDF</a-button>
          <a-button v-if="contract?.fileUrl" type="primary" @click="openEditor">在线编辑正文</a-button>
        </a-space>
      </template>
    </a-page-header>

    <a-card v-if="contract" style="margin-top: 8px">
      <a-descriptions bordered :column="3" size="small">
        <a-descriptions-item label="合同名称" :span="2">{{ contract.name }}</a-descriptions-item>
        <a-descriptions-item label="签订日期">{{ contract.signDate }}</a-descriptions-item>
        <a-descriptions-item label="客户">{{ customerName }}</a-descriptions-item>
        <a-descriptions-item label="合同金额">¥{{ fmt(contract.amount) }}</a-descriptions-item>
        <a-descriptions-item label="税率">{{ contract.taxRate }}%</a-descriptions-item>
        <a-descriptions-item label="生效~到期">
          {{ contract.effectiveDate || '-' }} ~ {{ contract.expiryDate || '-' }}
        </a-descriptions-item>
        <a-descriptions-item label="付款方式">{{ contract.paymentMethod || '-' }}</a-descriptions-item>
        <a-descriptions-item label="我方">{{ contract.ourParty || '-' }}</a-descriptions-item>
        <a-descriptions-item label="对方">{{ contract.counterParty || '-' }}</a-descriptions-item>
        <a-descriptions-item label="付款条款" :span="3">{{ contract.paymentTerms || '-' }}</a-descriptions-item>
      </a-descriptions>
    </a-card>

    <a-card style="margin-top: 12px">
      <a-tabs v-model:activeKey="tab">
        <a-tab-pane key="performance" tab="履约跟踪">
          <a-row :gutter="16">
            <a-col :span="6">
              <a-statistic title="合同总额" :value="Number(perf.amount || 0)" :precision="2" prefix="¥" />
            </a-col>
            <a-col :span="6">
              <a-statistic title="已收款" :value="Number(perf.received || 0)" :precision="2" prefix="¥" :value-style="{ color: '#3f8600' }" />
            </a-col>
            <a-col :span="6">
              <a-statistic title="已开票" :value="Number(perf.invoiced || 0)" :precision="2" prefix="¥" :value-style="{ color: '#722ed1' }" />
            </a-col>
            <a-col :span="6">
              <a-statistic title="待收款" :value="Number(perf.unreceived || 0)" :precision="2" prefix="¥" :value-style="{ color: '#cf1322' }" />
            </a-col>
          </a-row>
          <a-divider />
          <div style="margin-bottom: 10px">
            <a-radio-group v-model:value="payType" button-style="solid" @change="loadPayments" size="small">
              <a-radio-button :value="1">收款计划</a-radio-button>
              <a-radio-button :value="2">实际收款</a-radio-button>
              <a-radio-button :value="3">开票记录</a-radio-button>
            </a-radio-group>
            <a-button type="primary" size="small" style="margin-left: 12px" @click="openPayment">
              新增{{ ['', '收款计划', '实际收款', '开票记录'][payType] }}
            </a-button>
          </div>
          <a-table :data-source="payments" row-key="id" size="small" :pagination="false" :columns="paymentCols">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'date'">
                {{ record.planDate || record.occurDate }}
              </template>
              <template v-else-if="column.key === 'amount'">¥{{ fmt(record.amount) }}</template>
              <template v-else-if="column.key === 'invoiceNo'">{{ record.invoiceNo || '-' }}</template>
            </template>
          </a-table>
        </a-tab-pane>

        <a-tab-pane key="change" tab="变更记录">
          <div style="margin-bottom: 10px">
            <a-button type="primary" @click="openChange">发起合同变更</a-button>
          </div>
          <a-timeline>
            <a-timeline-item v-for="c in changes" :key="c.id" :color="changeColor(c.status)">
              <div>
                <a-tag>{{ c.changeNo }}</a-tag>
                <a-tag>{{ changeTypeText(c.changeType) }}</a-tag>
                <a-tag :color="changeColor(c.status)">{{ changeStatusText(c.status) }}</a-tag>
              </div>
              <div>变更前: {{ c.beforeContent || '-' }}</div>
              <div>变更后: {{ c.afterContent }}</div>
              <div class="sub">原因: {{ c.reason }} · {{ c.createTime }}</div>
            </a-timeline-item>
          </a-timeline>
        </a-tab-pane>

        <a-tab-pane key="approval" tab="审批记录">
          <a-empty v-if="!contract.approvalId" description="草稿尚未提交审批" />
          <a-timeline v-else>
            <a-timeline-item v-for="r in records" :key="r.id" :color="r.action === 'APPROVE' ? 'green' : 'red'">
              <div>
                <a-tag :color="r.action === 'APPROVE' ? 'green' : 'red'">
                  {{ r.action === 'APPROVE' ? '通过' : '驳回' }}
                </a-tag>
                {{ r.nodeName }} · {{ r.approverName }}
              </div>
              <div class="sub">{{ r.comment || '无意见' }} · {{ r.createTime }}</div>
            </a-timeline-item>
          </a-timeline>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <PaymentModal ref="paymentRef" :contract-id="id" @saved="loadAll" />
    <ChangeModal ref="changeRef" :contract-id="id" @saved="loadChanges" />

    <a-modal v-model:open="editorOpen" title="合同正文在线编辑(OnlyOffice)" width="90%" :footer="null" destroy-on-close>
      <OnlyOfficeEditor v-if="editorConfig" :config="editorConfig" />
      <a-alert v-else message="未配置 OnlyOffice 文档服务或合同未上传正文" type="warning" />
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { contractApi, approvalApi } from '@/api'
import { dictMap } from '@/composables/useDict'
import { exportElementToPdf } from '@/utils/pdf'
import PaymentModal from './components/PaymentModal.vue'
import ChangeModal from './components/ChangeModal.vue'
import OnlyOfficeEditor from '@/components/OnlyOfficeEditor.vue'

const route = useRoute()
const router = useRouter()
const id = Number(route.params.id)
const rootEl = ref<HTMLElement>()
const contract = ref<any>(null)
const customerName = ref('')
const perf = ref<any>({})
const payments = ref<any[]>([])
const changes = ref<any[]>([])
const records = ref<any[]>([])
const payType = ref(1)
const tab = ref('performance')
const paymentRef = ref()
const changeRef = ref()

const editorOpen = ref(false)
const editorConfig = ref<any>(null)

const statusLabel = ref('')
const statusColor = computed(() =>
  ({ DRAFT: 'default', APPROVING: 'processing', APPROVED: 'cyan', EXECUTING: 'blue',
    COMPLETED: 'green', CHANGED: 'orange', TERMINATED: 'red' } as any)[contract.value?.status] || 'default')

const paymentCols = computed(() => {
  if (payType.value === 1) {
    return [
      { title: '计划日期', key: 'date' },
      { title: '计划金额', key: 'amount' },
      { title: '备注', dataIndex: 'remark' }
    ]
  }
  if (payType.value === 2) {
    return [
      { title: '到账日期', key: 'date' },
      { title: '到账金额', key: 'amount' },
      { title: '备注', dataIndex: 'remark' }
    ]
  }
  return [
    { title: '开票日期', key: 'date' },
    { title: '开票金额', key: 'amount' },
    { title: '发票号', key: 'invoiceNo' },
    { title: '备注', dataIndex: 'remark' }
  ]
})

function fmt(v: any) {
  return Number(v || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}
function changeTypeText(t: string) {
  return { AMOUNT: '金额变更', PERIOD: '周期变更', TERMS: '条款变更', OTHER: '其他' }[t] || t
}
function changeStatusText(s: string) {
  return { APPROVING: '审批中', APPROVED: '已通过', REJECTED: '已驳回' }[s] || s
}
function changeColor(s: string) {
  return { APPROVING: 'processing', APPROVED: 'green', REJECTED: 'red' }[s] || 'default'
}

async function loadAll() {
  const c: any = await contractApi.detail(id)
  contract.value = c
  const m = await dictMap('contract_status')
  statusLabel.value = m.get(c.status) || c.status
  const listRes: any = await contractApi.page({ current: 1, size: 1, customerId: c.customerId })
  customerName.value = listRes.records?.[0]?.customerName || `客户#${c.customerId}`
  perf.value = await contractApi.performance(id)
  loadPayments()
  loadChanges()
  if (c.approvalId) {
    records.value = (await approvalApi.timeline(c.approvalId)) as any
  }
}
async function loadPayments() {
  payments.value = (await contractApi.payments(id, payType.value)) as any
}
async function loadChanges() {
  changes.value = (await contractApi.changes(id)) as any
}
function openPayment() {
  paymentRef.value?.open(id, payType.value)
}
function openChange() {
  changeRef.value?.open(id)
}
async function openEditor() {
  try {
    editorConfig.value = await contractApi.editorConfig(id, true)
    editorOpen.value = true
  } catch (e) {
    editorConfig.value = null
    editorOpen.value = true
  }
}
async function exportPdf() {
  if (rootEl.value) {
    await exportElementToPdf(rootEl.value, `${contract.value?.code || '合同'}.pdf`)
    message.success('PDF 已生成')
  }
}

onMounted(loadAll)
</script>
<style scoped>
.sub { color: #999; font-size: 12px; }
</style>
