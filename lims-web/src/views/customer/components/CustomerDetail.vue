<template>
  <a-drawer
    v-model:open="open"
    :title="customer?.name"
    width="860px"
    @update:open="(v: boolean) => (open = v)"
  >
    <a-tabs v-model:activeKey="tab">
      <a-tab-pane key="base" tab="基本信息">
        <a-descriptions bordered :column="2" size="small" v-if="customer">
          <a-descriptions-item label="客户编号">{{ customer.code }}</a-descriptions-item>
          <a-descriptions-item label="分级">
            <a-tag :color="levelColor">{{ customer.customerLevel }}级</a-tag>
            信用分 {{ customer.creditScore }}
          </a-descriptions-item>
          <a-descriptions-item label="客户类型">{{ typeText }}</a-descriptions-item>
          <a-descriptions-item label="行业">{{ customer.industry || '-' }}</a-descriptions-item>
          <a-descriptions-item label="联系人">{{ customer.contactPerson || '-' }}</a-descriptions-item>
          <a-descriptions-item label="电话">{{ customer.contactPhone || '-' }}</a-descriptions-item>
          <a-descriptions-item label="地区" :span="2">
            {{ [customer.province, customer.city, customer.district, customer.address].filter(Boolean).join(' ') }}
          </a-descriptions-item>
          <a-descriptions-item label="授信额度">¥{{ fmt(customer.creditLimit) }}</a-descriptions-item>
          <a-descriptions-item label="账期">{{ customer.creditPeriod }} 天</a-descriptions-item>
          <a-descriptions-item label="信用代码">{{ customer.taxNo || '-' }}</a-descriptions-item>
          <a-descriptions-item label="来源">{{ customer.source || '-' }}</a-descriptions-item>
          <a-descriptions-item label="开户行">{{ customer.bankName || '-' }}</a-descriptions-item>
          <a-descriptions-item label="银行账号">{{ customer.bankAccount || '-' }}</a-descriptions-item>
          <a-descriptions-item label="备注" :span="2">{{ customer.remark || '-' }}</a-descriptions-item>
        </a-descriptions>
        <div style="margin-top: 16px; text-align: right">
          <a-button @click="emit('edit', customer?.id)" type="primary">编辑客户</a-button>
        </div>
      </a-tab-pane>

      <a-tab-pane key="qual" tab="资质文件">
        <div style="margin-bottom: 12px">
          <a-button type="primary" @click="openQual()">新增资质</a-button>
        </div>
        <a-table :data-source="quals" row-key="id" size="small" :pagination="false" :columns="qualCols">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'validStatus'">
              <a-tag :color="record.validStatus === 1 ? 'green' : record.validStatus === 2 ? 'orange' : 'red'">
                {{ ['', '有效', '30天内到期', '已过期'][record.validStatus] }}
              </a-tag>
            </template>
            <template v-else-if="column.key === 'file'">
              <a v-if="record.fileUrl" :href="withHost(record.fileUrl)" target="_blank">{{ record.fileName || '查看文件' }}</a>
              <span v-else>-</span>
            </template>
            <template v-else-if="column.key === 'action'">
              <a-popconfirm title="确定删除?" @confirm="delQual(record)">
                <a-button danger size="small" type="link">删除</a-button>
              </a-popconfirm>
            </template>
          </template>
        </a-table>
      </a-tab-pane>

      <a-tab-pane key="credit" tab="信用管理">
        <a-alert
          message="调整信用分将自动联动客户分级(≥85 A / ≥70 B / ≥50 C / <50 D)"
          type="info"
          show-icon
          style="margin-bottom: 12px"
        />
        <a-space style="margin-bottom: 12px">
          <a-button type="primary" @click="openCredit(1)">调整信用分</a-button>
          <a-button @click="openCredit(2)">调整授信额度</a-button>
          <a-button @click="openCredit(3)">调整账期</a-button>
        </a-space>
        <a-timeline>
          <a-timeline-item v-for="l in creditLogs" :key="l.id" :color="l.changeType === 4 ? 'red' : 'blue'">
            <div>{{ creditTypeText(l.changeType) }}: {{ l.beforeValue }} → {{ l.afterValue }}</div>
            <div class="sub">{{ l.operatorName }} · {{ l.createTime }} · {{ l.reason }}</div>
          </a-timeline-item>
        </a-timeline>
      </a-tab-pane>

      <a-tab-pane key="follow" tab="跟进记录">
        <a-textarea v-model:value="followContent" :rows="2" placeholder="本次跟进内容..." />
        <div style="margin: 8px 0; text-align: right">
          <a-button type="primary" @click="addFollow">添加跟进</a-button>
        </div>
        <a-timeline>
          <a-timeline-item v-for="f in follows" :key="f.id">
            <div>{{ f.content }}</div>
            <div class="sub">{{ f.operatorName }} · {{ f.followType || '' }} · {{ f.createTime }}</div>
          </a-timeline-item>
        </a-timeline>
      </a-tab-pane>

      <a-tab-pane key="pool" tab="公海流转">
        <a-timeline>
          <a-timeline-item v-for="l in poolLogs" :key="l.id">
            <div>
              <a-tag :color="actionColor(l.action)">{{ actionText(l.action) }}</a-tag>
              {{ l.remark || '' }}
            </div>
            <div class="sub">{{ l.createTime }}</div>
          </a-timeline-item>
        </a-timeline>
      </a-tab-pane>
    </a-tabs>

    <QualEdit ref="qualRef" :customer-id="customerId" @saved="loadQuals" />
  </a-drawer>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { message } from 'ant-design-vue'
