<template>
  <a-modal
    v-model:open="open"
    :title="form.id ? '编辑客户' : '新建客户'"
    width="760px"
    @ok="onSubmit"
    :confirm-loading="saving"
    destroy-on-close
  >
    <a-form :model="form" layout="vertical">
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="客户全称" required>
            <a-input v-model:value="form.name" placeholder="企业/单位全称" />
          </a-form-item>
        </a-col>
        <a-col :span="6">
          <a-form-item label="简称">
            <a-input v-model:value="form.shortName" />
          </a-form-item>
        </a-col>
        <a-col :span="6">
          <a-form-item label="客户类型">
            <a-select v-model:value="form.customerType">
              <a-select-option :value="10">企业</a-select-option>
              <a-select-option :value="20">政府事业单位</a-select-option>
              <a-select-option :value="30">个人</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="联系人">
            <a-input v-model:value="form.contactPerson" />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="联系电话">
            <a-input v-model:value="form.contactPhone" />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="所属行业">
            <a-input v-model:value="form.industry" placeholder="如 制药/化工/政府" />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="省份">
            <a-input v-model:value="form.province" />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="城市">
            <a-input v-model:value="form.city" />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="区县">
            <a-input v-model:value="form.district" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item label="详细地址">
            <a-input v-model:value="form.address" />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="统一社会信用代码">
            <a-input v-model:value="form.taxNo" />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="开户银行">
            <a-input v-model:value="form.bankName" />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="银行账号">
            <a-input v-model:value="form.bankAccount" />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="客户分级">
            <a-select v-model:value="form.customerLevel">
              <a-select-option value="A">A级(重点)</a-select-option>
              <a-select-option value="B">B级</a-select-option>
              <a-select-option value="C">C级</a-select-option>
              <a-select-option value="D">D级</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="授信额度(元)">
            <a-input-number v-model:value="form.creditLimit" :min="0" style="width: 100%" />
          </a-form-item>
        </a-col>
        <a-col :span="8">
          <a-form-item label="账期(天)">
            <a-input-number v-model:value="form.creditPeriod" :min="0" style="width: 100%" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="客户来源">
            <a-input v-model:value="form.source" placeholder="展会/转介绍/招标/自助..." />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="入库方式" v-if="!form.id">
            <a-radio-group v-model:value="form.toPool">
              <a-radio :value="true">放入公海</a-radio>
              <a-radio :value="false">直接归属我</a-radio>
            </a-radio-group>
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item label="备注">
            <a-textarea v-model:value="form.remark" :rows="2" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </a-modal>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { customerApi } from '@/api'

const emit = defineEmits<{ (e: 'saved'): void }>()
const open = ref(false)
const saving = ref(false)
const form = reactive<any>({})

async function openModal(id?: number) {
  Object.keys(form).forEach((k) => delete (form as any)[k])
  Object.assign(form, {
    customerType: 10,
    customerLevel: 'C',
    creditLimit: 0,
    creditPeriod: 0,
    toPool: true
  })
  if (id) {
    const data: any = await customerApi.detail(id)
    Object.assign(form, data)
  }
  open.value = true
}

async function onSubmit() {
  if (!form.name) {
    message.warning('请填写客户全称')
    return
  }
  saving.value = true
  try {
    await customerApi.save(form)
    message.success('保存成功')
    open.value = false
    emit('saved')
  } finally {
    saving.value = false
  }
}

defineExpose({ open: openModal })
</script>
