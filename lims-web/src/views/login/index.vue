<template>
  <div class="login-wrap">
    <div class="login-card">
      <div class="brand">
        <div class="logo">LIMS</div>
        <h2>环境检测实验室信息系统</h2>
        <p>委托与合同管理平台</p>
      </div>
      <a-form layout="vertical" :model="form" @finish="onLogin">
        <a-form-item label="用户名" name="username" :rules="[{ required: true, message: '请输入用户名' }]">
          <a-input v-model:value="form.username" size="large" placeholder="请输入用户名">
            <template #prefix><UserOutlined /></template>
          </a-input>
        </a-form-item>
        <a-form-item label="密码" name="password" :rules="[{ required: true, message: '请输入密码' }]">
          <a-input-password v-model:value="form.password" size="large" placeholder="请输入密码">
            <template #prefix><LockOutlined /></template>
          </a-input-password>
        </a-form-item>
        <a-button type="primary" size="large" block html-type="submit" :loading="loading">
          登 录
        </a-button>
      </a-form>
      <div class="tips">演示账号: admin / manager / sales / reviewer / finance / sampler，密码均为 admin123</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { UserOutlined, LockOutlined } from '@ant-design/icons-vue'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const loading = ref(false)
const form = reactive({ username: 'admin', password: 'admin123' })

async function onLogin() {
  loading.value = true
  try {
    await userStore.login(form.username, form.password)
    message.success(`欢迎回来，${userStore.realName}`)
    router.push((route.query.redirect as string) || '/dashboard')
  } catch (e) {
    // 错误已由拦截器提示
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrap {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0b5ed7 0%, #0a3d62 100%);
}
.login-card {
  width: 400px;
  background: #fff;
  border-radius: 12px;
  padding: 40px 36px 28px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.25);
}
.brand { text-align: center; margin-bottom: 28px; }
.logo {
  display: inline-block;
  background: #1677ff;
  color: #fff;
  font-weight: 700;
  font-size: 20px;
  padding: 6px 14px;
  border-radius: 8px;
  margin-bottom: 12px;
}
.brand h2 { margin: 0 0 4px; font-size: 20px; }
.brand p { margin: 0; color: #888; font-size: 13px; }
.tips { margin-top: 14px; color: #999; font-size: 12px; text-align: center; }
</style>
