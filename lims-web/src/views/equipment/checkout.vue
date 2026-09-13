<template>
  <a-card title="设备领用归还">
    <a-space style="margin-bottom: 16px">
      <a-button
        v-if="userStore.hasPerm('equipment:checkout')"
        type="primary"
        @click="openCheckout"
      >设备领用</a-button>
      <a-radio-group v-model:value="query.status" button-style="solid" @change="reload">
        <a-radio-button value="">全部</a-radio-button>
        <a-radio-button value="BORROWED">已领用</a-radio-button>
        <a-radio-button value="RETURNED">已归还</a-radio-button>
      </a-radio-group>
    </a-space>

    <a-table
      :data-source="data"
      :columns="columns"
      row-key="id"
      :loading="loading"
      :pagination="pagination"
      @change="onPage"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-tag :color="record.status === 'BORROWED' ? 'orange' : 'green'">
            {{ record.status === 'BORROWED' ? '已领用' : '已归还' }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'checkResult'">
          <a-tag v-if="record.checkResult" :color="resultColor[record.checkResult]">
            {{ resultLabel[record.checkResult] }}
          </a-tag>
          <span v-else>-</span>
        </template>
        <template v-else-if="column.key === 'action'">
          <a
            v-if="record.status === 'BORROWED' && userStore.hasPerm('equipment:checkout')"
            @click="openReturn(record)"
            >归还检查</a>
          <span v-else>-</span>
        </template>
      </template>
    </a-table>

    <!-- 领用登记弹窗 -->
    <a-modal
      v-model:open="checkoutOpen"
      title="设备领用登记"
      :confirm-loading="saving"
      @ok="onCheckout"
    >
      <a-form layout="vertical" style="margin-top: 12px">
        <a-form-item label="设备/容器" required>
          <a-select
            v-model:value="checkoutForm.equipmentId"
            show-search
            option-filter-prop="label"
            placeholder="选择可用设备"
          >
            <a-select-option
              v-for="e in availableEquips"
              :key="e.id"
              :value="e.id"
              :label="`${e.code} ${e.name}`"
            >
              {{ e.code }} {{ e.name }}（可用 {{ e.qtyAvailable }} {{ e.unit }}）
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-row :gutter="12">
          <a-col :span="12">
            <a-form-item label="领用数量" required>
              <a-input-number v-model:value="checkoutForm.qty" :min="1" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="预计归还">
              <a-date-picker
                v-model:value="checkoutForm.expectedReturnTime"
                show-time
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
        </a-row>
        <a-form-item label="领用备注">
          <a-textarea v-model:value="checkoutForm.checkoutRemark" :rows="2" />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 归还检查弹窗 -->
    <a-modal v-model:open="returnOpen" title="归还检查" :confirm-loading="saving" @ok="onReturn">
      <a-descriptions bordered :column="1" size="small" style="margin: 12px 0">
        <a-descriptions-item label="设备">{{ returnRecord?.equipmentName }}</a-descriptions-item>
        <a-descriptions-item label="领用数量">{{ returnRecord?.qty }}</a-descriptions-item>
        <a-descriptions-item label="领用人">{{ returnRecord?.checkoutByName }}</a-descriptions-item>
      </a-descriptions>
      <a-form layout="vertical">
        <a-form-item label="检查结果" required>
          <a-radio-group v-model:value="returnForm.checkResult">
            <a-radio value="OK">完好(回补库存)</a-radio>
            <a-radio value="DAMAGED">损坏(不回补)</a-radio>
            <a-radio value="MISSING">缺失(不回补)</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="备注">
          <a-textarea v-model:value="returnForm.returnRemark" :rows="2" />
        </a-form-item>
      </a-form>
    </a-modal>
  </a-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { message } from 'ant-design-vue'
import { equipmentApi } from '@/api'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const loading = ref(false)
const saving = ref(false)
const data = ref<any[]>([])
const availableEquips = ref<any[]>([])
const checkoutOpen = ref(false)
const returnOpen = ref(false)
const returnRecord = ref<any>(null)
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true })
const query = reactive<any>({ status: '', current: 1, size: 10 })

const resultLabel: Record<string, string> = {
  OK: '完好',
  DAMAGED: '损坏',
  MISSING: '缺失'
}
const resultColor: Record<string, string> = {
  OK: 'green',
  DAMAGED: 'orange',
  MISSING: 'red'
}

const checkoutForm = reactive<any>({
  equipmentId: undefined,
  qty: 1,
  expectedReturnTime: null,
  checkoutRemark: ''
})
const returnForm = reactive<any>({ checkResult: 'OK', returnRemark: '' })

const columns = [
  { title: '设备', key: 'equipmentName', dataIndex: 'equipmentName' },
  { title: '数量', key: 'qty', dataIndex: 'qty', width: 80 },
  { title: '领用人', key: 'checkoutByName', dataIndex: 'checkoutByName', width: 100 },
  { title: '领用时间', key: 'checkoutTime', dataIndex: 'checkoutTime', width: 170 },
  { title: '预计归还', key: 'expectedReturnTime', dataIndex: 'expectedReturnTime', width: 170 },
  { title: '归还时间', key: 'returnTime', dataIndex: 'returnTime', width: 170 },
  { title: '归还人', key: 'returnByName', dataIndex: 'returnByName', width: 100 },
  { title: '检查结果', key: 'checkResult', width: 100 },
  { title: '状态', key: 'status', width: 90 },
  { title: '操作', key: 'action', width: 100 }
]

async function reload() {
  loading.value = true
  try {
    query.current = pagination.current
    query.size = pagination.pageSize
    const res: any = await equipmentApi.checkoutPage(query)
    data.value = res.records
    pagination.total = res.total
  } finally {
    loading.value = false
  }
}

function onPage(p: any) {
  pagination.current = p.current
  pagination.pageSize = p.pageSize
  reload()
}

async function openCheckout() {
  Object.assign(checkoutForm, {
    equipmentId: undefined,
    qty: 1,
    expectedReturnTime: null,
    checkoutRemark: ''
  })
  availableEquips.value = (await equipmentApi.options()) as any[]
  checkoutOpen.value = true
}

async function onCheckout() {
  if (!checkoutForm.equipmentId) return message.warning('请选择设备')
  saving.value = true
  try {
    await equipmentApi.checkout({ ...checkoutForm })
    message.success('领用登记成功')
    checkoutOpen.value = false
    reload()
  } finally {
    saving.value = false
  }
}

function openReturn(record: any) {
  returnRecord.value = record
  returnForm.checkResult = 'OK'
  returnForm.returnRemark = ''
  returnOpen.value = true
}

async function onReturn() {
  saving.value = true
  try {
    await equipmentApi.doReturn(returnRecord.value.id, { ...returnForm })
    message.success('归还完成')
    returnOpen.value = false
    reload()
  } finally {
    saving.value = false
  }
}

onMounted(reload)
</script>
