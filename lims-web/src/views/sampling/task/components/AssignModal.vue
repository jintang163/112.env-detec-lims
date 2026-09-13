<template>
  <a-modal
    v-model:open="open"
    title="采样任务派工"
    :confirm-loading="saving"
    @ok="onOk"
  >
    <a-form layout="vertical" style="margin-top: 12px">
      <a-form-item label="采样计划">
        <a-input :value="planTitle" disabled />
      </a-form-item>
      <a-form-item label="采样员" required>
        <a-select
          v-model:value="assigneeId"
          show-search
          option-filter-prop="label"
          placeholder="选择采样员(ROLE_SAMPLER)"
        >
          <a-select-option
            v-for="u in samplers"
            :key="u.userId"
            :value="u.userId"
            :label="`${u.realName} ${u.username} ${u.phone}`"
          >
            {{ u.realName }}（{{ u.username }} {{ u.phone }}）
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="备注">
        <a-textarea v-model:value="remark" :rows="3" placeholder="携带设备/注意事项等" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { samplingTaskApi, systemApi } from '@/api'

const emit = defineEmits<{ (e: 'saved'): void }>()

const open = ref(false)
const saving = ref(false)
const planId = ref<number>()
const planTitle = ref('')
const assigneeId = ref<number>()
const remark = ref('')
const samplers = ref<any[]>([])

async function openModal(id: number | string, title?: string) {
  open.value = true
  planId.value = Number(id)
  planTitle.value = title || ''
  assigneeId.value = undefined
  remark.value = ''
  samplers.value = (await systemApi.userOptions('ROLE_SAMPLER')) as any[]
}

async function onOk() {
  if (!assigneeId.value) return message.warning('请选择采样员')
  saving.value = true
  try {
    await samplingTaskApi.assign({
      planId: planId.value!,
      assigneeId: assigneeId.value,
      remark: remark.value
    })
    message.success('派工成功, 已通知采样员')
    open.value = false
    emit('saved')
  } finally {
    saving.value = false
  }
}

defineExpose({ open: openModal })
</script>
