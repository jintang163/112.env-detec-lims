<template>
  <a-card title="操作日志">
    <template #extra>
      <a-space>
        <a-select
          v-model:value="module"
          style="width: 140px"
          placeholder="模块"
          allow-clear
          @change="reload(1)"
        >
          <a-select-option
            v-for="m in moduleOptions"
            :key="m"
            :value="m"
          >{{ m }}</a-select-option>
        </a-select>
        <a-input
          v-model:value="username"
          placeholder="操作人"
          style="width: 130px"
          allow-clear
          @press-enter="reload(1)"
        />
        <a-input
          v-model:value="keyword"
          placeholder="动作/接口关键字"
          style="width: 200px"
          allow-clear
          @press-enter="reload(1)"
        />
        <a-button type="primary" @click="reload(1)">查询</a-button>
      </a-space>
    </template>

    <a-table
      :data-source="data"
      :columns="columns"
      row-key="id"
      :loading="loading"
      :pagination="false"
      size="small"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'module'">
          <a-tag color="blue">{{ record.module }}</a-tag>
        </template>
        <template v-else-if="column.key === 'costMs'">
          <span :style="{ color: record.costMs > 2000 ? '#fa8c16' : '#52c41a' }">
            {{ record.costMs }} ms
          </span>
        </template>
        <template v-else-if="column.key === 'params'">
          <a-typography-paragraph
            v-if="record.params"
            :ellipsis="{ rows: 2, expandable: true, symbol: '展开' }"
            style="margin-bottom: 0; max-width: 360px"
            :content="record.params"
          />
          <span v-else style="color: #bbb">-</span>
        </template>
      </template>
    </a-table>

    <div style="text-align: right; margin-top: 12px">
      <a-pagination
        :current="query.current"
        :page-size="query.size"
        :total="total"
        size="small"
        show-total
        @change="(p: number) => reload(p)"
      />
    </div>
  </a-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { systemApi } from '@/api'

interface OperationLog {
  id: number
  userId?: number
  username?: string
  module?: string
  action?: string
  method?: string
  params?: string
  ip?: string
  costMs?: number
  createTime?: string
}

const moduleOptions = [
  '认证',
  '客户管理',
  '合同管理',
  '委托管理',
  '报价管理',
  '审批中心',
  '文件',
  '移动采样'
]

const loading = ref(false)
const data = ref<OperationLog[]>([])
const total = ref(0)
const module = ref<string | undefined>(undefined)
const username = ref('')
const keyword = ref('')
const query = reactive({ current: 1, size: 10 })

const columns = [
  { title: '时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
  { title: '操作人', dataIndex: 'username', key: 'username', width: 110 },
  { title: '模块', dataIndex: 'module', key: 'module', width: 100 },
  { title: '动作', dataIndex: 'action', key: 'action', width: 150 },
  { title: '接口', dataIndex: 'method', key: 'method', width: 230 },
  { title: '参数', dataIndex: 'params', key: 'params' },
  { title: 'IP', dataIndex: 'ip', key: 'ip', width: 130 },
  { title: '耗时', dataIndex: 'costMs', key: 'costMs', width: 100 }
]

async function reload(page?: number) {
  if (page) {
    query.current = page
  }
  loading.value = true
  try {
    const res: any = await systemApi.operationLogs({
      current: query.current,
      size: query.size,
      module: module.value,
      username: username.value || undefined,
      keyword: keyword.value || undefined
    })
    data.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

onMounted(() => reload(1))
</script>
