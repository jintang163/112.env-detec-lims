<template>
  <a-card>
    <a-form layout="inline" class="page-toolbar" style="margin-bottom: 16px">
      <a-form-item label="关键字">
        <a-input
          v-model:value="query.keyword"
          placeholder="编号/名称"
          allow-clear
          @press-enter="reload"
        />
      </a-form-item>
      <a-form-item label="分类">
        <a-select
          v-model:value="query.category"
          allow-clear
          style="width: 130px"
          placeholder="全部"
          @change="reload"
        >
          <a-select-option v-for="d in categoryDict" :key="d.itemValue" :value="d.itemValue">
            {{ d.itemLabel }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <a-button type="primary" @click="reload">查询</a-button>
      </a-form-item>
      <a-form-item style="float: right">
        <a-button
          v-if="userStore.hasPerm('equipment:save')"
          type="primary"
          @click="openEdit()"
        >新增设备</a-button>
      </a-form-item>
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
        <template v-if="column.key === 'category'">
          <DictTag code="equipment_category" :value="record.category" />
        </template>
        <template v-else-if="column.key === 'qtyAvailable'">
          <a-tag :color="record.qtyAvailable > 0 ? 'green' : 'red'">
            {{ record.qtyAvailable }} / {{ record.qtyTotal }} {{ record.unit }}
          </a-tag>
        </template>
        <template v-else-if="column.key === 'status'">
          <DictTag code="equipment_status" :value="record.status" />
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a v-if="userStore.hasPerm('equipment:save')" @click="openEdit(record)">编辑</a>
            <a @click="router.push('/equipment/checkout')">领用记录</a>
          </a-space>
        </template>
      </template>
    </a-table>

    <a-modal
      v-model:open="editOpen"
      :title="form.id ? '编辑设备' : '新增设备'"
      :confirm-loading="saving"
      @ok="onSave"
    >
      <a-form layout="vertical" style="margin-top: 12px">
        <a-row :gutter="12">
          <a-col :span="12">
            <a-form-item label="名称" required>
              <a-input v-model:value="form.name" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="分类">
              <a-select v-model:value="form.category">
                <a-select-option
                  v-for="d in categoryDict"
                  :key="d.itemValue"
                  :value="d.itemValue"
                >{{ d.itemLabel }}</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="规格型号">
              <a-input v-model:value="form.spec" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="单位">
              <a-input v-model:value="form.unit" placeholder="台/个" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="总数量">
              <a-input-number v-model:value="form.qtyTotal" :min="0" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="状态">
              <a-select v-model:value="form.status">
                <a-select-option value="NORMAL">正常</a-select-option>
                <a-select-option value="MAINTENANCE">维修中</a-select-option>
                <a-select-option value="SCRAPPED">报废</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="保管人">
              <a-input v-model:value="form.keeperName" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="购置日期">
              <a-date-picker
                v-model:value="form.purchaseDate"
                value-format="YYYY-MM-DD"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="备注">
              <a-input v-model:value="form.remark" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </a-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { equipmentApi } from '@/api'
import { loadDict, type DictItem } from '@/composables/useDict'
import { useUserStore } from '@/stores/user'
import DictTag from '@/components/DictTag.vue'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const saving = ref(false)
const data = ref<any[]>([])
const categoryDict = ref<DictItem[]>([])
const editOpen = ref(false)
const pagination = reactive({ current: 1, pageSize: 10, total: 0, showSizeChanger: true })
const query = reactive<any>({ keyword: '', category: '', current: 1, size: 10 })

const form = reactive<any>({
  id: undefined,
  name: '',
  category: 'DEVICE',
  spec: '',
  unit: '台',
  qtyTotal: 1,
  status: 'NORMAL',
  keeperName: '',
  purchaseDate: null,
  remark: ''
})

const columns = [
  { title: '设备编号', dataIndex: 'code', key: 'code', width: 140 },
  { title: '名称', dataIndex: 'name', key: 'name' },
  { title: '分类', key: 'category' },
  { title: '规格型号', dataIndex: 'spec', key: 'spec', width: 140 },
  { title: '可用/总量', key: 'qtyAvailable' },
  { title: '状态', key: 'status', width: 90 },
  { title: '保管人', dataIndex: 'keeperName', key: 'keeperName', width: 100 },
  { title: '操作', key: 'action', width: 150 }
]

async function reload() {
  loading.value = true
  try {
    query.current = pagination.current
    query.size = pagination.pageSize
    const res: any = await equipmentApi.page(query)
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

function openEdit(record?: any) {
  Object.assign(form, {
    id: undefined,
    name: '',
    category: 'DEVICE',
    spec: '',
    unit: '台',
    qtyTotal: 1,
    status: 'NORMAL',
    keeperName: '',
    purchaseDate: null,
    remark: '',
    ...record
  })
  editOpen.value = true
}

async function onSave() {
  if (!form.name) return message.warning('请填写名称')
  saving.value = true
  try {
    await equipmentApi.save({ ...form })
    message.success('保存成功')
    editOpen.value = false
    reload()
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  categoryDict.value = await loadDict('equipment_category')
  reload()
})
</script>
