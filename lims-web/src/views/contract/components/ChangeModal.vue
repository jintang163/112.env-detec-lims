<template>
  <a-modal v-model:open="open" title="发起合同变更" width="560px" @ok="onSubmit" :confirm-loading="saving" destroy-on-close>
    <a-form layout="vertical">
      <a-form-item label="变更类型" required>
        <a-select v-model:value="form.changeType">
          <a-select-option value="AMOUNT">金额变更</a-select-option>
          <a-select-option value="PERIOD">周期变更</a-select-option>
          <a-select-option value="TERMS">条款变更</a-select-option>
          <a-select-option value="OTHER">其他</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item v-if="form.changeType === 'AMOUNT'" label="变更后金额(元, 通过审批后自动同步)" required>
        <a-input-number v-model:value="afterAmount" :min="0" style="width: 100%" />
      </a-form-item>
      <a-form-item v-else label="变更后内容" required>
        <a-textarea v-model:value="form.afterContent" :rows="3" />
      </a-form-item>
      <a-form-item label="变更前内容">
        <a-textarea v-model:value="form.beforeContent" :rows="2" />
      </a-form-item>
      <a-form-item label="变更原因" required>
        <a-textarea v-model:value="form.reason" :rows="2" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { contractApi } from '@/api'

const emit = defineEmits<{ (e: 'saved'): void }>()
const open = ref(false)
const saving = ref(false)
const afterAmount = ref<number>()
const form = reactive<any>({
  contractId: undefined,
  changeType: 'AMOUNT',
  beforeContent: '',
  afterContent: '',
  reason: ''
})

function openModal(contractId: number) {
  Object.assign(form, { contractId, changeType: 'AMOUNT', beforeContent: '', afterContent: '', reason: '' })
  afterAmount.value = undefined
  open.value = true
}

async function onSubmit() {
  if (form.changeType === 'AMOUNT') {
    if (!afterAmount.value) return message.warning('请填写变更后金额')
    form.afterContent = String(afterAmount.value)
  } else if (!form.afterContent || !form.reason) {
    return message.warning('请完善变更内容与原因')
  }
  saving.value = true
  try {
    await contractApi.applyChange(form)
    message.success('变更单已提交审批')
    open.value = false
    emit('saved')
  } finally {
    saving.value = false
  }
}

defineExpose({ open: openModal })
</script>
