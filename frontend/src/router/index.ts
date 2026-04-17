import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginView.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/',
    name: 'Dashboard',
    component: () => import('@/views/DashboardView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/mobile',
    name: 'MobileDashboard',
    component: () => import('@/views/MobileDashboardView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/reports',
    name: 'Reports',
    component: () => import('@/views/ReportView.vue'),
    meta: { requiresAuth: true },
  },
  {
    path: '/alerts',
    name: 'AlertConfig',
    component: () => import('@/views/AlertConfigView.vue'),
    meta: { requiresAuth: true },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

let sessionChecked = false

router.beforeEach(async (to) => {
  const authStore = useAuthStore()

  // 최초 로드 시 세션 확인
  if (!sessionChecked) {
    sessionChecked = true
    await authStore.checkSession()
  }

  const requiresAuth = to.meta.requiresAuth !== false

  if (requiresAuth && !authStore.isAuthenticated) {
    return { name: 'Login', query: { redirect: to.fullPath } }
  }

  // 이미 인증된 사용자가 로그인 페이지 접근 시 대시보드로 리다이렉트
  if (to.name === 'Login' && authStore.isAuthenticated) {
    return { name: 'Dashboard' }
  }

  // 모바일 기기에서 대시보드 접근 시 모바일 뷰로 리다이렉트
  if (to.name === 'Dashboard' && isMobileDevice() && to.query.desktop !== 'true') {
    return { name: 'MobileDashboard' }
  }
})

function isMobileDevice(): boolean {
  return window.innerWidth < 768
}

export default router
