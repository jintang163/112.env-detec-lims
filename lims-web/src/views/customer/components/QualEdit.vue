<template>
  <a-modal v-model:open="open" title="新增资质文件" @ok="onSubmit" :confirm-loading="saving" destroy-on-close>
    <a-form layout="vertical">
      <a-form-item label="资质类型" required>
        <a-select v-model:value="form.qualType" placeholder="选择或输入类型" show-search>
          <a-select-option value="营业执照">营业执照</a-select-option>
          <a-select-option value="排污许可证">排污许可证</a-select-option>
          <a-select-option value="行业资质">行业资质</a-select-option>
          <a-select-option value="授权委托书">授权委托书</a-select-option>
          <a-select-option value="其他">其他</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item label="证书编号">
        <a-input v-model:value="form.certNo" />
      </a-form-item>
      <a-form-item label="有效期">
        <a-range-picker v-model:value="dateRange" />
      </a-form-item>
      <a-form-item label="资质文件">
        <a-upload
          :action="uploadUrl"
          :headers="headers"
          :max-count="1"
          @change="onUpload"
        >
          <a-button>点击上传(PDF/图片)</a-button>
        </a-upload>
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import dayjs, { Dayjs } from 'dayjs'
import { message } from 'ant-design-vue'
import { customerApi, fileApi } from '@/api'

const emit = defineEmits<{ (e: 'saved'): void }>()
const open = ref(false)
const saving = ref(false)
const dateRange = ref<[Dayjs, Dayjs]>()
const form = reactive<any>({ customerId: undefined, qualType: undefined })
const uploadUrl = fileApi.uploadUrl
const headers = { Authorization: `Bearer ${localStorage.getItem('lims_token')}` }

function openModal(customerId: number) {
  Object.assign(form, { customerId, qualType: undefined, certNo: '', fileId: undefined, fileUrl: '', fileName: '' })
  dateRange.value = undefined
  open.value = true
}

function onUpload(info: any) {
  if (info.file.status === 'done') {
    const data = info.file.response?.data
    form.fileId = data?.id
    form.fileUrl = data?.url
    form.fileName = data?.originalName
    message.success('上传成功')
  } else if (info.file.status === 'error') {
    message.error('上传失败')
  }
}

async function onSubmit() {
  if (!form.qualType) return message.warning('请选择资质类型')
  if (dateRange.value) {
    form.validFrom = dateRange.value[0].format('YYYY-MM-DD')
    form.validTo = dateRange.value[1].format('YYYY-MM-DD')
  }
  saving.value = true
  try {
    await customerApi.saveQualification(form)
    message.success('已保存')
    open.value = false
    emit('saved')
  } finally {
    saving.value = false
  }
}

defineExpose({ open: openModal })
</script>
