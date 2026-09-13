<template>
  <div>
    <a-card>
      <a-form layout="inline" class="page-toolbar">
        <a-form-item label="关键字">
          <a-input v-model:value="query.keyword" placeholder="名称/编号/联系人" allow-clear @press-enter="reload" />
        </a-form-item>
        <a-form-item label="分级">
          <a-select v-model:value="query.customerLevel" allow-clear style="width: 100px" @change="reload">
            <a-select-option value="A">A级</a-select-option>
            <a-select-option value="B">B级</a-select-option>
            <a-select-option value="C">C级</a-select-option>
            <a-select-option value="D">D级</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="类型">
          <a-select v-model:value="query.customerType" allow-clear style="width: 130px" @change="reload">
            <a-select-option :value="10">企业</a-select-option>
            <a-select-option :value="20">政府事业</a-select-option>
            <a-select-option :value="30">个人</a-select-option>
          </a-select>
        </a-form-item>
        <a-radio-group v-model:value="query.scope" button-style="solid" @change="reload">
          <a-radio-button value="all">全部</a-radio-button>
          <a-radio-button value="mine">我的客户</a-radio-button>
          <a-radio-button value="pool">公海客户</a-radio-button>
        </a-radio-group>
        <a-space>
          <a-button @click="reset">重置</a-button>
          <a-button type="primary" @click="openEdit()" v-if="userStore.hasPerm('customer:add')">新建客户</a-button>
        </a-space>
      </a-form>

      <a-table
        :data-source="data"
        :columns="columns"
        row-key="id"
        :loading="loading"
        :pagination="pagination"
        @change="onPage"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'name'">
            <a @click="openDetail(record)">{{ record.name }}</a>
            <div class="sub">{{ record.code }}</div>
          </template>
          <template v-else-if="column.key === 'customerLevel'">
            <a-tag :color="levelColor(record.customerLevel)">{{ record.customerLevel }}级</a-tag>
            <span class="sub">信用分 {{ record.creditScore }}</span>
          </template>
          <template v-else-if="column.key === 'contact'">
            {{ record.contactPerson || '-' }} <span class="sub">{{ record.contactPhone }}</span>
          </template>
          <template v-else-if="column.key === 'owner'">
            <a-tag v-if="!record.ownerUserId" color="cyan">公海</a-tag>
            <span v-else>{{ record.ownerName }}</span>
          </template>
          <template v-else-if="column.key === 'status'">
            <a-badge :status="record.status === 1 ? 'success' : 'error'" :text="record.status === 1 ? '正常' : '停用/黑名单'" />
          </template>
          <template v-else-if="column.key === 'action'">
            <a-space>
              <a @click="openDetail(record)">详情</a>
              <a v-if="!record.ownerUserId && userStore.hasPerm('customer:pool')" @click="claim(record)">认领</a>
              <a-dropdown v-if="record.ownerUserId">
                <a>更多 <DownOutlined /></a>
                <template #overlay>
                  <a-menu>
                    <a-menu-item v-if="userStore.isAdmin || userStore.roles.includes('ROLE_MANAGER')" @click="openTransfer(record)">分配/转交</a-menu-item>
                    <a-menu-item v-if="userStore.hasPerm('customer:pool')" @click="release(record)">退回公海</a-menu-item>
                    <a-menu-divider />
                    <a-menu-item @click="toggleStatus(record)">{{ record.status === 1 ? '停用/拉黑' : '恢复正常' }}</a-menu-item>
                  </a-menu>
                </template>
              </a-dropdown>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <CustomerEdit ref="editRef" @saved="reload" />
    <CustomerDetail ref="detailRef" @edit="openEdit" />
    <TransferModal ref="transferRef" @saved="reload" />
  </div>
</template>

<script setup lang="ts">
import { h, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { DownOutlined } from '@ant-design/icons-vue'
import { message, Modal } from 'ant-design-vue'
import { customerApi } from '@/api'
import { useUserStore } from '@/stores/user'
import CustomerEdit from './components/CustomerEdit.vue'
import CustomerDetail from './components/CustomerDetail.vue'
import TransferModal from './components/TransferModal.vue'

const route = useRoute()
const userStore = useUserStore()
const loading = ref(false)
const data = ref<any[]>([])
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true })
const query = reactive<any>({
  keyword: (route.query.keyword as string) || '',
  scope: 'all',
  current: 1,
  size: 10
})

const columns = [
  { title: '客户名称', key: 'name' },
  { title: '分级/信用', key: 'customerLevel', width: 120 },
  { title: '联系人', key: 'contact', width: 180 },
  { title: '行业', dataIndex: 'industry', width: 120 },
  { title: '归属', key: 'owner', width: 110 },
  { title: '状态', key: 'status', width: 120 },
  { title: '操作', key: 'action', width: 150 }
]

function levelColor(l: string) {
  return { A: 'red', B: 'orange', C: 'blue', D: 'default' }[l] || 'default'
}

async function reload() {
  loading.value = true
  try {
    query.current = pagination.current
    query.size = pagination.pageSize
    const res: any = await customerApi.page(query)
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
function reset() {
  Object.assign(query, { keyword: '', customerLevel: undefined, customerType: undefined })
  reload()
}

const editRef = ref()
function openEdit(record?: any) {
  editRef.value?.open(record?.id)
}
const detailRef = ref()
function openDetail(record: any) {
  detailRef.value?.open(record.id)
}
const transferRef = ref()
function openTransfer(record: any) {
  transferRef.value?.open(record)
}

async function claim(record: any) {
  await customerApi.claim(record.id)
  message.success('认领成功')
  reload()
}
async function release(record: any) {
  const remark = await new Promise<string | null>((resolve) => {
    let val = ''
    Modal.confirm({
      title: `将「${record.name}」退回公海?`,
      content: () =>
        h('textarea', {
          class: 'ant-input',
          rows: 2,
          placeholder: '退回原因(可选)',
          onInput: (e: any) => (val = e.target.value)
        }),
      onOk: () => resolve(val),
      onCancel: () => resolve(null as any)
    })
  })
  if (remark === null) return
  await customerApi.release(record.id, remark)
  message.success('已退回公海')
  reload()
}
async function toggleStatus(record: any) {
  await customerApi.setStatus(record.id, record.status === 1 ? 0 : 1)
  message.success('操作成功')
  reload()
}

onMounted(reload)
</script>
<style scoped>
.sub { color: #999; font-size: 12px; }
</style>
