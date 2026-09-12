<template>
  <a-card>
    <a-form layout="inline" class="page-toolbar">
      <a-form-item label="关键字">
        <a-input v-model:value="query.keyword" placeholder="委托单号/项目名称" allow-clear @press-enter="reload" />
      </a-form-item>
      <a-form-item label="状态">
        <a-select v-model:value="query.status" allow-clear style="width: 130px" @change="reload">
          <a-select-option v-for="d in statusDict" :key="d.itemValue" :value="d.itemValue">
            {{ d.itemLabel }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="紧急">
        <a-select v-model:value="query.urgency" allow-clear style="width: 100px" @change="reload">
          <a-select-option value="URGENT">加急</a-select-option>
          <a-select-option value="NORMAL">普通</a-select-option>
        </a-select>
      </a-form-item>
      <a-checkbox v-model:checked="query.mine" @change="reload">只看我创建的</a-checkbox>
      <a-button type="primary" @click="router.push('/entrust/edit')">新建委托单</a-button>
    </a-form>

    <a-table
      :data-source="data"
      :columns="columns"
      :row-key="(r: any) => r.order.id"
      :loading="loading"
      :pagination="pagination"
      @change="onPage"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'code'">
          <a @click="router.push(`/entrust/detail/${record.order.id}`)">{{ record.order.code }}</a>
          <a-tag v-if="record.order.urgency === 'URGENT'" color="red" class="urgent-tag" style="margin-left: 4px">加急</a-tag>
        </template>
        <template v-else-if="column.key === 'title'">{{ record.order.title }}</template>
        <template v-else-if="column.key === 'customerName'">{{ record.customerName }}</template>
        <template v-else-if="column.key === 'expectedReportDate'">{{ record.order.expectedReportDate }}</template>
        <template v-else-if="column.key === 'amount'">¥{{ fmt(record.order.adjustedAmount) }}</template>
        <template v-else-if="column.key === 'subcontract'">
          <a-tag v-if="record.order.hasSubcontract" color="orange">已分包</a-tag>
          <span v-else>-</span>
        </template>
        <template v-else-if="column.key === 'status'">
          <DictTag code="entrust_status" :value="record.order.status" />
        </template>
      </template>
    </a-table>
  </a-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import DictTag from '@/components/DictTag.vue'
import { entrustApi } from '@/api'
import { loadDict } from '@/composables/useDict'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const data = ref<any[]>([])
const statusDict = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true })
const query = reactive<any>({
  keyword: (route.query.keyword as string) || '',
  status: undefined,
  urgency: undefined,
  mine: false,
  current: 1,
  size: 10
})

const columns = [
  { title: '委托单号', key: 'code', width: 190 },
  { title: '项目名称', key: 'title', ellipsis: true },
  { title: '客户', key: 'customerName', width: 180 },
  { title: '期望报告日', key: 'expectedReportDate', width: 120 },
  { title: '金额', key: 'amount', width: 120 },
  { title: '分包', key: 'subcontract', width: 90 },
  { title: '状态', key: 'status', width: 110 }
]

function fmt(v: any) {
  return Number(v || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

async function reload() {
  loading.value = true
  try {
    query.current = pagination.current
    query.size = pagination.pageSize
    const res: any = await entrustApi.page(query)
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

onMounted(async () => {
  statusDict.value = await loadDict('entrust_status')
  reload()
})
</script>
