<template>
  <transition name="alert-fade">
    <div
      v-if="visible"
      class="alert-banner"
      :class="severityClass"
      role="alert"
      :aria-live="severity === 'CRITICAL' ? 'assertive' : 'polite'"
    >
      <div class="alert-icon-wrapper" :class="severityIconBg">
        <i :class="iconClass"></i>
      </div>
      <div class="alert-body">
        <span v-if="title" class="alert-title">{{ title }}</span>
        <span class="alert-message">{{ message }}</span>
        <span v-if="timestamp" class="alert-time">
          <i class="pi pi-clock"></i>
          {{ formattedTimestamp }}
        </span>
      </div>
      <div class="alert-actions">
        <button
          v-if="acknowledgeLabel"
          class="alert-acknowledge"
          :class="severityBtnClass"
          @click="$emit('acknowledge')"
        >
          {{ acknowledgeLabel }}
        </button>
        <button
          class="alert-close"
          @click="$emit('close')"
          :aria-label="'알림 닫기: ' + message"
        >
          <i class="pi pi-times"></i>
        </button>
      </div>
    </div>
  </transition>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  message: string
  severity: 'CRITICAL' | 'IMPORTANT' | 'INFORMATIONAL'
  visible: boolean
  title?: string
  timestamp?: string | number | null
  acknowledgeLabel?: string
}>(), {
  acknowledgeLabel: '',
})

defineEmits<{
  close: []
  acknowledge: []
}>()

const severityClass = computed(() => `severity-${props.severity.toLowerCase()}`)

const severityIconBg = computed(() => `icon-bg-${props.severity.toLowerCase()}`)

const severityBtnClass = computed(() => `btn-${props.severity.toLowerCase()}`)

const iconClass = computed(() => {
  switch (props.severity) {
    case 'CRITICAL': return 'pi pi-exclamation-triangle'
    case 'IMPORTANT': return 'pi pi-exclamation-circle'
    default: return 'pi pi-info-circle'
  }
})

const formattedTimestamp = computed(() => {
  if (!props.timestamp) return ''
  try {
    const date = typeof props.timestamp === 'string'
      ? new Date(props.timestamp)
      : new Date(props.timestamp)
    return date.toLocaleString('ko-KR', {
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit',
    })
  } catch {
    return ''
  }
})
</script>

<style scoped>
.alert-banner {
  display: flex;
  align-items: flex-start;
  padding: 0.875rem 1rem;
  border-radius: 8px;
  margin-bottom: 0.5rem;
  gap: 0.75rem;
  border-left: 4px solid transparent;
}

/* CRITICAL - 빨간색 */
.severity-critical {
  background-color: #fef2f2;
  color: #991b1b;
  border-left-color: #ef4444;
  border: 1px solid #fecaca;
  border-left: 4px solid #ef4444;
}

/* IMPORTANT - 주황/노란색 */
.severity-important {
  background-color: #fffbeb;
  color: #92400e;
  border-left-color: #f59e0b;
  border: 1px solid #fde68a;
  border-left: 4px solid #f59e0b;
}

/* INFORMATIONAL - 파란색 */
.severity-informational {
  background-color: #eff6ff;
  color: #1e40af;
  border-left-color: #3b82f6;
  border: 1px solid #bfdbfe;
  border-left: 4px solid #3b82f6;
}

.alert-icon-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 2rem;
  height: 2rem;
  border-radius: 50%;
  flex-shrink: 0;
  font-size: 1rem;
}

.icon-bg-critical {
  background-color: #fee2e2;
  color: #dc2626;
}

.icon-bg-important {
  background-color: #fef3c7;
  color: #d97706;
}

.icon-bg-informational {
  background-color: #dbeafe;
  color: #2563eb;
}

.alert-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  min-width: 0;
}

.alert-title {
  font-weight: 700;
  font-size: 0.875rem;
}

.alert-message {
  font-size: 0.875rem;
  line-height: 1.4;
}

.alert-time {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.75rem;
  opacity: 0.7;
  margin-top: 0.125rem;
}

.alert-actions {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-shrink: 0;
}

.alert-acknowledge {
  font-size: 0.8125rem;
  padding: 0.375rem 0.75rem;
  border-radius: 6px;
  border: none;
  cursor: pointer;
  font-weight: 600;
  transition: opacity 0.15s;
}

.alert-acknowledge:hover {
  opacity: 0.85;
}

.btn-critical {
  background-color: #dc2626;
  color: #ffffff;
}

.btn-important {
  background-color: #d97706;
  color: #ffffff;
}

.btn-informational {
  background-color: #2563eb;
  color: #ffffff;
}

.alert-close {
  background: none;
  border: none;
  cursor: pointer;
  padding: 0.375rem;
  color: inherit;
  opacity: 0.6;
  border-radius: 4px;
  transition: opacity 0.15s, background-color 0.15s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.alert-close:hover {
  opacity: 1;
  background-color: rgba(0, 0, 0, 0.05);
}

.alert-fade-enter-active,
.alert-fade-leave-active {
  transition: all 0.3s ease;
}

.alert-fade-enter-from,
.alert-fade-leave-to {
  opacity: 0;
  transform: translateX(20px);
}
</style>
