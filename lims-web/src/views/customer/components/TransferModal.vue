<template>
  <a-modal v-model:open="open" title="分配 / 转交客户" @ok="onSubmit" :confirm-loading="saving">
    <a-form layout="vertical">
      <a-form-item label="目标业务员" required>
        <a-select v-model:value="userId" placeholder="选择业务员" show-search option-filter-prop="label">
          <a-select-option
            v-for="u in users"
            :key="u.userId"
            :value="u.userId"
            :label="u.realName"
          >
            {{ u.realName }}（{{ u.username }}）
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="备注">
        <a-textarea v-model:value="remark" :rows="2" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { customerApi, systemApi } from '@/api'

const emit = defineEmits<{ (e: 'saved'): void }>()
const open = ref(false)
const saving = ref(false)
const users = ref<any[]>([])
const userId = ref<number>()
const remark = ref('')
let customerId: number | undefined

async function openModal(record: any) {
  customerId = record.id
  userId.value = undefined
  remark.value = ''
  users.value = (await systemApi.userOptions()) as any
  open.value = true
}

async function onSubmit() {
  if (!userId.value) return message.warning('请选择目标业务员')
  saving.value = true
  try {
    await customerApi.transfer(customerId!, userId.value, remark.value)
    message.success('分配成功')
    open.value = false
    emit('saved')
  } finally {
    saving.value = false
  }
}

defineExpose({ open: openModal })
</script>
