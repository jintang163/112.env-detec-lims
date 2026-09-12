<template>
  <a-card>
    <a-page-header :title="isEdit ? '编辑报价单' : '编制报价单'" @back="router.back()">
      <template #extra>
        <a-space>
          <a-button @click="save(false)">保存草稿</a-button>
          <a-button type="primary" @click="save(true)">保存并提交审批</a-button>
        </a-space>
      </template>
    </a-page-header>

    <a-form layout="vertical" style="margin-top: 8px">
      <a-card title="报价信息" size="small">
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="报价标题" required>
              <a-input v-model:value="form.title" />
            </a-form-item>
          </a-col>
          <a-col :span="5">
            <a-form-item label="客户" required>
              <a-select v-model:value="form.customerId" show-search :filter-option="filterCustomer">
                <a-select-option v-for="c in customers" :key="c.id" :value="c.id" :label="c.name">
                  {{ c.name }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="4">
            <a-form-item label="计价方式">
              <a-select v-model:value="form.pricingMode">
                <a-select-option value="RULE">Aviator规则自动计价</a-select-option>
                <a-select-option value="MANUAL">手工填写金额</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="3">
            <a-form-item label="加急系数">
              <a-input-number v-model:value="form.urgentFactor" :min="1" :step="0.1" :precision="2" style="width: 100%" @change="recalculate" />
            </a-form-item>
          </a-col>
          <a-col :span="4">
            <a-form-item label="报价有效期至">
              <a-date-picker v-model:value="form.validUntil" style="width: 100%" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-card>

      <a-card title="检测项目明细" size="small" style="margin-top: 12px">
        <div style="margin-bottom: 8px">
          <a-space>
            <a-button type="primary" @click="addItem">添加项目</a-button>
            <a-button @click="recalculate" :disabled="form.pricingMode !== 'RULE'">
              按规则重新计价
            </a-button>
            <a-button type="link" @click="rulesOpen = true">计价规则说明</a-button>
          </a-space>
        </div>
        <a-table :data-source="form.items" row-key="_k" size="small" :pagination="false">
          <a-table-column title="#" :width="50">
            <template #default="{ index }">{{ index + 1 }}</template>
          </a-table-column>
          <a-table-column title="检测项目/参数" :width="210">
            <template #default="{ record }"><a-input v-model:value="record.itemName" placeholder="如 水质化学需氧量(COD)" /></template>
          </a-table-column>
          <a-table-column title="标准号" :width="160">
            <template #default="{ record }"><a-input v-model:value="record.standardCode" /></template>
          </a-table-column>
          <a-table-column title="规格/点位说明" :width="150">
            <template #default="{ record }"><a-input v-model:value="record.spec" /></template>
          </a-table-column>
          <a-table-column title="单位" :width="90">
            <template #default="{ record }"><a-input v-model:value="record.unit" /></template>
          </a-table-column>
          <a-table-column title="数量/点次" :width="100">
            <template #default="{ record }">
              <a-input-number v-model:value="record.qty" :min="0" @change="rowCalc(record)" />
            </template>
          </a-table-column>
          <a-table-column title="单价" :width="110">
            <template #default="{ record }">
              <a-input-number v-model:value="record.unitPrice" :min="0" @change="rowCalc(record)" />
            </template>
          </a-table-column>
          <a-table-column title="金额" :width="110">
            <template #default="{ record }">¥{{ fmt(record.amount) }}</template>
          </a-table-column>
          <a-table-column title="命中公式" :width="200">
            <template #default="{ record }"><span class="sub">{{ record.formula || '-' }}</span></template>
          </a-table-column>
          <a-table-column title="操作" :width="70">
            <template #default="{ index }">
              <a-button type="link" danger size="small" @click="form.items.splice(index, 1); sum()">删除</a-button>
            </template>
          </a-table-column>
        </a-table>

        <div class="sum-bar">
          <span>明细合计: <b>¥{{ fmt(total) }}</b></span>
          <span>优惠/调减:
            <a-input-number v-model:value="form.discountAmount" :min="0" @change="sum" size="small" />
          </span>
          <span class="final">报价总额: ¥{{ fmt(final) }}</span>
        </div>
      </a-card>

      <a-card title="备注" size="small" style="margin-top: 12px">
        <a-textarea v-model:value="form.remark" :rows="2" />
      </a-card>
    </a-form>

    <a-modal v-model:open="rulesOpen" title="Aviator 计价规则" width="640px" :footer="null">
      <a-table :data-source="rules" row-key="id" size="small" :pagination="false">
        <a-table-column title="优先级" data-index="priority" :width="70" />
        <a-table-column title="规则" data-index="ruleName" :width="140" />
        <a-table-column title="关键字" data-index="keyword" :width="90" />
        <a-table-column title="Aviator 表达式" data-index="expression" />
      </a-table>
      <a-alert
        style="margin-top: 10px"
        type="info"
        show-icon
        message="可用变量: qty(数量) unit_price(单价) points(点位数) complexity(工况系数) urgent_factor(加急系数);项目名称包含关键字时命中该规则,按优先级数字小者优先,无关键字为兜底规则。"
      />
    </a-modal>
  </a-card>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import dayjs from 'dayjs'
import { message } from 'ant-design-vue'
import { customerApi, quoteApi } from '@/api'

const route = useRoute()
const router = useRouter()
const editId = route.params.id ? Number(route.params.id) : undefined
const isEdit = !!editId
let keySeed = 1

const form = reactive<any>({
  title: '',
  customerId: undefined,
  pricingMode: 'RULE',
  urgentFactor: 1,
  discountAmount: 0,
  validUntil: dayjs().add(30, 'day').format('YYYY-MM-DD'),
  remark: '',
  items: [] as any[]
})
const customers = ref<any[]>([])
const rules = ref<any[]>([])
const rulesOpen = ref(false)

const total = computed(() => (form.items as any[]).reduce((s, i) => s + Number(i.amount || 0), 0))
const final = computed(() => Math.max(0, total.value - Number(form.discountAmount || 0)))

function fmt(v: any) {
  return Number(v || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
function filterCustomer(input: string, option: any) {
  return (option?.label || '').includes(input)
}
function sum() {
  // 触发 final 计算即可
}
function addItem() {
  ;(form.items as any[]).push({
    _k: keySeed++,
    itemName: '',
    standardCode: '',
    spec: '',
    unit: '点次',
    qty: 1,
    unitPrice: 0,
    amount: 0,
    formula: ''
  })
}
/** 单行: 手工模式直接乘, 规则模式由后端试算时统一处理 */
function rowCalc(record: any) {
  if (form.pricingMode === 'MANUAL') {
    record.amount = Number(record.qty || 0) * Number(record.unitPrice || 0)
    record.formula = 'qty * unit_price(手工)'
  }
}
/** 调后端试算(规则计价) */
async function recalculate() {
  if (form.pricingMode !== 'RULE' || !form.items.length) return
  const items = JSON.parse(JSON.stringify(form.items.map(({ _k, ...rest }: any) => rest)))
  const res: any = await quoteApi.calculate(items, form.urgentFactor)
  form.items.forEach((it: any, idx: number) => {
    it.amount = res[idx]?.amount ?? 0
    it.formula = res[idx]?.formula ?? ''
  })
}

async function save(submit: boolean) {
  if (!form.title || !form.customerId) return message.warning('请完善标题与客户')
  if (!form.items.length) return message.warning('请添加检测项目')
  if (form.pricingMode === 'RULE') {
    await recalculate()
  }
  const payload = JSON.parse(JSON.stringify(form))
  payload.items?.forEach((i: any) => delete i._k)
  let id = editId
  if (editId) {
    payload.id = editId
    await quoteApi.save(payload)
  } else {
    id = (await quoteApi.save(payload)) as number
  }
  if (submit) {
    await quoteApi.submit(id)
    message.success('已提交审批')
  } else {
    message.success('草稿已保存')
  }
  router.push(`/quote/detail/${id}`)
}

onMounted(async () => {
  const res: any = await customerApi.page({ current: 1, size: 300, scope: 'all' })
  customers.value = res.records
  try {
    rules.value = (await quoteApi.rules()) as any
  } catch (e) {
    rules.value = []
  }
  if (editId) {
    const detail: any = await quoteApi.detail(editId)
    Object.assign(form, {
      title: detail.title,
      customerId: detail.customerId,
      pricingMode: detail.pricingMode,
      urgentFactor: Number(detail.urgentFactor),
      discountAmount: Number(detail.discountAmount),
      validUntil: detail.validUntil ? dayjs(detail.validUntil) : undefined,
      remark: detail.remark
    })
    form.items = (detail.items || []).map((i: any) => ({ ...i, _k: keySeed++ }))
  }
})
</script>
<style scoped>
.sub { color: #999; font-size: 12px; }
.sum-bar {
  display: flex;
  justify-content: flex-end;
  gap: 24px;
  align-items: center;
  margin-top: 12px;
  font-size: 14px;
}
.final { font-size: 18px; color: #f5222d; font-weight: 700; }
</style>
