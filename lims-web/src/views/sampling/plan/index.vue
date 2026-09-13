<template>
  <a-card>
    <a-form layout="inline" class="page-toolbar" style="margin-bottom: 16px">
      <a-form-item label="关键字">
        <a-input
          v-model:value="query.keyword"
          placeholder="计划编号/名称"
          allow-clear
          @press-enter="reload"
        />
      </a-form-item>
      <a-form-item label="状态">
        <a-select
          v-model:value="query.status"
          allow-clear
          style="width: 130px"
          placeholder="全部"
          @change="reload"
        >
          <a-select-option v-for="d in statusDict" :key="d.itemValue" :value="d.itemValue">
            {{ d.itemLabel }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="采样日期">
        <a-range-picker v-model:value="dateRange" value-format="YYYY-MM-DD" @change="reload" />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" @click="reload">查询</a-button>
      </a-form-item>
      <a-form-item style="float: right">
        <a-button
          v-if="userStore.hasPerm('sampling:plan:save')"
          type="primary"
          @click="router.push('/sampling/plan/edit')"
        >
          制定采样计划
        </a-button>
      </a-form-item>
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
          <a @click="router.push(`/sampling/plan/detail/${record.id}`)">{{ record.code }}</a>
        </template>
        <template v-else-if="column.key === 'title'">
          <a @click="router.push(`/sampling/plan/detail/${record.id}`)">{{ record.title }}</a>
          <div class="muted">{{ record.orderCode }}</div>
        </template>
        <template v-else-if="column.key === 'customerName'">{{ record.customerName }}</template>
        <template v-else-if="column.key === 'planDate'">{{ record.planDate }}</template>
        <template v-else-if="column.key === 'status'">
          <DictTag code="sampling_plan_status" :value="record.status" />
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a @click="router.push(`/sampling/plan/detail/${record.id}`)">查看</a>
            <a
              v-if="record.status === 'DRAFT' && userStore.hasPerm('sampling:plan:save')"
              @click="router.push(`/sampling/plan/edit/${record.id}`)"
              >编辑</a
            >
            <a-popconfirm
              v-if="record.status === 'DRAFT' && userStore.hasPerm('sampling:plan:issue')"
              title="确认下发该采样计划？下发后可派工"
              @confirm="onIssue(record)"
            >
              <a>下发</a>
            </a-popconfirm>
          </a-space>
        </template>
      </template>
    </a-table>
  </a-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import { samplingPlanApi } from '@/api'
import { loadDict, type DictItem } from '@/composables/useDict'
import { useUserStore } from '@/stores/user'
import DictTag from '@/components/DictTag.vue'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const data = ref<any[]>([])
const statusDict = ref<DictItem[]>([])
const dateRange = ref<string[]>()
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true })
const query = reactive<any>({ keyword: '', status: '', current: 1, size: 10 })

const columns = [
  { title: '计划编号', key: 'code', dataIndex: 'code', width: 160 },
  { title: '计划名称/委托单', key: 'title' },
  { title: '客户', key: 'customerName', dataIndex: 'customerName', width: 200 },
  { title: '采样日期', key: 'planDate', dataIndex: 'planDate', width: 120 },
  { title: '联系人', dataIndex: 'contactPerson', key: 'contactPerson', width: 100 },
  { title: '状态', key: 'status', dataIndex: 'status', width: 100 },
  { title: '操作', key: 'action', width: 170 }
]

async function reload() {
  loading.value = true
  try {
    query.current = pagination.current
    query.size = pagination.pageSize
    query.planDateStart = dateRange.value?.[0]
    query.planDateEnd = dateRange.value?.[1]
    const res: any = await samplingPlanApi.page(query)
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

async function onIssue(record: any) {
  await samplingPlanApi.issue(record.id)
  message.success('计划已下发')
  reload()
}

onMounted(async () => {
  statusDict.value = await loadDict('sampling_plan_status')
  reload()
})
</script>

<style scoped>
.muted {
  color: #999;
  font-size: 12px;
}
</style>
