<template>
  <a-card>
    <a-form layout="inline" class="page-toolbar">
      <a-form-item label="关键字">
        <a-input v-model:value="query.keyword" placeholder="报价编号/标题" allow-clear @press-enter="reload" />
      </a-form-item>
      <a-form-item label="状态">
        <a-select v-model:value="query.status" allow-clear style="width: 130px" @change="reload">
          <a-select-option v-for="d in statusDict" :key="d.itemValue" :value="d.itemValue">
            {{ d.itemLabel }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-button type="primary" @click="router.push('/quote/edit')">编制报价单</a-button>
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
          <a @click="router.push(`/quote/detail/${record.id}`)">{{ record.code }}</a>
        </template>
        <template v-else-if="column.key === 'finalAmount'">¥{{ fmt(record.finalAmount) }}</template>
        <template v-else-if="column.key === 'status'">
          <DictTag code="quote_status" :value="record.status" />
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a @click="router.push(`/quote/detail/${record.id}`)">详情</a>
            <a v-if="record.status === 'DRAFT'" @click="router.push(`/quote/edit/${record.id}`)">编辑</a>
            <a v-if="record.status === 'APPROVED'" @click="toEntrust(record)">生成委托单</a>
            <a v-if="['DRAFT', 'APPROVED'].includes(record.status)" danger @click="voidQuote(record)">作废</a>
          </a-space>
        </template>
      </template>
    </a-table>
  </a-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import DictTag from '@/components/DictTag.vue'
import { quoteApi } from '@/api'
import { loadDict } from '@/composables/useDict'

const router = useRouter()
const loading = ref(false)
const data = ref<any[]>([])
const statusDict = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true })
const query = reactive<any>({ keyword: '', current: 1, size: 10 })

const columns = [
  { title: '报价编号', key: 'code', width: 150 },
  { title: '标题', dataIndex: 'title', ellipsis: true },
  { title: '客户', dataIndex: 'customerName', width: 200 },
  { title: '报价总额', key: 'finalAmount', width: 130 },
  { title: '有效期至', dataIndex: 'validUntil', width: 120 },
  { title: '状态', key: 'status', width: 100 },
  { title: '操作', key: 'action', width: 230 }
]

function fmt(v: any) {
  return Number(v || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}
async function reload() {
  loading.value = true
  try {
    query.current = pagination.current
    query.size = pagination.pageSize
    const res: any = await quoteApi.page(query)
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
function toEntrust(record: any) {
  router.push({ path: '/entrust/edit', query: { quoteId: record.id, customerId: record.customerId } })
}
function voidQuote(record: any) {
  Modal.confirm({
    title: `作废报价单 ${record.code}?`,
    onOk: async () => {
      await quoteApi.void(record.id)
      message.success('已作废')
      reload()
    }
  })
}
onMounted(async () => {
  statusDict.value = await loadDict('quote_status')
  reload()
})
</script>
