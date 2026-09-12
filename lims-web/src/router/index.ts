import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', component: () => import('@/views/login/index.vue'), meta: { public: true } },
    {
      path: '/',
      component: () => import('@/layouts/MainLayout.vue'),
      redirect: '/dashboard',
      children: [
        { path: 'dashboard', name: '工作台', component: () => import('@/views/dashboard/index.vue') },
        { path: 'customer', name: '客户管理', component: () => import('@/views/customer/index.vue') },
        { path: 'customer/pool', name: '公海客户', component: () => import('@/views/customer/index.vue') },
        {
          path: 'contract',
          name: '合同管理',
          component: () => import('@/views/contract/index.vue')
        },
        {
          path: 'contract/detail/:id',
          name: '合同详情',
          component: () => import('@/views/contract/detail.vue')
        },
        { path: 'entrust', name: '委托管理', component: () => import('@/views/entrust/index.vue') },
        {
          path: 'entrust/edit/:id?',
          name: '委托单编辑',
          component: () => import('@/views/entrust/edit.vue')
        },
        {
          path: 'entrust/detail/:id',
          name: '委托单详情',
          component: () => import('@/views/entrust/detail.vue')
        },
        { path: 'quote', name: '报价管理', component: () => import('@/views/quote/index.vue') },
        { path: 'quote/edit/:id?', name: '报价编制', component: () => import('@/views/quote/edit.vue') },
        {
          path: 'quote/detail/:id',
          name: '报价详情',
          component: () => import('@/views/quote/detail.vue')
        },
        { path: 'approval', name: '审批中心', component: () => import('@/views/approval/index.vue') },
        { path: 'notification', name: '消息通知', component: () => import('@/views/notification/index.vue') },
        { path: 'profile', name: '个人中心', component: () => import('@/views/profile/index.vue') }
      ]
    },
    { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
  ]
})

router.beforeEach((to) => {
  const token = localStorage.getItem('lims_token')
  if (!to.meta.public && !token) {
    return { path: '/login', query: { redirect: to.fullPath } }
  }
  if (to.path === '/login' && token) {
    return { path: '/dashboard' }
  }
})

export default router
