<template>
  <a-card title="消息通知">
    <template #extra>
      <a-space>
        <a-radio-group v-model:value="isRead" button-style="solid" size="small" @change="reload">
          <a-radio-button :value="undefined">全部</a-radio-button>
          <a-radio-button :value="0">未读</a-radio-button>
          <a-radio-button :value="1">已读</a-radio-button>
        </a-radio-group>
        <a-button size="small" @click="readAll">全部已读</a-button>
      </a-space>
    </template>
    <a-list :data-source="data" :loading="loading" item-layout="horizontal">
      <template #renderItem="{ item }">
        <a-list-item :class="{ unread: item.isRead === 0 }">
          <a-list-item-meta :title="item.title" :description="item.content">
            <template #avatar>
              <a-badge :status="item.isRead === 0 ? 'processing' : 'default'" />
            </template>
          </a-list-item-meta>
          <template #actions>
            <span class="time">{{ item.createTime }}</span>
            <a v-if="item.isRead === 0" @click="read(item)">标为已读</a>
          </template>
        </a-list-item>
      </template>
    </a-list>
    <div style="text-align: right; margin-top: 12px">
      <a-pagination
        :current="query.current"
        :page-size="query.size"
        :total="total"
        size="small"
        @change="(p: number) => { query.current = p; reload() }"
      />
    </div>
  </a-card>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { notificationApi } from '@/api'

const loading = ref(false)
const data = ref<any[]>([])
const total = ref(0)
const isRead = ref<number | undefined>(undefined)
const query = reactive({ current: 1, size: 10 })

async function reload() {
  loading.value = true
  try {
    const res: any = await notificationApi.page({ current: query.current, size: query.size, isRead: isRead.value })
    data.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}
async function read(item: any) {
  await notificationApi.read(item.id)
  reload()
}
async function readAll() {
  await notificationApi.readAll()
  reload()
}
onMounted(reload)
</script>
<style scoped>
.unread { background: #f0f7ff; border-radius: 6px; }
.time { color: #999; font-size: 12px; margin-right: 12px; }
</style>
