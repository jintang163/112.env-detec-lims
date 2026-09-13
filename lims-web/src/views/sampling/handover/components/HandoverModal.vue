<template>
  <a-modal
    :open="open"
    title="样品交接核对"
    width="900px"
    :footer="null"
    @cancel="open = false"
  >
    <a-spin :spinning="loading">
      <template v-if="detail">
        <a-descriptions bordered :column="3" size="small">
          <a-descriptions-item label="交接单号">{{ detail.handover.code }}</a-descriptions-item>
          <a-descriptions-item label="采样任务">{{ detail.taskCode }}</a-descriptions-item>
          <a-descriptions-item label="状态">
            <DictTag code="handover_status" :value="detail.handover.status" />
          </a-descriptions-item>
          <a-descriptions-item label="移交人">{{ detail.handover.handoverByName }}</a-descriptions-item>
          <a-descriptions-item label="移交时间">{{ fmt(detail.handover.handoverAt) }}</a-descriptions-item>
          <a-descriptions-item label="样品数量">
            <a-tag color="blue">{{ detail.handover.sampleCount }}</a-tag>
          </a-descriptions-item>
          <a-descriptions-item label="样品状态" :span="3">
            {{ detail.handover.sampleStatus }}
          </a-descriptions-item>
          <a-descriptions-item v-if="detail.sigUrl" label="采样员签名" :span="3">
            <a-image :src="detail.sigUrl" :width="180" />
          </a-descriptions-item>
        </a-descriptions>

        <a-table
          :data-source="detail.samples"
          :pagination="false"
          row-key="id"
          size="small"
          style="margin-top: 12px"
          :scroll="{ y: 320 }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'sampleCode'">{{ record.sample.sampleCode }}</template>
            <template v-else-if="column.key === 'pointName'">{{ record.sample.pointName || '-' }}</template>
            <template v-else-if="column.key === 'sampleName'">
              {{ record.sample.sampleName }} / {{ record.sample.itemName }}
            </template>
            <template v-else-if="column.key === 'env'">
              {{ record.sample.temperature ?? '-' }}℃ / pH {{ record.sample.ph ?? '-' }}
            </template>
            <template v-else-if="column.key === 'qc'">
              <a-tag v-if="record.sample.isQc === 1" color="purple">质控</a-tag>
              <span v-else>-</span>
            </template>
            <template v-else-if="column.key === 'storage'">
              {{ record.sample.storageCondition || '-' }}
            </template>
            <template v-else-if="column.key === 'photos'">
              <a-image
                v-for="p in record.photos"
                :key="p.id"
                :src="p.url"
                :width="36"
                height="36"
                style="object-fit: cover; margin-right: 4px"
              />
              <span v-if="!record.photos?.length">-</span>
            </template>
          </template>
          <a-table-column key="sampleCode" title="样品编号" width="170" />
          <a-table-column key="pointName" title="点位" width="120" />
          <a-table-column key="sampleName" title="样品/项目" />
          <a-table-column key="env" title="温度/pH" width="130" />
          <a-table-column key="qc" title="质控" width="70" />
          <a-table-column key="storage" title="保存条件" width="100" />
          <a-table-column key="photos" title="照片" width="120" />
        </a-table>

        <div v-if="detail.handover.status === 'PENDING' && canConfirm" style="margin-top: 16px">
          <a-textarea
            v-model:value="remark"
            :rows="2"
            placeholder="接收备注 / 拒收原因"
          />
          <div style="margin-top: 12px; text-align: right">
            <a-space>
              <a-popconfirm title="确认拒收? 任务将退回采样员" @confirm="onReject">
                <a-button danger>拒收</a-button>
              </a-popconfirm>
              <a-button type="primary" :loading="saving" @click="onConfirm">
                核对无误, 接收({{ detail.samples.length }}个样品)
              </a-button>
            </a-space>
          </div>
        </div>
      </template>
    </a-spin>
  </a-modal>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { message } from 'ant-design-vue'
import { handoverApi } from '@/api'
import { useUserStore } from '@/stores/user'
import DictTag from '@/components/DictTag.vue'

const emit = defineEmits<{ (e: 'saved'): void }>()
const userStore = useUserStore()

const open = ref(false)
const loading = ref(false)
const saving = ref(false)
const handoverId = ref<number>()
const detail = ref<any>(null)
const remark = ref('')

const canConfirm = computed(() => userStore.hasPerm('sampling:handover:confirm'))

function fmt(t: string) {
  return t ? t.replace('T', ' ').slice(0, 16) : '-'
}

async function openModal(id: number) {
  open.value = true
  handoverId.value = id
  remark.value = ''
  detail.value = null
  loading.value = true
  try {
    detail.value = await handoverApi.detail(id)
  } finally {
    loading.value = false
  }
}

async function onConfirm() {
  saving.value = true
  try {
    await handoverApi.confirm(handoverId.value!, remark.value)
    message.success('已接收, 委托单自动进入检测中')
    open.value = false
    emit('saved')
  } finally {
    saving.value = false
  }
}

async function onReject() {
  await handoverApi.reject(handoverId.value!, remark.value || '样品核对不符')
  message.success('已拒收')
  open.value = false
  emit('saved')
}

defineExpose({ open: openModal })
</script>
