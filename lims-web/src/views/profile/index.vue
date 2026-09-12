<template>
  <a-card title="个人中心">
    <a-descriptions bordered :column="1" style="max-width: 520px">
      <a-descriptions-item label="姓名">{{ user.realName }}</a-descriptions-item>
      <a-descriptions-item label="登录名">{{ user.username }}</a-descriptions-item>
      <a-descriptions-item label="角色">
        <a-tag v-for="r in user.roles" :key="r" color="blue">{{ r }}</a-tag>
      </a-descriptions-item>
      <a-descriptions-item label="功能权限">
        <a-space wrap>
          <a-tag v-for="p in user.permissions" :key="p">{{ p }}</a-tag>
        </a-space>
      </a-descriptions-item>
    </a-descriptions>
  </a-card>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useUserStore } from '@/stores/user'

const user = useUserStore()
onMounted(() => {
  if (!user.realName) user.fetchInfo()
})
</script>
