<template>
  <div id="app-layout">
    <nav v-if="showNav" class="app-nav" role="navigation" aria-label="메인 네비게이션">
      <div class="nav-brand">
        <i class="pi pi-chart-bar"></i>
        <span class="nav-title">Retail P&amp;L Dashboard</span>
      </div>
      <div class="nav-links">
        <router-link to="/" class="nav-link" active-class="nav-link-active" :exact="true">
          <i class="pi pi-home"></i>
          <span>대시보드</span>
        </router-link>
        <router-link to="/reports" class="nav-link" active-class="nav-link-active">
          <i class="pi pi-file"></i>
          <span>리포트</span>
        </router-link>
        <router-link to="/alerts" class="nav-link" active-class="nav-link-active">
          <i class="pi pi-bell"></i>
          <span>알림 설정</span>
        </router-link>
      </div>
      <div class="nav-user">
        <span class="nav-username">{{ authStore.user?.username }}</span>
        <span class="nav-role">{{ roleLabel }}</span>
        <button class="nav-logout" @click="handleLogout" aria-label="로그아웃">
          <i class="pi pi-sign-out"></i>
          <span>로그아웃</span>
        </button>
      </div>
    </nav>
    <main :class="{ 'with-nav': showNav }">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const showNav = computed(() => route.name !== 'Login' && authStore.isAuthenticated)

const roleLabel = computed(() => {
  const roleMap: Record<string, string> = {
    CEO: 'CEO',
    EXECUTIVE: '임원',
    MARKETING: '마케팅',
    FINANCE: '재무',
    PRODUCT: '상품',
  }
  return authStore.userRole ? roleMap[authStore.userRole] ?? authStore.userRole : ''
})

async function handleLogout() {
  await authStore.logout()
  router.push({ name: 'Login' })
}
</script>

<style>
html, body, #app {
  margin: 0;
  padding: 0;
  height: 100%;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}
</style>

<style scoped>
#app-layout {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.app-nav {
  display: flex;
  align-items: center;
  padding: 0 1.5rem;
  height: 56px;
  background-color: #1e293b;
  color: #f1f5f9;
  gap: 2rem;
  flex-shrink: 0;
}

.nav-brand {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-weight: 700;
  font-size: 1.1rem;
  white-space: nowrap;
}

.nav-title {
  color: #f1f5f9;
}

.nav-links {
  display: flex;
  gap: 0.25rem;
  flex: 1;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.5rem 1rem;
  border-radius: 6px;
  color: #94a3b8;
  text-decoration: none;
  font-size: 0.875rem;
  transition: background-color 0.15s, color 0.15s;
}

.nav-link:hover {
  background-color: #334155;
  color: #f1f5f9;
}

.nav-link-active {
  background-color: #3b82f6;
  color: #ffffff;
}

.nav-user {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  font-size: 0.875rem;
  white-space: nowrap;
}

.nav-username {
  font-weight: 600;
  color: #f1f5f9;
}

.nav-role {
  color: #94a3b8;
  font-size: 0.75rem;
  background-color: #334155;
  padding: 0.125rem 0.5rem;
  border-radius: 9999px;
}

.nav-logout {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  background: none;
  border: 1px solid #475569;
  color: #94a3b8;
  padding: 0.375rem 0.75rem;
  border-radius: 6px;
  cursor: pointer;
  font-size: 0.8125rem;
  transition: background-color 0.15s, color 0.15s;
}

.nav-logout:hover {
  background-color: #475569;
  color: #f1f5f9;
}

main {
  flex: 1;
  overflow: auto;
}

main.with-nav {
  background-color: #f8fafc;
}
</style>
