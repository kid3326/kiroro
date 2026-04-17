<template>
  <div class="mobile-dashboard" @touchstart="onTouchStart" @touchend="onTouchEnd">
    <OfflineBanner :last-updated="dashboardStore.lastUpdated" />

    <!-- Mobile header -->
    <header class="mobile-header">
      <h1 class="mobile-title">P&amp;L 대시보드</h1>
      <button class="btn-refresh" @click="refresh" :disabled="dashboardStore.isLoading">
        <i :class="dashboardStore.isLoading ? 'pi pi-spin pi-spinner' : 'pi pi-refresh'"></i>
      </button>
    </header>

    <!-- Quick date filter -->
    <div class="mobile-date-filter">
      <button
        v-for="range in quickRanges"
        :key="range.label"
        class="date-btn"
        :class="{ active: activeRange === range.label }"
        @click="applyRange(range)"
      >
        {{ range.label }}
      </button>
    </div>

    <!-- KPI Cards (swipeable) -->
    <section class="mobile-kpi-section" aria-label="주요 KPI">
      <template v-if="dashboardStore.isLoading && dashboardStore.kpiMetrics.length === 0">
        <div class="kpi-scroll">
          <div v-for="n in 4" :key="'skel-' + n" class="mobile-kpi-card mobile-kpi-skeleton">
            <div class="skeleton-line" style="width: 60%; height: 0.75rem;"></div>
            <div class="skeleton-line" style="width: 80%; height: 1.5rem;"></div>
            <div class="skeleton-line" style="width: 40%; height: 0.75rem;"></div>
          </div>
        </div>
      </template>
      <div v-else class="kpi-scroll">
        <div
          v-for="kpi in dashboardStore.kpiMetrics"
          :key="kpi.label"
          class="mobile-kpi-card"
        >
          <span class="mobile-kpi-title">{{ kpi.label }}</span>
          <span class="mobile-kpi-value">{{ formatValue(kpi.value, kpi.format) }}</span>
          <span
            v-if="kpi.changePercent !== undefined"
            class="mobile-kpi-change"
            :class="kpi.changePercent >= 0 ? 'positive' : 'negative'"
          >
            <i :class="kpi.changePercent >= 0 ? 'pi pi-arrow-up' : 'pi pi-arrow-down'"></i>
            {{ kpi.changePercent > 0 ? '+' : '' }}{{ kpi.changePercent.toFixed(1) }}%
          </span>
        </div>
      </div>
    </section>

    <!-- Alert summary -->
    <section v-if="alertStore.activeAlerts.length > 0" class="mobile-alerts" aria-label="알림">
      <h2 class="mobile-section-title">
        알림 <span class="alert-badge">{{ alertStore.activeAlerts.length }}</span>
      </h2>
      <div class="mobile-alert-list">
        <div
          v-for="alert in alertStore.activeAlerts.slice(0, 5)"
          :key="alert.id"
          class="mobile-alert-item"
          :class="`severity-${alert.severity.toLowerCase()}`"
        >
          <i :class="severityIcon(alert.severity)" class="alert-icon"></i>
          <div class="alert-content">
            <span class="alert-title">{{ alert.title }}</span>
            <span class="alert-message">{{ alert.message }}</span>
          </div>
        </div>
      </div>
    </section>

    <!-- Offline read-only notice -->
    <div v-if="isOffline" class="offline-notice">
      <i class="pi pi-lock"></i>
      <span>오프라인 모드에서는 읽기 전용으로 동작합니다.</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useDashboardStore } from '@/stores/dashboardStore'
import { useAlertStore } from '@/stores/alertStore'
import { useFilterStore } from '@/stores/filterStore'
import OfflineBanner from '@/components/common/OfflineBanner.vue'

const dashboardStore = useDashboardStore()
const alertStore = useAlertStore()
const filterStore = useFilterStore()

const isOffline = ref(!navigator.onLine)
const activeRange = ref('이번 달')

