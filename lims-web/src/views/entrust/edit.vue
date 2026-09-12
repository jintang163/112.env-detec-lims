<template>
  <a-card>
    <a-page-header :title="isEdit ? '编辑委托单' : '新建委托单'" @back="router.back()">
      <template #extra>
        <a-space>
          <a-button @click="onSave(false)">保存草稿</a-button>
          <a-button type="primary" @click="onSave(true)">保存并提交合同评审</a-button>
        </a-space>
      </template>
    </a-page-header>

    <a-form layout="vertical" style="margin-top: 8px">
      <a-card title="基本信息" size="small">
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="项目名称" required>
              <a-input v-model:value="form.title" placeholder="如:XX公司9月废水废气例行检测" />
            </a-form-item>
          </a-col>
          <a-col :span="5">
            <a-form-item label="客户" required>
              <a-select
                v-model:value="form.customerId"
                show-search
                placeholder="选择客户"
                :filter-option="filterCustomer"
                @change="onCustomerChange"
              >
                <a-select-option v-for="c in customers" :key="c.id" :value="c.id" :label="c.name">
                  {{ c.name }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="5">
            <a-form-item label="关联合同">
              <a-select v-model:value="form.contractId" allow-clear placeholder="可先建单后补合同">
                <a-select-option v-for="c in contracts" :key="c.id" :value="c.id">
                  {{ c.code }} {{ c.name }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="3">
            <a-form-item label="委托类型">
              <a-select v-model:value="form.entrustType">
                <a-select-option value="ENTRUST">委托检测</a-select-option>
                <a-select-option value="SUPERVISION">监督检测</a-select-option>
                <a-select-option value="SPOT">抽查检测</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="3">
            <a-form-item label="加急">
              <a-switch
                :checked="form.urgency === 'URGENT'"
                checked-children="加急"
                un-checked-children="普通"
                @change="(v: boolean) => (form.urgency = v ? 'URGENT' : 'NORMAL')"
              />
            </a-form-item>
          </a-col>
          <a-col :span="4">
            <a-form-item label="样品方式">
              <a-select v-model:value="form.sampleSource">
                <a-select-option :value="1">现场采样</a-select-option>
                <a-select-option :value="2">客户送样</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="4">
            <a-form-item label="计划采样时间">
              <a-date-picker
                v-model:value="form.plannedSamplingTime"
                show-time
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
          <a-col :span="4">
            <a-form-item label="期望报告时间" required>
              <a-date-picker v-model:value="form.expectedReportDate" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="4">
            <a-form-item label="现场联系人">
              <a-input v-model:value="form.contactPerson" />
            </a-form-item>
          </a-col>
          <a-col :span="4">
            <a-form-item label="联系电话">
              <a-input v-model:value="form.contactPhone" />
            </a-form-item>
          </a-col>
          <a-col :span="4">
            <a-form-item label="从报价单带入">
              <a-select v-model:value="quoteId" allow-clear placeholder="选择已审批报价" @change="applyQuote">
                <a-select-option v-for="q in quotes" :key="q.id" :value="q.id">
                  {{ q.code }} ¥{{ fmt(q.finalAmount) }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>
      </a-card>

      <a-card title="采样地址(BD-09 百度坐标系)" size="small" style="margin-top: 12px">
        <a-row :gutter="16">
          <a-col :span="6">
            <a-form-item label="省/市/区">
              <a-input-group compact>
                <a-input v-model:value="form.province" placeholder="省" style="width: 33%" />
                <a-input v-model:value="form.city" placeholder="市" style="width: 34%" />
                <a-input v-model:value="form.district" placeholder="区" style="width: 33%" />
              </a-input-group>
            </a-form-item>
          </a-col>
          <a-col :span="9">
            <a-form-item label="详细采样地址">
              <a-input v-model:value="form.samplingAddress" />
            </a-form-item>
          </a-col>
        </a-row>
        <BMapPicker v-model="mapValue" style="margin-top: 4px" />
      </a-card>

      <a-card title="检测项目与标准" size="small" style="margin-top: 12px">
        <div style="margin-bottom: 8px">
          <a-button type="primary" @click="addItem">添加检测项</a-button>
          <span class="sub">标准号示例如 HJ 828-2017、GB/T 16157-1996</span>
        </div>
        <a-table :data-source="form.items" row-key="_k" size="small" :pagination="false">
          <a-table-column title="#" :width="50">
            <template #default="{ index }">{{ index + 1 }}</template>
          </a-table-column>
          <a-table-column title="检测项目/参数" :width="200">
            <template #default="{ record }"><a-input v-model:value="record.itemName" /></template>
          </a-table-column>
          <a-table-column title="检测标准号" :width="170">
            <template #default="{ record }"><a-input v-model:value="record.standardCode" placeholder="HJ 828-2017" /></template>
          </a-table-column>
          <a-table-column title="标准名称" :width="220">
            <template #default="{ record }"><a-input v-model:value="record.standardName" /></template>
          </a-table-column>
          <a-table-column title="样品名称" :width="120">
            <template #default="{ record }"><a-input v-model:value="record.sampleName" /></template>
          </a-table-column>
          <a-table-column title="数量/点次" :width="100">
            <template #default="{ record }">
              <a-input-number v-model:value="record.qty" :min="0" @change="calcRow(record)" />
            </template>
          </a-table-column>
          <a-table-column title="单价" :width="110">
            <template #default="{ record }">
              <a-input-number v-model:value="record.unitPrice" :min="0" @change="calcRow(record)" />
            </template>
          </a-table-column>
          <a-table-column title="金额" :width="110">
            <template #default="{ record }">¥{{ fmt(record.amount) }}</template>
          </a-table-column>
          <a-table-column title="分包" :width="70">
            <template #default="{ record }">
              <a-checkbox :checked="record.isSubcontract === 1" @change="(e: any) => (record.isSubcontract = e.target.checked ? 1 : 0)" />
            </template>
          </a-table-column>
          <a-table-column title="操作" :width="70">
            <template #default="{ index }">
              <a-button type="link" danger size="small" @click="form.items.splice(index, 1)">删除</a-button>
            </template>
          </a-table-column>
        </a-table>
        <div style="text-align: right; margin-top: 8px; font-weight: 600">
          合计: ¥{{ fmt(totalAmount) }}
        </div>
      </a-card>

      <a-card title="采样点位(移动端可现场继续补充)" size="small" style="margin-top: 12px">
        <div style="margin-bottom: 8px">
          <a-button @click="addPoint">添加点位</a-button>
        </div>
        <a-table :data-source="form.points" row-key="_k" size="small" :pagination="false">
          <a-table-column title="#" :width="50">
            <template #default="{ index }">{{ index + 1 }}</template>
          </a-table-column>
          <a-table-column title="点位名称">
            <template #default="{ record }"><a-input v-model:value="record.name" placeholder="如 1#排气筒" /></template>
          </a-table-column>
          <a-table-column title="经度 BD-09" :width="180">
            <template #default="{ record }"><a-input-number v-model:value="record.lng" :precision="7" style="width: 100%" /></template>
          </a-table-column>
          <a-table-column title="纬度 BD-09" :width="180">
            <template #default="{ record }"><a-input-number v-model:value="record.lat" :precision="7" style="width: 100%" /></template>
          </a-table-column>
          <a-table-column title="位置描述">
            <template #default="{ record }"><a-input v-model:value="record.addrDesc" /></template>
          </a-table-column>
          <a-table-column title="操作" :width="70">
            <template #default="{ index }">
              <a-button type="link" danger size="small" @click="form.points.splice(index, 1)">删除</a-button>
            </template>
          </a-table-column>
        </a-table>
      </a-card>

      <a-card title="备注" size="small" style="margin-top: 12px">
        <a-textarea v-model:value="form.remark" :rows="2" />
      </a-card>
    </a-form>
  </a-card>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import BMapPicker from '@/components/BMapPicker.vue'
import { customerApi, contractApi, quoteApi, entrustApi } from '@/api'

const route = useRoute()
const router = useRouter()
const editId = route.params.id ? Number(route.params.id) : undefined
const isEdit = !!editId

let keySeed = 1
const form = reactive<any>({
  title: '',
  customerId: undefined,
  contractId: undefined,
  quoteId: undefined,
  entrustType: 'ENTRUST',
  urgency: 'NORMAL',
  sampleSource: 1,
  expectedReportDate: dayjs().add(10, 'day').format('YYYY-MM-DD'),
  items: [] as any[],
  points: [] as any[]
})
const customers = ref<any[]>([])
const contracts = ref<any[]>([])
const quotes = ref<any[]>([])
const quoteId = ref<number>()

const mapValue = ref<any>({})
const totalAmount = computed(() =>
  (form.items as any[]).reduce((s, i) => s + Number(i.amount || 0), 0)
)

function fmt(v: any) {
  return Number(v || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
function filterCustomer(input: string, option: any) {
  return (option?.label || '').includes(input)
}
function calcRow(record: any) {
  record.amount = Number(record.qty || 0) * Number(record.unitPrice || 0)
}
function addItem() {
  ;(form.items as any[]).push({
    _k: keySeed++,
    itemName: '',
    standardCode: '',
    standardName: '',
    sampleName: '',
    qty: 1,
    unitPrice: 0,
    amount: 0,
    isSubcontract: 0
  })
}
function addPoint() {
  ;(form.points as any[]).push({ _k: keySeed++, name: '', lng: undefined, lat: undefined, addrDesc: '' })
}

async function onCustomerChange(cid: number) {
  const res: any = await contractApi.page({ current: 1, size: 50, customerId: cid })
  contracts.value = res.records.filter((c: any) =>
    ['APPROVED', 'EXECUTING', 'CHANGED'].includes(c.status))
  const qres: any = await quoteApi.page({ current: 1, size: 50, customerId: cid, status: 'APPROVED' })
  quotes.value = qres.records
}

async function applyQuote(qid: number) {
  if (!qid) return
  const detail: any = await quoteApi.detail(qid)
  form.quoteId = qid
  form.items = (detail.items || []).map((it: any) => ({
    ...it,
    id: undefined,
    _k: keySeed++
  }))
  message.success(`已带入报价 ${detail.code} 的 ${detail.items?.length || 0} 个检测项`)
}

async function onSave(submit: boolean) {
  if (!form.title || !form.customerId) return message.warning('请填写项目名称并选择客户')
  if (!form.items.length) return message.warning('请至少添加一个检测项目')
  if (!form.expectedReportDate) return message.warning('请选择期望报告时间')
  if (mapValue.value?.lng) {
    form.lng = mapValue.value.lng
    form.lat = mapValue.value.lat
    if (!form.samplingAddress && mapValue.value.address) form.samplingAddress = mapValue.value.address
  }
  const payload = JSON.parse(JSON.stringify(form))
  payload.items?.forEach((i: any) => delete i._k)
  payload.points?.forEach((p: any) => delete p._k)

  let id = editId
  if (editId) {
    payload.id = editId
    await entrustApi.save(payload)
  } else {
    id = await entrustApi.save(payload)
  }
  if (submit) {
    await entrustApi.submitReview(id)
    message.success('已提交合同评审')
  } else {
    message.success('草稿已保存')
  }
  router.push(`/entrust/detail/${id}`)
}

onMounted(async () => {
  const res: any = await customerApi.page({ current: 1, size: 300, scope: 'all' })
  customers.value = res.records
  if (route.query.quoteId) {
    quoteId.value = Number(route.query.quoteId)
    form.customerId = Number(route.query.customerId)
    await onCustomerChange(form.customerId)
    await applyQuote(quoteId.value)
  }
  if (editId) {
    const detail: any = await entrustApi.detail(editId)
    Object.assign(form, detail.order)
    form.items = detail.items.map((i: any) => ({ ...i, _k: keySeed++ }))
    form.points = detail.points.map((p: any) => ({ ...p, _k: keySeed++ }))
    if (detail.order.customerId) onCustomerChange(detail.order.customerId)
    if (detail.order.lng) {
      mapValue.value = {
        lng: Number(detail.order.lng),
        lat: Number(detail.order.lat),
        address: detail.order.samplingAddress
      }
    }
  }
})
</script>
<style scoped>
.sub { color: #999; font-size: 12px; margin-left: 10px; }
</style>
