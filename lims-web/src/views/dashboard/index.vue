<template>
  <div>
    <a-row :gutter="16">
      <a-col :span="6" v-for="card in cards" :key="card.label">
        <a-card class="stat-card">
          <a-statistic :title="card.label" :value="card.value" :value-style="{ color: card.color }">
            <template #prefix><component :is="card.icon" /></template>
          </a-statistic>
        </a-card>
      </a-col>
    </a-row>

    <a-row :gutter="16" style="margin-top: 16px">
      <a-col :span="10">
        <a-card title="委托单状态分布">
          <EChart :option="pieOption" height="340px" />
        </a-card>
      </a-col>
      <a-col :span="14">
        <a-card title="合同回款概览(履约中/已审批)">
          <EChart :option="paymentOption" height="340px" />
        </a-card>
      </a-col>
    </a-row>

    <a-row :gutter="16" style="margin-top: 16px">
      <a-col :span="14">
        <a-card title="报告临期预警(7天内)">
          <a-table
            :data-source="expiring"
            :pagination="false"
            size="small"
            row-key="id"
            :columns="expireCols"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'code'">
                <a @click="router.push(`/entrust/detail/${record.id}`)">{{ record.code }}</a>
              </template>
              <template v-else-if="column.key === 'urgency'">
                <a-tag v-if="record.urgency === 'URGENT'" color="red" class="urgent-tag">加急</a-tag>
                <a-tag v-else color="default">普通</a-tag>
              </template>
              <template v-else-if="column.key === 'expectedReportDate'">
                {{ record.expectedReportDate }}
              </template>
            </template>
          </a-table>
        </a-card>
      </a-col>
      <a-col :span="10">
        <a-card title="客户资质过期预警">
          <a-list :data-source="quals" :locale="{ emptyText: '暂无预警' }" size="small">
            <template #renderItem="{ item }">
              <a-list-item>
                <a-list-item-meta>
                  <template #title>
                    <a-tag :color="item.validStatus === 3 ? 'red' : 'orange'">
                      {{ item.validStatus === 3 ? '已过期' : '30天内到期' }}
                    </a-tag>
                    {{ item.qualType }}
                  </template>
                  <template #description>
                    证书号 {{ item.certNo || '-' }} · 有效期至 {{ item.validTo }}
                  </template>
                </a-list-item-meta>
              </a-list-item>
            </template>
          </a-list>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  AuditOutlined,
  TeamOutlined,
  FileProtectOutlined,
  ThunderboltOutlined
} from '@ant-design/icons-vue'
import dayjs from 'dayjs'
import EChart from '@/components/EChart.vue'
import { dashboardApi } from '@/api'
import { loadDict } from '@/composables/useDict'

const router = useRouter()
const summary = ref<any>({})
const statusRows = ref<any[]>([])
const expiring = ref<any[]>([])
const quals = ref<any[]>([])

const cards = computed(() => [
  { label: '本月委托单', value: summary.value.entrustMonth ?? 0, icon: markRaw(AuditOutlined), color: '#1677ff' },
  { label: '在途加急单', value: summary.value.entrustUrgent ?? 0, icon: markRaw(ThunderboltOutlined), color: '#f5222d' },
  { label: '履约中合同', value: summary.value.contractExecuting ?? 0, icon: markRaw(FileProtectOutlined), color: '#52c41a' },
  { label: '客户总数 / 公海', value: `${summary.value.customerTotal ?? 0} / ${summary.value.poolTotal ?? 0}`, icon: markRaw(TeamOutlined), color: '#722ed1' }
])

import { markRaw } from 'vue'

const STATUS_COLORS: Record<string, string> = {
  DRAFT: '#bfbfbf',
  REVIEWING: '#1677ff',
  REVIEW_REJECTED: '#f5222d',
  ACCEPTED: '#13c2c2',
  SAMPLING: '#2f54eb',
  TESTING: '#2060df',
  REPORTING: '#722ed1',
  COMPLETED: '#52c41a',
  CANCELLED: '#8c8c8c'
}

const pieOption = computed(() => {
  const data = statusRows.value.map((r: any) => ({
    name: r.label || r.status,
    value: Number(r.cnt),
    itemStyle: { color: STATUS_COLORS[r.status] }
  }))
  return {
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, type: 'scroll' },
    series: [
      {
        type: 'pie',
        radius: ['38%', '66%'],
        center: ['50%', '44%'],
        label: { formatter: '{b}\n{c}单' },
        data
      }
    ]
  }
})

const paymentOption = computed(() => {
  const p = summary.value.payment || {}
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    legend: { bottom: 0 },
    grid: { left: 60, right: 20, top: 30, bottom: 40 },
    xAxis: { type: 'category', data: ['合同总额', '已收款', '已开票', '待收款'] },
    yAxis: { type: 'value', name: '元' },
    series: [
      {
        type: 'bar',
        barWidth: 42,
        data: [
          { value: p.amount || 0, itemStyle: { color: '#1677ff' } },
          { value: p.received || 0, itemStyle: { color: '#52c41a' } },
          { value: p.invoiced || 0, itemStyle: { color: '#722ed1' } },
          { value: p.unreceived || 0, itemStyle: { color: '#fa8c16' } }
        ],
        label: { show: true, position: 'top', formatter: (v: any) => (v.value / 10000).toFixed(1) + '万' }
      }
    ]
  }
})

const expireCols = [
  { title: '委托单号', key: 'code' },
  { title: '项目名称', dataIndex: 'title', ellipsis: true },
  { title: '紧急程度', key: 'urgency', width: 90 },
  { title: '期望报告日', key: 'expectedReportDate', width: 120 }
]

onMounted(async () => {
  const dict = await loadDict('entrust_status')
  const [s, st, ex, q] = await Promise.all([
    dashboardApi.summary(),
    dashboardApi.entrustStatus(),
    dashboardApi.expiring(),
    dashboardApi.qualWarnings()
  ])
  summary.value = s
  const labelMap = new Map(dict.map((d: any) => [d.itemValue, d.itemLabel]))
  statusRows.value = (st as any[]).map((r) => ({ ...r, label: labelMap.get(r.status) }))
  expiring.value = (ex as any[]).map((r) => ({
    ...r,
    expectedReportDate: dayjs(r.expectedReportDate).format('YYYY-MM-DD')
  }))
  quals.value = q as any[]
})
</script>