import { customerApi } from '@/api'
import { useUserStore } from '@/stores/user'
import QualEdit from './QualEdit.vue'

const emit = defineEmits<{ (e: 'edit', id: number): void }>()
const userStore = useUserStore()
const open = ref(false)
const tab = ref('base')
const customerId = ref<number>()
const customer = ref<any>(null)
const quals = ref<any[]>([])
const creditLogs = ref<any[]>([])
const follows = ref<any[]>([])
const poolLogs = ref<any[]>([])
const followContent = ref('')
const qualRef = ref()

const levelColor = computed(() =>
  ({ A: 'red', B: 'orange', C: 'blue', D: 'default' }[customer.value?.customerLevel] || 'default')
)
const typeText = computed(() => ({ 10: '企业', 20: '政府事业单位', 30: '个人' } as any)[customer.value?.customerType])

function withHost(url: string) {
  return url?.startsWith('http') ? url : `/api${url}`
}
function fmt(v: any) {
  return Number(v || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}
function creditTypeText(t: number) {
  return { 1: '信用分', 2: '授信额度', 3: '账期', 4: '冻结/解冻' }[t] || '变更'
}
function actionText(a: string) {
  return { CLAIM: '认领', RELEASE: '退回公海', TRANSFER: '分配/转交' }[a] || a
}
function actionColor(a: string) {
  return { CLAIM: 'green', RELEASE: 'orange', TRANSFER: 'blue' }[a] || 'default'
}

const qualCols = [
  { title: '资质类型', dataIndex: 'qualType' },
  { title: '证书号', dataIndex: 'certNo' },
  { title: '有效期至', dataIndex: 'validTo' },
  { title: '状态', key: 'validStatus' },
  { title: '文件', key: 'file' },
  { title: '操作', key: 'action', width: 80 }
]

async function openModal(id: number) {
  customerId.value = id
  open.value = true
  tab.value = 'base'
  customer.value = await customerApi.detail(id)
  loadQuals()
  creditLogs.value = (await customerApi.creditLogs(id)) as any
  follows.value = (await customerApi.follows(id)) as any
  poolLogs.value = (await customerApi.poolLogs(id)) as any
}
async function loadQuals() {
  quals.value = (await customerApi.qualifications(customerId.value!)) as any
}
function openQual() {
  qualRef.value?.open(customerId.value!)
}
async function delQual(record: any) {
  await customerApi.deleteQualification(record.id)
  message.success('已删除')
  loadQuals()
}
function openCredit(changeType: number) {
  let target = ''
  let reason = ''
  const titles: any = { 1: '调整信用分(0-100)', 2: '调整授信额度(元)', 3: '调整账期(天)' }
  import('ant-design-vue').then(({ Modal }) => {
    Modal.confirm({
      title: titles[changeType],
      content: (h: any) =>
        h('div', [
          h('input', {
            class: 'ant-input',
            placeholder: '目标值',
            type: 'number',
            onInput: (e: any) => (target = e.target.value)
          }),
          h('input', {
            class: 'ant-input',
            style: 'margin-top:8px',
            placeholder: '调整原因(必填)',
            onInput: (e: any) => (reason = e.target.value)
          })
        ]),
      onOk: async () => {
        if (!target || !reason) {
          message.warning('请填写目标值和原因')
          throw new Error()
        }
        await customerApi.adjustCredit({
          customerId: customerId.value,
          changeType,
          targetValue: Number(target),
          reason
        })
        message.success('调整成功')
        customer.value = await customerApi.detail(customerId.value!)
        creditLogs.value = (await customerApi.creditLogs(customerId.value!)) as any
      }
    })
  })
}
async function addFollow() {
  if (!followContent.value) return
  await customerApi.addFollow({ customerId: customerId.value, content: followContent.value, followType: '电话' })
  followContent.value = ''
  message.success('已添加')
  follows.value = (await customerApi.follows(customerId.value!)) as any
}

defineExpose({ open: openModal })
</script>
<style scoped>
.sub { color: #999; font-size: 12px; }
</style>
