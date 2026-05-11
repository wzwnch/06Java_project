import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import { isLoggedIn } from '@/utils/auth'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { requiresAuth: false, title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/register/index.vue'),
    meta: { requiresAuth: false, title: '注册' }
  },
  {
    path: '/',
    name: 'Layout',
    component: () => import('@/components/Layout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { requiresAuth: true, title: '仪表盘' }
      },
      {
        path: 'link',
        name: 'Link',
        component: () => import('@/views/link/index.vue'),
        meta: { requiresAuth: true, title: '短链接管理' }
      },
      {
        path: 'group',
        name: 'Group',
        component: () => import('@/views/group/index.vue'),
        meta: { requiresAuth: true, title: '分组管理' }
      },
      {
        path: 'recycle',
        name: 'Recycle',
        component: () => import('@/views/recycle/index.vue'),
        meta: { requiresAuth: true, title: '回收站' }
      },
      {
        path: 'stats',
        name: 'Stats',
        component: () => import('@/views/stats/index.vue'),
        meta: { requiresAuth: true, title: '监控统计' }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue'),
    meta: { requiresAuth: false, title: '页面不存在' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, _from, next) => {
  document.title = to.meta.title ? `${to.meta.title} - 短链接系统` : '短链接系统'
  const requiresAuth = to.meta.requiresAuth !== false
  if (requiresAuth && !isLoggedIn()) {
    next({ name: 'Login', query: { redirect: to.fullPath } })
  } else {
    next()
  }
})

export default router
