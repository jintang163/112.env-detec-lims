<template>
  <a-card title="我的审批待办">
    <a-table :data-source="todos" row-key="id" :loading="loading" :pagination="false">
      <a-table-column title="事项" data-index="title" />
      <a-table-column title="当前节点" data-index="nodeName" :width="180" />
      <a-table-column title="到达时间" data-index="createTime" :width="180" />
      <a-table-column title="操作" :width="260">
        <template #default="{ record }">
          <a-space>
            <a-button type="primary" size="small" @click="openAct(record, true)">通过</a-button>
            <a-button danger size="small" @click="openAct(record, false)">驳回</a-button>
            <a-button size="small" @click="viewBiz(record)">查看单据</a-button>
          </a-space>
        </template>
      </a-table-column>
    </a-table>
  </a-card>
</template>

<script setup lang="ts">
import { h, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { approvalApi } from '@/api'

const router = useRouter()
const loading = ref(false)
const todos = ref<any[]>([])

async function reload() {
  loading.value = true
  try {
    todos.value = (await approvalApi.todo()) as any
  } finally {
    loading.value = false
  }
}

function openAct(record: any, approve: boolean) {
  let comment = ''
  Modal.confirm({
    title: approve ? `审批通过: ${record.title}` : `驳回: ${record.title}`,
    content: () =>
      h('textarea', {
        class: 'ant-input',
        rows: 3,
        placeholder: approve ? '审批意见(可选)' : '驳回原因(必填)',
        onInput: (e: any) => (comment = e.target.value)
      }),
    onOk: async () => {
      if (!approve && !comment) {
        message.warning('驳回必须填写原因')
        throw new Error()
      }
      await approvalApi.act(record.id, approve, comment)
      message.success(approve ? '已通过' : '已驳回')
      reload()
    }
  })
}

function viewBiz(record: any) {
  const map: Record<string, string> = {
    CONTRACT: '/contract/detail/',
    CONTRACT_CHANGE: '/contract/detail/',
    QUOTE: '/quote/detail/',
    ENTRUST_REVIEW: '/entrust/detail/',
    ADJUSTMENT: '/entrust/detail/',
    SUBCONTRACT: '/entrust/detail/'
  }
  const base = map[record.bizType]
  if (base && record.bizId) {
    router.push(base + record.bizId)
  } else {
    message.info('请在对应业务列表中查看该单据')
  }
}

onMounted(reload)
</script>