// Touch gesture tracking
const touchStartX = ref(0)
const touchStartY = ref(0)

interface QuickRange {
  label: string
  getDates: () => { from: string; to: string }
}

const quickRanges: QuickRange[] = [
  {
    label: '오늘',
    getDates: () => {
      const today = new Date().toISOString().split('T')[0]
      return { from: today, to: today }
    },
  },
  {
    label: '7일',
    getDates: () => {
      const end = new Date()
      const start = new Date()
      start.setDate(start.getDate() - 6)
      return { from: start.toISOString().split('T')[0], to: end.toISOString().split('T')[0] }
    },
  },
  {
    label: '이번 달',
    getDates: () => {
      const now = new Date()
      const start = new Date(now.getFullYear(), now.getMonth(), 1)
      return { from: start.toISOString().split('T')[0], to: now.toISOString().split('T')[0] }
    },
  },
  {
    label: '분기',
    getDates: () => {
      const now = new Date()
      const quarter = Math.floor(now.getMonth() / 3)
      const start = new Date(now.getFullYear(), quarter * 3, 1)
      return { from: start.toISOString().split('T')[0], to: now.toISOString().split('T')[0] }
    },
  },
]

function formatValue(value: number, format: string) {
  switch (format) {
    case 'currency':
      if (Math.abs(value) >= 1e8) return `${(value / 1e8).toFixed(1)}억원`
      if (Math.abs(value) >= 1e4) return `${(value / 1e4).toFixed(0)}만원`
      return new Intl.NumberFormat('ko-KR', { style: 'currency', currency: 'KRW', maximumFractionDigits: 0 }).format(value)
    case 'percent':
      return `${value.toFixed(1)}%`
    default:
      return new Intl.NumberFormat('ko-KR').format(value)
  }
}

function severityIcon(severity: string) {
  switch (severity) {
    case 'CRITICAL': return 'pi pi-exclamation-triangle'
    case 'IMPORTANT': return 'pi pi-exclamation-circle'
    default: return 'pi pi-info-circle'
  }
}

function applyRange(range: QuickRange) {
  activeRange.value = range.label
  filterStore.dateRange = range.getDates()
  filterStore.syncToUrl()
  dashboardStore.fetchAll()
}

function refresh() {
  dashboardStore.fetchAll()
  alertStore.fetchAlerts()
}

// Touch gesture handlers for swipe navigation
function onTouchStart(e: TouchEvent) {
  touchStartX.value = e.touches[0].clientX
  touchStartY.value = e.touches[0].clientY
}

function onTouchEnd(e: TouchEvent) {
  const deltaX = e.changedTouches[0].clientX - touchStartX.value
  const deltaY = e.changedTouches[0].clientY - touchStartY.value

  // Only handle horizontal swipes (ignore vertical scrolling)
  if (Math.abs(deltaX) > 50 && Math.abs(deltaX) > Math.abs(deltaY) * 2) {
    const currentIdx = quickRanges.findIndex((r) => r.label === activeRange.value)
    if (deltaX < 0 && currentIdx < quickRanges.length - 1) {
      // Swipe left - next range
      applyRange(quickRanges[currentIdx + 1])
    } else if (deltaX > 0 && currentIdx > 0) {
      // Swipe right - previous range
      applyRange(quickRanges[currentIdx - 1])
    }
  }
}

function handleOnline() { isOffline.value = false }
function handleOffline() { isOffline.value = true }

onMounted(async () => {
  window.addEventListener('online', handleOnline)
  window.addEventListener('offline', handleOffline)

  filterStore.syncFromUrl()
  await Promise.all([
    dashboardStore.fetchAll(),
    alertStore.fetchAlerts(),
  ])
})

onUnmounted(() => {
  window.removeEventListener('online', handleOnline)
  window.removeEventListener('offline', handleOffline)
})
</script>

