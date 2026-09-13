<template>
  <a-card>
    <a-form layout="inline" class="page-toolbar" style="margin-bottom: 16px">
      <a-form-item label="关键字">
        <a-input
          v-model:value="query.keyword"
          placeholder="任务编号/采样员"
          allow-clear
          @press-enter="reload"
        />
      </a-form-item>
      <a-form-item label="状态">
        <a-select
          v-model:value="query.status"
          allow-clear
          style="width: 150px"
          placeholder="全部"
          @change="reload"
        >
          <a-select-option v-for="d in statusDict" :key="d.itemValue" :value="d.itemValue">
            {{ d.itemLabel }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <a-button type="primary" @click="reload">查询</a-button>
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
        <template v-if="column.key === 'code'">{{ record.code }}</template>
        <template v-else-if="column.key === 'planTitle'">{{ record.planTitle }}</template>
        <template v-else-if="column.key === 'orderCode'">{{ record.orderCode }}</template>
        <template v-else-if="column.key === 'sampleCount'">
          <a-tag color="cyan">{{ record.sampleCount ?? 0 }}</a-tag>
        </template>
        <template v-else-if="column.key === 'status'">
          <DictTag code="sampling_task_status" :value="record.status" />
        </template>
        <template v-else-if="column.key === 'action'">
          <a @click="viewDetail(record)">详情</a>
        </template>
      </template>
    </a-table>
  </a-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { samplingTaskApi } from '@/api'
import { loadDict, type DictItem } from '@/composables/useDict'
import DictTag from '@/components/DictTag.vue'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const data = ref<any[]>([])
const statusDict = ref<DictItem[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true })
const query = reactive<any>({ keyword: '', status: '', current: 1, size: 10 })

const columns = [
  { title: '任务编号', key: 'code', dataIndex: 'code', width: 150 },
  { title: '采样计划', key: 'planTitle' },
  { title: '委托单', key: 'orderCode', dataIndex: 'orderCode', width: 140 },
  { title: '采样员', key: 'assigneeName', dataIndex: 'assigneeName', width: 110 },
  { title: '样品数', key: 'sampleCount', dataIndex: 'sampleCount', width: 90 },
  { title: '下载时间', key: 'downloadedAt', dataIndex: 'downloadedAt', width: 170 },
  { title: '派工时间', key: 'assignedAt', dataIndex: 'assignedAt', width: 170 },
  { title: '状态', key: 'status', dataIndex: 'status', width: 130 },
  { title: '操作', key: 'action', width: 120 }
]

async function reload() {
  loading.value = true
  try {
    query.current = pagination.current
    query.size = pagination.pageSize
    if (route.query.code) query.keyword = String(route.query.code)
    const res: any = await samplingTaskApi.page(query)
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

function viewDetail(record: any) {
  router.push(`/sampling/plan/detail/${record.planId}`)
}

onMounted(async () => {
  statusDict.value = await loadDict('sampling_task_status')
  reload()
})
</script>
