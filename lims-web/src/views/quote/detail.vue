<template>
  <div ref="rootEl">
    <a-page-header :title="detail?.code" @back="router.back()">
      <template #subTitle>
        <DictTag code="quote_status" :value="detail?.status" />
      </template>
      <template #extra>
        <a-space>
          <a-button @click="exportPdf">导出PDF</a-button>
          <a-button @click="exportWord">导出/打印 Word</a-button>
          <a-button v-if="detail?.status === 'DRAFT'" type="primary" @click="submit">提交审批</a-button>
          <a-button v-if="detail?.status === 'APPROVED'" type="primary" @click="toEntrust">生成委托单</a-button>
        </a-space>
      </template>
    </a-page-header>

    <a-card v-if="detail" style="margin-top: 8px" id="quote-print-area">
      <div class="paper">
        <h1 class="doc-title">环境检测报价单</h1>
        <a-descriptions :column="2" size="small" class="doc-head">
          <a-descriptions-item label="报价编号">{{ detail.code }}</a-descriptions-item>
          <a-descriptions-item label="报价日期">{{ detail.createTime?.substring(0, 10) }}</a-descriptions-item>
          <a-descriptions-item label="客户名称">{{ detail.customerName }}</a-descriptions-item>
          <a-descriptions-item label="有效期至">{{ detail.validUntil }}</a-descriptions-item>
          <a-descriptions-item label="加急系数">{{ detail.urgentFactor }}</a-descriptions-item>
          <a-descriptions-item label="计价方式">{{ detail.pricingMode === 'RULE' ? '规则自动计价' : '手工计价' }}</a-descriptions-item>
        </a-descriptions>

        <a-table
          :data-source="detail.items || []"
          :pagination="false"
          size="small"
          row-key="id"
          :columns="columns"
          bordered
        >
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'no'">{{ index + 1 }}</template>
            <template v-else-if="column.key === 'amount'">¥{{ fmt(record.amount) }}</template>
            <template v-else-if="column.key === 'unitPrice'">¥{{ fmt(record.unitPrice) }}</template>
          </template>
        </a-table>

        <div class="sum-line">
          <div>明细合计: ¥{{ fmt(detail.totalAmount) }}</div>
          <div>优惠/调减: ¥{{ fmt(detail.discountAmount) }}</div>
          <div class="final">报价总额(人民币大写): {{ amountCn }} = ¥{{ fmt(detail.finalAmount) }}</div>
        </div>

        <div class="sign">
          <p>报价单位(盖章): 某某环境检测有限公司</p>
          <p>客户确认签字: ________________　　日期: ________________</p>
        </div>
      </div>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import DictTag from '@/components/DictTag.vue'
import { quoteApi, approvalApi } from '@/api'
import { exportElementToPdf } from '@/utils/pdf'

const route = useRoute()
const router = useRouter()
const id = Number(route.params.id)
const rootEl = ref<HTMLElement>()
const detail = ref<any>(null)

const columns = [
  { title: '#', key: 'no', width: 50 },
  { title: '检测项目/参数', dataIndex: 'itemName' },
  { title: '检测标准', dataIndex: 'standardCode', width: 170 },
  { title: '规格/点位说明', dataIndex: 'spec', width: 150 },
  { title: '单位', dataIndex: 'unit', width: 70 },
  { title: '数量', dataIndex: 'qty', width: 70 },
  { title: '单价', key: 'unitPrice', width: 100 },
  { title: '金额', key: 'amount', width: 110 }
]

function fmt(v: any) {
  return Number(v || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}
const amountCn = computed(() => '(以系统生成 Word 报价单中的大写金额为准)')

async function load() {
  detail.value = await quoteApi.detail(id)
}
async function submit() {
  await quoteApi.submit(id)
  message.success('已提交审批')
  load()
}
function toEntrust() {
  router.push({
    path: '/entrust/edit',
    query: { quoteId: id, customerId: detail.value.customerId }
  })
}
async function exportPdf() {
  if (rootEl.value) {
    await exportElementToPdf(rootEl.value, `${detail.value.code}-报价单.pdf`)
  }
}
function exportWord() {
  // 带鉴权头下载 blob
  import('axios').then(async (axios) => {
    const token = localStorage.getItem('lims_token')
    const resp = await axios.default.get(quoteApi.exportWordUrl(id), {
      responseType: 'blob',
      headers: { Authorization: `Bearer ${token}` }
    })
    const url = URL.createObjectURL(resp.data)
    const a = document.createElement('a')
    a.href = url
    a.download = `${detail.value.code}-报价单.docx`
    a.click()
    URL.revokeObjectURL(url)
  })
}

onMounted(load)
</script>
<style scoped>
.paper { padding: 8px 16px; }
.doc-title { text-align: center; margin: 4px 0 18px; font-size: 22px; letter-spacing: 4px; }
.sum-line { margin: 14px 0; text-align: right; line-height: 1.9; }
.final { font-weight: 700; color: #f5222d; }
.sign { margin-top: 40px; line-height: 2.2; }
</style>
