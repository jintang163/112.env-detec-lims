<template>
  <div>
    <a-page-header :title="detail?.plan.code" :sub-title="detail?.plan.title" @back="router.back()">
      <template #extra>
        <a-space>
          <a-button
            v-if="detail?.plan.status === 'DRAFT' && userStore.hasPerm('sampling:plan:save')"
            @click="router.push(`/sampling/plan/edit/${id}`)"
          >编辑</a-button>
          <a-button
            v-if="detail?.plan.status === 'DRAFT' && userStore.hasPerm('sampling:plan:issue')"
            type="primary"
            @click="onIssue"
          >下发计划</a-button>
          <a-button
            v-if="canAssign"
            type="primary"
            @click="assignRef?.open(id, detail.plan.title)"
          >任务派工</a-button>
        </a-space>
      </template>
    </a-page-header>

    <a-card v-if="detail" size="small">
      <a-steps :current="currentStep" size="small" style="max-width: 720px; margin-bottom: 8px">
        <a-step title="计划制定" />
        <a-step title="计划下发" />
        <a-step title="现场采样" />
        <a-step title="样品交接" />
      </a-steps>
      <a-descriptions bordered :column="3" size="small" style="margin-top: 12px">
        <a-descriptions-item label="状态">
          <DictTag code="sampling_plan_status" :value="detail.plan.status" />
        </a-descriptions-item>
        <a-descriptions-item label="委托单">{{ detail.orderCode }}</a-descriptions-item>
        <a-descriptions-item label="客户">{{ detail.plan.customerName }}</a-descriptions-item>
        <a-descriptions-item label="采样日期">{{ detail.plan.planDate }}</a-descriptions-item>
        <a-descriptions-item label="计划时间">
          {{ fmt(detail.plan.startTime) }} ~ {{ fmt(detail.plan.endTime) }}
        </a-descriptions-item>
        <a-descriptions-item label="天气">{{ detail.plan.weather }}</a-descriptions-item>
        <a-descriptions-item label="联系人">{{ detail.plan.contactPerson }}</a-descriptions-item>
        <a-descriptions-item label="电话">{{ detail.plan.contactPhone }}</a-descriptions-item>
        <a-descriptions-item label="采样地址" :span="3">{{ detail.plan.address }}</a-descriptions-item>
        <a-descriptions-item label="备注" :span="3">{{ detail.plan.remark }}</a-descriptions-item>
      </a-descriptions>
    </a-card>

    <a-card v-if="detail" size="small" style="margin-top: 12px">
      <a-tabs>
        <a-tab-pane :tab-key="'tasks'">
          <template #title>采样任务({{ detail.tasks.length }})</template>
          <a-table :data-source="detail.tasks" :pagination="false" row-key="id" size="small">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'code'">
                <a @click="router.push(`/sampling/task?code=${record.code}`)">{{ record.code }}</a>
              </template>
              <template v-else-if="column.key === 'status'">
                <DictTag code="sampling_task_status" :value="record.status" />
              </template>
            </template>
            <a-table-column key="code" title="任务编号" />
            <a-table-column key="assigneeName" title="采样员" data-index="assigneeName" />
            <a-table-column key="assignedAt" title="派工时间" data-index="assignedAt" />
            <a-table-column key="downloadedAt" title="下载时间" data-index="downloadedAt" />
            <a-table-column key="status" title="状态" />
          </a-table>
        </a-tab-pane>

        <a-tab-pane>
          <template #title>点位清单({{ detail.points.length }})</template>
          <a-table :data-source="detail.points" :pagination="false" row-key="id" size="small">
            <a-table-column key="sortNo" title="序号" data-index="sortNo" width="70" />
            <a-table-column key="name" title="点位名称" data-index="name" />
            <a-table-column key="addrDesc" title="位置描述" data-index="addrDesc" />
            <a-table-column key="lng" title="经度" data-index="lng" />
            <a-table-column key="lat" title="纬度" data-index="lat" />
          </a-table>
        </a-tab-pane>

        <a-tab-pane>
          <template #title>检测项/样品要求({{ detail.items.length }})</template>
          <a-table :data-source="detail.items" :pagination="false" row-key="id" size="small">
            <a-table-column key="itemName" title="检测项目" data-index="itemName" />
            <a-table-column key="sampleName" title="样品名称" data-index="sampleName" />
            <a-table-column key="sampleQty" title="数量" data-index="sampleQty" width="80" />
            <a-table-column key="container" title="容器" data-index="container" />
            <a-table-column key="preservation" title="保存条件" data-index="preservation" />
            <a-table-column key="qcRequired" title="质控" width="80">
              <template #default="{ record }">
                <a-tag v-if="record.qcRequired === 1" color="purple">质控</a-tag>
                <span v-else>-</span>
              </template>
            </a-table-column>
          </a-table>
        </a-tab-pane>

        <a-tab-pane>
          <template #title>携带设备({{ detail.equipments.length }})</template>
          <a-table :data-source="detail.equipments" :pagination="false" row-key="id" size="small">
            <a-table-column key="equipmentName" title="设备/容器" data-index="equipmentName" />
            <a-table-column key="qty" title="数量" data-index="qty" width="100" />
          </a-table>
        </a-tab-pane>

        <a-tab-pane>
          <template #title>现场样品({{ samples.length }})</template>
          <a-table :data-source="samples" :pagination="false" row-key="id" size="small">
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'sampleCode'">
                <a-popover>
                  <template #content>
                    <img v-if="qrMap[record.sample.sampleCode]" :src="qrMap[record.sample.sampleCode]" width="160" />
                  </template>
                  <a>{{ record.sample.sampleCode }} <QrcodeOutlined /></a>
                </a-popover>
              </template>
              <template v-else-if="column.key === 'qc'">
                <a-tag v-if="record.sample.isQc === 1" color="purple">
                  {{ qcLabel(record.sample.qcType) }}
                </a-tag>
                <span v-else>-</span>
              </template>
              <template v-else-if="column.key === 'photos'">
                <a-image
                  v-for="p in record.photos"
                  :key="p.id"
                  :src="p.url"
                  :width="44"
                  height="44"
                  style="object-fit: cover; margin-right: 4px"
                />
                <span v-if="!record.photos?.length" class="muted">-</span>
              </template>
              <template v-else-if="column.key === 'status'">
                <DictTag code="sample_status" :value="record.sample.status" />
              </template>
            </template>
            <a-table-column key="sampleCode" title="样品编号" />
            <a-table-column key="pointName" title="点位" data-index="pointName" />
            <a-table-column key="sampleName">
              <template #title>样品/项目</template>
              <template #default="{ record }">
                {{ record.sample.sampleName }} / {{ record.sample.itemName }}
              </template>
            </a-table-column>
            <a-table-column key="temperature">
              <template #title>温度/pH</template>
              <template #default="{ record }">
                {{ record.sample.temperature ?? '-' }}℃ / {{ record.sample.ph ?? '-' }}
              </template>
            </a-table-column>
            <a-table-column key="storageCondition" title="保存条件" data-index="storageCondition">
              <template #default="{ record }">{{ record.sample.storageCondition || '-' }}</template>
            </a-table-column>
            <a-table-column key="qc" title="质控" />
            <a-table-column key="photos" title="现场照片" />
            <a-table-column key="status" title="状态" width="100" />
          </a-table>
        </a-tab-pane>
      </a-tabs>
    </a-card>

    <AssignModal ref="assignRef" @saved="reload" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import QRCode from 'qrcode'
import { QrcodeOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { samplingPlanApi, samplingTaskApi } from '@/api'
import { loadDict, type DictItem } from '@/composables/useDict'
import { useUserStore } from '@/stores/user'
import DictTag from '@/components/DictTag.vue'
import AssignModal from '../task/components/AssignModal.vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const id = String(route.params.id)
const detail = ref<any>(null)
const samples = ref<any[]>([])
const qrMap = ref<Record<string, string>>({})
const assignRef = ref<InstanceType<typeof AssignModal>>()
let qcDict: DictItem[] = []

const currentStep = computed(() => {
  const p = detail.value?.plan
  if (!p) return 0
  if (p.status === 'DRAFT') return 0
  const task = detail.value.tasks?.[0]
  if (p.status === 'ISSUED' && !task) return 1
  if (task?.status === 'HANDED') return 4
  if (task && ['SUBMITTED'].includes(task.status)) return 3
  return 2
})
const canAssign = computed(
  () =>
    detail.value?.plan.status === 'ISSUED' &&
    !detail.value.tasks.some((t: any) => ['ASSIGNED', 'SUBMITTED'].includes(t.status)) &&
    userStore.hasPerm('sampling:task:assign')
)

function fmt(t: string) {
  return t ? t.replace('T', ' ').slice(0, 16) : '-'
}
function qcLabel(v: string) {
  return qcDict.find((d) => d.itemValue === v)?.itemLabel || '质控'
}

async function reload() {
  detail.value = await samplingPlanApi.detail(id)
  const taskSamples = await Promise.all(
    detail.value.tasks.map((t: any) => samplingTaskApi.detail(t.id))
  )
  samples.value = taskSamples.flatMap((d: any) => d.samples || [])
  for (const s of samples.value) {
    if (s.sample?.sampleCode) {
      qrMap.value[s.sample.sampleCode] = await QRCode.toDataURL(s.sample.sampleCode, {
        width: 160,
        margin: 1
      })
    }
  }
}

async function onIssue() {
  await samplingPlanApi.issue(Number(id))
  message.success('计划已下发')
  reload()
}

onMounted(async () => {
  qcDict = await loadDict('qc_type')
  reload()
})
</script>

<style scoped>
.muted {
  color: #999;
}
</style>
