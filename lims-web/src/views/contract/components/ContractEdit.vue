<template>
  <a-modal v-model:open="open" title="新建合同" width="720px" @ok="onSubmit" :confirm-loading="saving" destroy-on-close>
    <a-form layout="vertical">
      <a-row :gutter="16">
        <a-col :span="24">
          <a-form-item label="合同名称" required>
            <a-input v-model:value="form.name" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="客户" required>
            <a-select
              v-model:value="form.customerId"
              show-search
              placeholder="选择客户"
              :filter-option="(i: any, e: any) => (e?.children?.[0] || '').includes(form.customerKw || '')"
              @search="(v: string) => (form.customerKw = v)"
            >
              <a-select-option v-for="c in customers" :key="c.id" :value="c.id">
                {{ c.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="合同金额(含税,元)" required>
            <a-input-number v-model:value="form.amount" :min="0" style="width: 100%" />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="签订日期">
            <a-date-picker v-model:value="form.signDate" style="width: 100%" />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="生效日期">
            <a-date-picker v-model:value="form.effectiveDate" style="width: 100%" />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="到期日期">
            <a-date-picker v-model:value="form.expiryDate" style="width: 100%" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="税率(%)">
            <a-input-number v-model:value="form.taxRate" :min="0" :max="100" style="width: 100%" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="付款方式">
            <a-input v-model:value="form.paymentMethod" placeholder="月付/季付/预付/验收后..." />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="我方签约主体">
            <a-input v-model:value="form.ourParty" placeholder="某某环境检测有限公司" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="对方签约主体">
            <a-input v-model:value="form.counterParty" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item label="付款条款">
            <a-textarea v-model:value="form.paymentTerms" :rows="2" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item label="合同正文(Word, 可稍后在详情页在线编辑)">
            <a-upload :action="uploadUrl" :headers="headers" :max-count="1" @change="onUpload">
              <a-button>上传合同文件</a-button>
            </a-upload>
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import { contractApi, customerApi, fileApi } from '@/api'

const emit = defineEmits<{ (e: 'saved'): void }>()
const open = ref(false)
const saving = ref(false)
const customers = ref<any[]>([])
const form = reactive<any>({})
const uploadUrl = fileApi.uploadUrl
const headers = { Authorization: `Bearer ${localStorage.getItem('lims_token')}` }

async function openModal() {
  Object.assign(form, {
    name: '',
    customerId: undefined,
    amount: 0,
    taxRate: 6,
    signDate: dayjs(),
    ourParty: '某某环境检测有限公司'
  })
  const res: any = await customerApi.page({ current: 1, size: 200, scope: 'all' })
  customers.value = res.records
  open.value = true
}

function onUpload(info: any) {
  if (info.file.status === 'done') {
    form.fileId = info.file.response.data.id
    form.fileUrl = info.file.response.data.url
    message.success('上传成功')
  }
}

async function onSubmit() {
  if (!form.name || !form.customerId || !form.amount) {
    return message.warning('请完善必填项')
  }
  saving.value = true
  try {
    await contractApi.save(form)
    message.success('已保存为草稿')
    open.value = false
    emit('saved')
  } finally {
    saving.value = false
  }
}

defineExpose({ open: openModal })
</script>
