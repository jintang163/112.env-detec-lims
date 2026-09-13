<template>
  <a-card>
    <a-form layout="inline" class="page-toolbar" style="margin-bottom: 16px">
      <a-form-item label="关键字">
        <a-input
          v-model:value="query.keyword"
          placeholder="交接单号/移交人"
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
        <template v-if="column.key === 'code'">
          <a @click="modalRef?.open(record.id)">{{ record.code }}</a>
        </template>
        <template v-else-if="column.key === 'status'">
          <DictTag code="handover_status" :value="record.status" />
        </template>
        <template v-else-if="column.key === 'action'">
          <a @click="modalRef?.open(record.id)">核对</a>
        </template>
      </template>
    </a-table>

    <HandoverModal ref="modalRef" @saved="reload" />
  </a-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { handoverApi } from '@/api'
import { loadDict, type DictItem } from '@/composables/useDict'
import DictTag from '@/components/DictTag.vue'
import HandoverModal from './components/HandoverModal.vue'

const loading = ref(false)
const data = ref<any[]>([])
const statusDict = ref<DictItem[]>([])
const modalRef = ref<InstanceType<typeof HandoverModal>>()
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true })
const query = reactive<any>({ keyword: '', status: '', current: 1, size: 10 })

const columns = [
  { title: '交接单号', key: 'code', dataIndex: 'code', width: 150 },
  { title: '采样任务', key: 'taskCode', dataIndex: 'taskCode', width: 150 },
  { title: '计划', key: 'planTitle', dataIndex: 'planTitle' },
  { title: '委托单', key: 'orderCode', dataIndex: 'orderCode', width: 140 },
  { title: '移交人', key: 'handoverByName', dataIndex: 'handoverByName', width: 100 },
  { title: '样品数', key: 'sampleCount', dataIndex: 'sampleCount', width: 80 },
  { title: '移交时间', key: 'handoverAt', dataIndex: 'handoverAt', width: 170 },
  { title: '接收人', key: 'receiverName', dataIndex: 'receiverName', width: 110 },
  { title: '状态', key: 'status', dataIndex: 'status', width: 100 },
  { title: '操作', key: 'action', width: 80 }
]

async function reload() {
  loading.value = true
  try {
    query.current = pagination.current
    query.size = pagination.pageSize
    const res: any = await handoverApi.page(query)
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
  statusDict.value = await loadDict('handover_status')
  reload()
})
</script>
