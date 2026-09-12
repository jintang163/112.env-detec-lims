<template>
  <a-modal v-model:open="open" :title="title" @ok="onSubmit" :confirm-loading="saving" destroy-on-close>
    <a-form layout="vertical">
      <a-form-item :label="payType === 1 ? '计划日期' : '发生日期'" required>
        <a-date-picker v-model:value="form.date" style="width: 100%" />
      </a-form-item>
      <a-form-item label="金额(元)" required>
        <a-input-number v-model:value="form.amount" :min="0" style="width: 100%" />
      </a-form-item>
      <a-form-item v-if="payType === 3" label="发票号">
        <a-input v-model:value="form.invoiceNo" />
      </a-form-item>
      <a-form-item label="备注">
        <a-textarea v-model:value="form.remark" :rows="2" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import { contractApi } from '@/api'

const emit = defineEmits<{ (e: 'saved'): void }>()
const open = ref(false)
const saving = ref(false)
const payType = ref(1)
let contractId: number
const form = reactive<any>({ date: dayjs(), amount: 0, invoiceNo: '', remark: '' })

const title = computed(() => `新增${['', '收款计划', '实际收款', '开票记录'][payType.value]}`)

function openModal(cid: number, type: number) {
  contractId = cid
  payType.value = type
  Object.assign(form, { date: dayjs(), amount: 0, invoiceNo: '', remark: '' })
  open.value = true
}

async function onSubmit() {
  if (!form.date || !form.amount) return message.warning('请完善必填项')
  const payload: any = { contractId, payType, amount: form.amount, remark: form.remark }
  if (payType.value === 1) payload.planDate = form.date.format('YYYY-MM-DD')
  else payload.occurDate = form.date.format('YYYY-MM-DD')
  if (payType.value === 3) payload.invoiceNo = form.invoiceNo
  saving.value = true
  try {
    await contractApi.savePayment(payload)
    message.success('已保存')
    open.value = false
    emit('saved')
  } finally {
    saving.value = false
  }
}

defineExpose({ open: openModal })
</script>
