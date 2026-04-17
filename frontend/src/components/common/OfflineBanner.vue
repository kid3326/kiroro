<template>
  <transition name="banner-slide">
    <div v-if="isOffline" class="offline-banner" role="alert" aria-live="assertive">
      <div class="offline-banner-content">
        <i class="pi pi-wifi-off offline-icon"></i>
        <div class="offline-text">
          <span class="offline-title">오프라인 모드</span>
          <span class="offline-detail">
            인터넷 연결이 끊어졌습니다. 캐시된 데이터를 표시합니다.
          </span>
        </div>
        <div class="offline-timestamp" v-if="formattedTimestamp">
          <i class="pi pi-clock"></i>
          <span>마지막 업데이트: {{ formattedTimestamp }}</span>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'

const props = defineProps<{
  lastUpdated?: number | string | null
}>()

const isOffline = ref(!navigator.onLine)

function handleOnline() {
  isOffline.value = false
}

function handleOffline() {
  isOffline.value = true
}

onMounted(() => {
  window.addEventListener('online', handleOnline)
  window.addEventListener('offline', handleOffline)
})

onUnmounted(() => {
  window.removeEventListener('online', handleOnline)
  window.removeEventListener('offline', handleOffline)
})

const formattedTimestamp = computed(() => {
  if (!props.lastUpdated) return null
  try {
    const date = typeof props.lastUpdated === 'string'
      ? new Date(props.lastUpdated)
      : new Date(props.lastUpdated)
    return date.toLocaleString('ko-KR', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    })
  } catch {
    return '알 수 없음'
  }
})
</script>

<style scoped>
.offline-banner {
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
  border-bottom: 2px solid #f59e0b;
  padding: 0.625rem 1.5rem;
}

.offline-banner-content {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  max-width: 1200px;
  margin: 0 auto;
}

.offline-icon {
  font-size: 1.25rem;
  color: #92400e;
  flex-shrink: 0;
}

.offline-text {
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
  flex: 1;
}

.offline-title {
  font-weight: 700;
  font-size: 0.875rem;
  color: #78350f;
}

.offline-detail {
  font-size: 0.8125rem;
  color: #92400e;
}

.offline-timestamp {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  font-size: 0.8125rem;
  color: #92400e;
  background: rgba(255, 255, 255, 0.5);
  padding: 0.25rem 0.75rem;
  border-radius: 9999px;
  white-space: nowrap;
  flex-shrink: 0;
}

.banner-slide-enter-active,
.banner-slide-leave-active {
  transition: all 0.3s ease;
}

.banner-slide-enter-from,
.banner-slide-leave-to {
  transform: translateY(-100%);
  opacity: 0;
}
</style>
