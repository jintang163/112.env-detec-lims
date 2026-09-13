<template>
  <div>
    <a-page-header :title="isEdit ? '编辑采样计划' : '制定采样计划'" @back="router.back()">
      <template #extra>
        <a-space>
          <a-button @click="router.back()">取消</a-button>
          <a-button
            type="primary"
            :loading="saving"
            @click="onSave(false)"
          >保存草稿</a-button>
          <a-button
            type="primary"
            danger
            :loading="saving"
            @click="onSave(true)"
          >保存并下发</a-button>
        </a-space>
      </template>
    </a-page-header>

    <a-card size="small" title="基本信息" style="margin-top: 12px">
      <a-form layout="vertical">
        <a-row :gutter="16">
          <a-col :span="8">
            <a-form-item label="委托单" required>
              <a-select
                v-model:value="form.orderId"
                show-search
                option-filter-prop="label"
                placeholder="选择已受理/采样中的委托单"
                :disabled="isEdit"
                @change="onOrderChange"
              >
                <a-select-option
                  v-for="o in orderOptions"
                  :key="o.order.id"
                  :value="o.order.id"
                  :label="`${o.order.code} ${o.order.title}`"
                >
                  {{ o.order.code }} - {{ o.order.title }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item label="计划名称" required>
              <a-input v-model:value="form.title" placeholder="默认取委托项目名称" />
            </a-form-item>
          </a-col>
          <a-col :span="4">
            <a-form-item label="采样日期" required>
              <a-date-picker
                v-model:value="form.planDate"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
          <a-col :span="4">
            <a-form-item label="天气">
              <a-input v-model:value="form.weather" placeholder="如 多云" />
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item label="计划开始">
              <a-date-picker
                v-model:value="form.startTime"
                show-time
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item label="计划结束">
              <a-date-picker
                v-model:value="form.endTime"
                show-time
                value-format="YYYY-MM-DD HH:mm:ss"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item label="现场联系人">
              <a-input :value="snapshot.contactPerson" disabled />
            </a-form-item>
          </a-col>
          <a-col :span="6">
            <a-form-item label="联系电话">
              <a-input :value="snapshot.contactPhone" disabled />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="采样地址">
              <a-input :value="snapshot.address" disabled />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="备注">
              <a-input v-model:value="form.remark" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-card>

    <a-card size="small" title="点位清单" style="margin-top: 12px">
      <a-table
        :data-source="form.points"
        :pagination="false"
        row-key="rowKey"
        size="small"
        :columns="pointCols"
      >
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'name'">
            <a-input v-model:value="record.name" placeholder="点位名称" />
          </template>
          <template v-else-if="column.key === 'lng'">
            <a-input-number v-model:value="record.lng" :precision="7" style="width: 100%" />
          </template>
          <template v-else-if="column.key === 'lat'">
            <a-input-number v-model:value="record.lat" :precision="7" style="width: 100%" />
          </template>
          <template v-else-if="column.key === 'addrDesc'">
            <a-input v-model:value="record.addrDesc" />
          </template>
          <template v-else-if="column.key === 'sortNo'">{{ index + 1 }}</template>
          <template v-else-if="column.key === 'action'">
            <a-popconfirm title="删除该点位?" @confirm="removeRow(form.points, index)">
              <a style="color: #f5222d">删除</a>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
      <a-button type="dashed" block style="margin-top: 8px" @click="addPoint">+ 添加点位</a-button>
    </a-card>

    <a-card size="small" title="检测项 / 样品要求" style="margin-top: 12px">
      <a-table
        :data-source="form.items"
        :pagination="false"
        row-key="rowKey"
        size="small"
        :columns="itemCols"
      >
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'itemName'">
            <a-input v-model:value="record.itemName" />
          </template>
          <template v-else-if="column.key === 'sampleName'">
            <a-input v-model:value="record.sampleName" />
          </template>
          <template v-else-if="column.key === 'sampleQty'">
            <a-input-number v-model:value="record.sampleQty" :min="0" style="width: 100%" />
          </template>
          <template v-else-if="column.key === 'container'">
            <a-input v-model:value="record.container" placeholder="如 棕色玻璃瓶" />
          </template>
          <template v-else-if="column.key === 'preservation'">
            <a-select v-model:value="record.preservation" allow-clear placeholder="保存条件">
              <a-select-option
                v-for="d in storageDict"
                :key="d.itemValue"
                :value="d.itemValue"
              >{{ d.itemLabel }}</a-select-option>
            </a-select>
          </template>
          <template v-else-if="column.key === 'qcRequired'">
            <a-switch v-model:checked="record.qcRequired" :checked-value="1" :un-checked-value="0" />
          </template>
          <template v-else-if="column.key === 'action'">
            <a-popconfirm title="删除该检测项?" @confirm="removeRow(form.items, index)">
              <a style="color: #f5222d">删除</a>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
      <a-button type="dashed" block style="margin-top: 8px" @click="addItem">+ 添加检测项</a-button>
    </a-card>

    <a-card size="small" title="携带设备 / 容器" style="margin: 12px 0">
      <a-table
        :data-source="form.equipments"
        :pagination="false"
        row-key="rowKey"
        size="small"
        :columns="equipCols"
      >
        <template #bodyCell="{ column, record, index }">
          <template v-if="column.key === 'equipmentId'">
            <a-select
              v-model:value="record.equipmentId"
              show-search
              option-filter-prop="label"
              placeholder="选择设备/容器"
              style="width: 100%"
              @change="(v: number) => onEquipChange(record, v)"
            >
              <a-select-option
                v-for="e in equipOptions"
                :key="e.id"
                :value="e.id"
                :disabled="e.qtyAvailable <= 0"
                :label="`${e.code} ${e.name}`"
              >
                {{ e.code }} {{ e.name }}（可用 {{ e.qtyAvailable }} {{ e.unit }}）
              </a-select-option>
            </a-select>
          </template>
          <template v-else-if="column.key === 'qty'">
            <a-input-number v-model:value="record.qty" :min="1" style="width: 100%" />
          </template>
          <template v-else-if="column.key === 'action'">
            <a-popconfirm title="删除?" @confirm="removeRow(form.equipments, index)">
              <a style="color: #f5222d">删除</a>
            </a-popconfirm>
          </template>
        </template>
      </a-table>
      <a-button type="dashed" block style="margin-top: 8px" @click="addEquip">+ 添加设备</a-button>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { entrustApi, equipmentApi, samplingPlanApi } from '@/api'
import { loadDict, type DictItem } from '@/composables/useDict'

const route = useRoute()
const router = useRouter()
const planId = ref<string>((route.params.id as string) || '')
const isEdit = !!planId.value
const saving = ref(false)
const orderOptions = ref<any[]>([])
const equipOptions = ref<any[]>([])
const storageDict = ref<DictItem[]>([])
let rowSeq = 0
const nextKey = () => `r${++rowSeq}`

const snapshot = reactive<any>({ contactPerson: '', contactPhone: '', address: '' })
const form = reactive<any>({
  id: undefined,
  orderId: undefined,
  title: '',
  planDate: '',
  startTime: null,
  endTime: null,
  weather: '',
  remark: '',
  points: [] as any[],
  items: [] as any[],
  equipments: [] as any[]
})

const pointCols = [
  { title: '序号', key: 'sortNo', width: 60 },
  { title: '点位名称', key: 'name' },
  { title: '经度(BD-09)', key: 'lng', width: 180 },
  { title: '纬度(BD-09)', key: 'lat', width: 180 },
  { title: '位置描述', key: 'addrDesc' },
  { title: '操作', key: 'action', width: 70 }
]
const itemCols = [
  { title: '检测项目', key: 'itemName' },
  { title: '样品名称', key: 'sampleName', width: 130 },
  { title: '数量', key: 'sampleQty', width: 90 },
  { title: '容器', key: 'container', width: 150 },
  { title: '保存条件', key: 'preservation', width: 150 },
  { title: '质控样', key: 'qcRequired', width: 80 },
  { title: '操作', key: 'action', width: 70 }
]
const equipCols = [
  { title: '设备/容器', key: 'equipmentId' },
  { title: '数量', key: 'qty', width: 120 },
  { title: '操作', key: 'action', width: 70 }
]

async function onOrderChange(orderId: number) {
  const detail: any = await entrustApi.detail(orderId)
  const o = detail.order
  form.title = form.title || o.title
  form.planDate = form.planDate || (o.plannedSamplingTime || '').slice(0, 10)
  form.startTime = form.startTime || o.plannedSamplingTime
  snapshot.contactPerson = o.contactPerson
  snapshot.contactPhone = o.contactPhone
  snapshot.address = [o.province, o.city, o.district, o.samplingAddress].filter(Boolean).join('')
  form.points = (detail.points || []).map((p: any) => ({ ...p, rowKey: nextKey() }))
  form.items = (detail.items || []).map((it: any) => ({
    orderItemId: it.id,
    itemName: it.itemName,
    sampleName: it.sampleName,
    sampleQty: it.sampleQty,
    container: null,
    preservation: null,
    qcRequired: 0,
    rowKey: nextKey()
  }))
}

function onEquipChange(record: any, equipmentId: number) {
  const e = equipOptions.value.find((x) => x.id === equipmentId)
  record.equipmentName = e?.name
}

function addPoint() {
  form.points.push({ name: '', lng: null, lat: null, addrDesc: '', rowKey: nextKey() })
}
function addItem() {
  form.items.push({
    itemName: '',
    sampleName: '',
    sampleQty: 1,
    container: null,
    preservation: null,
    qcRequired: 0,
    rowKey: nextKey()
  })
}
function addEquip() {
  form.equipments.push({ equipmentId: null, equipmentName: '', qty: 1, rowKey: nextKey() })
}
function removeRow(list: any[], idx: number) {
  list.splice(idx, 1)
}

async function onSave(issue: boolean) {
  if (!form.orderId) return message.warning('请选择委托单')
  if (!form.title) return message.warning('请填写计划名称')
  if (!form.planDate) return message.warning('请选择采样日期')
  if (!form.points.length) return message.warning('至少添加一个采样点位')
  saving.value = true
  try {
    const payload = {
      id: form.id,
      orderId: form.orderId,
      title: form.title,
      planDate: form.planDate,
      startTime: form.startTime,
      endTime: form.endTime,
      weather: form.weather,
      remark: form.remark,
      points: form.points.map((p: any, i: number) => ({
        pointId: p.pointId,
        name: p.name,
        lng: p.lng,
        lat: p.lat,
        addrDesc: p.addrDesc,
        sortNo: i + 1
      })),
      items: form.items.map((it: any, i: number) => ({ ...it, sortNo: i + 1 })),
      equipments: form.equipments
        .filter((e: any) => e.equipmentId)
        .map((e: any) => ({ equipmentId: e.equipmentId, equipmentName: e.equipmentName, qty: e.qty }))
    }
    let id = Number(planId.value)
    if (isEdit) {
      await samplingPlanApi.save(payload)
    } else {
      id = await samplingPlanApi.save(payload)
    }
    if (issue) {
      await samplingPlanApi.issue(id)
      message.success('计划已保存并下发')
    } else {
      message.success('草稿已保存')
    }
    router.replace(`/sampling/plan/detail/${id}`)
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  storageDict.value = await loadDict('storage_condition')
  equipOptions.value = (await equipmentApi.options()) as any[]
  // 委托单候选: 已受理/采样中
  const res: any = await entrustApi.page({ current: 1, size: 100 })
  orderOptions.value = res.records.filter(
    (r: any) => ['ACCEPTED', 'SAMPLING'].includes(r.order.status)
  )
  if (isEdit) {
    const d: any = await samplingPlanApi.detail(planId.value)
    form.id = d.plan.id
    form.orderId = d.plan.orderId
    form.title = d.plan.title
    form.planDate = d.plan.planDate
    form.startTime = d.plan.startTime
    form.endTime = d.plan.endTime
    form.weather = d.plan.weather
    form.remark = d.plan.remark
    snapshot.contactPerson = d.plan.contactPerson
    snapshot.contactPhone = d.plan.contactPhone
    snapshot.address = d.plan.address
    form.points = (d.points || []).map((p: any) => ({ ...p, rowKey: nextKey() }))
    form.items = (d.items || []).map((it: any) => ({ ...it, rowKey: nextKey() }))
    form.equipments = (d.equipments || []).map((e: any) => ({ ...e, rowKey: nextKey() }))
  }
})
</script>
