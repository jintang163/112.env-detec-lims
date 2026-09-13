<template>
  <a-layout style="min-height: 100vh">
    <a-layout-sider v-model:collapsed="collapsed" collapsible>
      <div class="logo-bar">
        <span v-if="!collapsed">环境检测 LIMS</span>
        <span v-else>LIMS</span>
      </div>
      <a-menu
        v-model:selectedKeys="selectedKeys"
        theme="dark"
        mode="inline"
        @click="onMenuClick"
      >
        <a-menu-item key="/dashboard">
          <DashboardOutlined /><span>工作台</span>
        </a-menu-item>
        <a-sub-menu key="customer-group">
          <template #title><TeamOutlined /><span>客户管理</span></template>
          <a-menu-item key="/customer">客户列表</a-menu-item>
          <a-menu-item key="/customer/pool">公海客户</a-menu-item>
        </a-sub-menu>
        <a-menu-item key="/contract">
          <FileProtectOutlined /><span>合同管理</span>
        </a-menu-item>
        <a-menu-item key="/entrust">
          <AuditOutlined /><span>委托管理</span>
        </a-menu-item>
        <a-menu-item key="/quote">
          <CalculatorOutlined /><span>报价管理</span>
        </a-menu-item>
        <a-menu-item key="/approval">
          <CheckSquareOutlined />
          <span>审批中心</span>
          <a-badge v-if="todoCount > 0" :count="todoCount" :offset="[12, -2]" />
        </a-menu-item>
        <a-menu-item key="/notification">
          <BellOutlined /><span>消息通知</span>
        </a-menu-item>
        <a-sub-menu v-if="userStore.roles.includes('ROLE_ADMIN')" key="system-group">
          <template #title><SettingOutlined /><span>系统管理</span></template>
          <a-menu-item key="/system/operation-log">
            <FileSearchOutlined /><span>操作日志</span>
          </a-menu-item>
        </a-sub-menu>
      </a-menu>
    </a-layout-sider>

    <a-layout>
      <a-layout-header class="topbar">
        <a-input-search
          v-model:value="globalKeyword"
          placeholder="搜索委托单号/合同号/客户名称(回车快速查询)"
          style="max-width: 420px"
          allow-clear
          @search="quickSearch"
        />
        <div class="top-right">
          <a-tooltip :title="wsConnected ? '实时连接正常' : '实时连接已断开,重连中...'">
            <a-badge :status="wsConnected ? 'success' : 'error'" :text="wsConnected ? '在线' : '离线'" />
          </a-tooltip>
          <a-dropdown>
            <span class="user-name">
              <a-avatar style="background: #1677ff">{{ userStore.realName?.[0] || 'U' }}</a-avatar>
              <span style="margin-left: 8px">{{ userStore.realName }}</span>
            </span>
            <template #overlay>
              <a-menu>
                <a-menu-item key="profile" @click="router.push('/profile')">个人中心</a-menu-item>
                <a-menu-divider />
                <a-menu-item key="logout" @click="onLogout">退出登录</a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </a-layout-header>
      <a-layout-content class="content">
        <router-view />
      </a-layout-content>
      <a-layout-footer class="footer">
        LIMS 环境检测实验室信息系统 · 阶段一:委托与合同管理 · © 2026
      </a-layout-footer>
    </a-layout>
  </a-layout>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  DashboardOutlined,
  TeamOutlined,
  FileProtectOutlined,
  AuditOutlined,
  CalculatorOutlined,
  CheckSquareOutlined,
  BellOutlined,
  SettingOutlined,
  FileSearchOutlined
} from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'
import { useWebSocket } from '@/composables/useWebSocket'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const collapsed = ref(false)
const selectedKeys = ref<string[]>([route.path])
const globalKeyword = ref('')

const { connect, connected: wsConnected, todoCount, refreshTodoCount } = useWebSocket()

watch(
  () => route.path,
  (p) => {
    selectedKeys.value = [p]
  }
)

onMounted(async () => {
  if (!userStore.realName) {
    await userStore.fetchInfo()
  }
  connect()
  refreshTodoCount()
})

function onMenuClick({ key }: { key: string }) {
  router.push(key)
}

function quickSearch(kw: string) {
  if (!kw) return
  router.push({ path: '/entrust', query: { keyword: kw } })
}

async function onLogout() {
  await userStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.logo-bar {
  height: 48px;
  margin: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 700;
  letter-spacing: 1px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 6px;
}
.topbar {
  background: #fff;
  padding: 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.top-right {
  display: flex;
  align-items: center;
  gap: 20px;
}
.user-name {
  cursor: pointer;
  display: inline-flex;
  align-items: center;
}
.content {
  margin: 16px;
}
.footer {
  text-align: center;
  color: #999;
  font-size: 12px;
}
</style>
