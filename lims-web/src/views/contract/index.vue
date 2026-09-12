<template>
  <a-card>
    <a-form layout="inline" class="page-toolbar">
      <a-form-item label="关键字">
        <a-input v-model:value="query.keyword" placeholder="合同名称/编号" allow-clear @press-enter="reload" />
      </a-form-item>
      <a-form-item label="状态">
        <a-select v-model:value="query.status" allow-clear style="width: 140px" @change="reload">
          <a-select-option v-for="d in statusDict" :key="d.itemValue" :value="d.itemValue">
            {{ d.itemLabel }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-button type="primary" @click="openEdit()">新建合同</a-button>
    </a-form>

    <a-table
      :data-source="data"
      :columns="columns"
      row-key="id"
      :loading="loading"
      :pagination="pagination"
      @change="onPage"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'code'">
          <a @click="router.push(`/contract/detail/${record.id}`)">{{ record.code }}</a>
        </template>
        <template v-else-if="column.key === 'customerName'">{{ record.customerName }}</template>
        <template v-else-if="column.key === 'amount'">¥{{ fmt(record.amount) }}</template>
        <template v-else-if="column.key === 'received'">
          <a-progress
            :percent="receiveRate(record)"
            size="small"
            :format="() => '¥' + fmt(record.receivedAmount)"
          />
        </template>
        <template v-else-if="column.key === 'status'">
          <DictTag code="contract_status" :value="record.status" />
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a @click="router.push(`/contract/detail/${record.id}`)">详情</a>
            <a v-if="record.status === 'DRAFT'" @click="submit(record)">提交审批</a>
            <a v-if="['APPROVED', 'CHANGED'].includes(record.status)" @click="startExec(record)">开始履约</a>
          </a-space>
        </template>
      </template>
    </a-table>

    <ContractEdit ref="editRef" @saved="reload" />
  </a-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import DictTag from '@/components/DictTag.vue'
import { contractApi } from '@/api'
import { loadDict } from '@/composables/useDict'
import ContractEdit from './components/ContractEdit.vue'

const router = useRouter()
const loading = ref(false)
const data = ref<any[]>([])
const statusDict = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true })
const query = reactive<any>({ keyword: '', current: 1, size: 10 })

const columns = [
  { title: '合同编号', key: 'code', width: 150 },
  { title: '合同名称', dataIndex: 'name', ellipsis: true },
  { title: '客户', key: 'customerName', width: 200 },
  { title: '金额', key: 'amount', width: 130 },
  { title: '回款', key: 'received', width: 180 },
  { title: '签订日期', dataIndex: 'signDate', width: 120 },
  { title: '状态', key: 'status', width: 110 },
  { title: '操作', key: 'action', width: 170 }
]

function fmt(v: any) {
  return Number(v || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}
function receiveRate(r: any) {
  if (!r.amount) return 0
  return Math.min(100, Math.round((Number(r.receivedAmount || 0) / Number(r.amount)) * 100))
}

async function reload() {
  loading.value = true
  try {
    query.current = pagination.current
    query.size = pagination.pageSize
    const res: any = await contractApi.page(query)
    data.value = res.records
    pagination.total = res.total
  } finally {
    loading.value = false
  }
}
function onPage(p: any) {
  pagination.current = p.current
  pagination.pageSize = p.pageSize
  reload()
}
const editRef = ref()
function openEdit() {
  editRef.value?.open()
}
async function submit(record: any) {
  await contractApi.submit(record.id)
  message.success('已提交审批')
  reload()
}
function startExec(record: any) {
  Modal.confirm({
    title: '确认开始履约?',
    onOk: async () => {
      await contractApi.changeStatus(record.id, 'EXECUTING')
      message.success('合同进入履约中')
      reload()
    }
  })
}

onMounted(async () => {
  statusDict.value = await loadDict('contract_status')
  reload()
})
</script>