<style scoped>
.mobile-dashboard {
  padding: 0;
  background: #f8fafc;
  min-height: 100vh;
  -webkit-overflow-scrolling: touch;
}

.mobile-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 1rem 0.5rem;
  background: #ffffff;
}

.mobile-title {
  font-size: 1.25rem;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
}

.btn-refresh {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f1f5f9;
  border: none;
  border-radius: 8px;
  color: #64748b;
  cursor: pointer;
  font-size: 1rem;
}

.btn-refresh:disabled {
  opacity: 0.5;
}

.mobile-date-filter {
  display: flex;
  gap: 0.375rem;
  padding: 0.5rem 1rem 0.75rem;
  background: #ffffff;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
}

.date-btn {
  padding: 0.375rem 0.75rem;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  border-radius: 9999px;
  font-size: 0.8125rem;
  color: #64748b;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.15s;
  flex-shrink: 0;
}

.date-btn.active {
  background: #3b82f6;
  color: #ffffff;
  border-color: #3b82f6;
}

.mobile-kpi-section {
  padding: 0.75rem 0;
}

.kpi-scroll {
  display: flex;
  gap: 0.75rem;
  padding: 0 1rem;
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  scroll-snap-type: x mandatory;
}

.kpi-scroll::-webkit-scrollbar {
  display: none;
}

.mobile-kpi-card {
  flex: 0 0 160px;
  background: #ffffff;
  border-radius: 12px;
  padding: 1rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  border: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
  scroll-snap-align: start;
}

.mobile-kpi-title {
  font-size: 0.6875rem;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.025em;
}

.mobile-kpi-value {
  font-size: 1.25rem;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.2;
}

.mobile-kpi-change {
  display: flex;
  align-items: center;
  gap: 0.125rem;
  font-size: 0.75rem;
  font-weight: 500;
}

.mobile-kpi-change.positive { color: #059669; }
.mobile-kpi-change.negative { color: #dc2626; }

.mobile-kpi-skeleton {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.skeleton-line {
  background: linear-gradient(90deg, #e2e8f0 25%, #f1f5f9 50%, #e2e8f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: 4px;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

.mobile-alerts {
  padding: 0.75rem 1rem;
}

.mobile-section-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 1rem;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 0.75rem;
}

.alert-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 1.25rem;
  height: 1.25rem;
  padding: 0 0.25rem;
  background: #ef4444;
  color: #ffffff;
  border-radius: 9999px;
  font-size: 0.6875rem;
  font-weight: 700;
}

.mobile-alert-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.mobile-alert-item {
  display: flex;
  align-items: flex-start;
  gap: 0.625rem;
  padding: 0.75rem;
  border-radius: 10px;
  background: #ffffff;
  border-left: 3px solid transparent;
}

.mobile-alert-item.severity-critical {
  border-left-color: #ef4444;
  background: #fef2f2;
}

.mobile-alert-item.severity-important {
  border-left-color: #f59e0b;
  background: #fffbeb;
}

.mobile-alert-item.severity-informational {
  border-left-color: #3b82f6;
  background: #eff6ff;
}

.alert-icon {
  flex-shrink: 0;
  margin-top: 0.125rem;
  font-size: 0.875rem;
}

.severity-critical .alert-icon { color: #ef4444; }
.severity-important .alert-icon { color: #f59e0b; }
.severity-informational .alert-icon { color: #3b82f6; }

.alert-content {
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
  min-width: 0;
}

.alert-title {
  font-size: 0.8125rem;
  font-weight: 600;
  color: #0f172a;
}

.alert-message {
  font-size: 0.75rem;
  color: #64748b;
  line-height: 1.4;
}

.offline-notice {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
  padding: 0.75rem;
  margin: 0.75rem 1rem;
  background: #fef3c7;
  border-radius: 8px;
  font-size: 0.8125rem;
  color: #92400e;
}

/* Pinch-to-zoom support */
.mobile-dashboard {
  touch-action: pan-x pan-y;
}
</style>
