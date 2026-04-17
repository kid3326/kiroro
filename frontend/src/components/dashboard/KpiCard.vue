<template>
  <div class="kpi-card" :class="{ 'kpi-card--loading': loading }">
    <div v-if="loading" class="kpi-skeleton">
      <div class="skeleton-line skeleton-title"></div>
      <div class="skeleton-line skeleton-value"></div>
      <div class="skeleton-line skeleton-change"></div>
    </div>
    <template v-else>
      <div class="kpi-header">
        <span class="kpi-title">{{ title }}</span>
        <i v-if="icon" :class="icon" class="kpi-icon"></i>
      </div>
      <div class="kpi-value">{{ formattedValue }}</div>
      <div v-if="changePercent !== undefined" class="kpi-change" :class="changeClass">
        <i :class="changePercent >= 0 ? 'pi pi-arrow-up' : 'pi pi-arrow-down'"></i>
        {{ changePercent > 0 ? '+' : '' }}{{ changePercent.toFixed(1) }}%
        <span class="kpi-change-label">전기 대비</span>
      </div>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  title: string
  value: number
  format?: 'currency' | 'number' | 'percent'
  changePercent?: number
  icon?: string
  loading?: boolean
}>(), {
  format: 'currency',
  loading: false,
})

const formattedValue = computed(() => {
  switch (props.format) {
    case 'currency':
      return new Intl.NumberFormat('ko-KR', { style: 'currency', currency: 'KRW', maximumFractionDigits: 0 }).format(props.value)
    case 'percent':
      return `${props.value.toFixed(1)}%`
    default:
      return new Intl.NumberFormat('ko-KR').format(props.value)
  }
})

const changeClass = computed(() => {
  if (props.changePercent === undefined) return ''
  return props.changePercent >= 0 ? 'positive' : 'negative'
})
</script>

<style scoped>
.kpi-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 1.25rem 1.5rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08), 0 1px 2px rgba(0, 0, 0, 0.06);
  transition: box-shadow 0.2s, transform 0.2s;
  border: 1px solid #e2e8f0;
}

.kpi-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transform: translateY(-1px);
}

.kpi-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
}

.kpi-title {
  font-size: 0.8125rem;
  font-weight: 600;
  color: #64748b;
  text-transform: uppercase;
  letter-spacing: 0.025em;
}

.kpi-icon {
  font-size: 1rem;
  color: #94a3b8;
}

.kpi-value {
  font-size: 1.625rem;
  font-weight: 700;
  color: #0f172a;
  margin-bottom: 0.5rem;
  line-height: 1.2;
}

.kpi-change {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.8125rem;
  font-weight: 500;
}

.kpi-change.positive {
  color: #059669;
}

.kpi-change.negative {
  color: #dc2626;
}

.kpi-change-label {
  color: #94a3b8;
  font-weight: 400;
  margin-left: 0.25rem;
}

/* Skeleton loading */
.kpi-skeleton {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.skeleton-line {
  background: linear-gradient(90deg, #e2e8f0 25%, #f1f5f9 50%, #e2e8f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.5s infinite;
  border-radius: 4px;
}

.skeleton-title { width: 60%; height: 0.875rem; }
.skeleton-value { width: 80%; height: 1.75rem; }
.skeleton-change { width: 40%; height: 0.875rem; }

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}
</style>
