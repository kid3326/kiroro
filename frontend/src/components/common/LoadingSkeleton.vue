<template>
  <div class="loading-skeleton-container" role="status" :aria-label="ariaLabel">
    <span class="sr-only">{{ ariaLabel }}</span>
    <template v-if="variant === 'card'">
      <div class="skeleton-card" v-for="n in count" :key="n">
        <Skeleton width="100%" height="1.5rem" class="skeleton-item" />
        <Skeleton width="60%" height="2.5rem" class="skeleton-item" />
        <Skeleton width="40%" height="1rem" class="skeleton-item" />
      </div>
    </template>
    <template v-else-if="variant === 'table'">
      <div class="skeleton-table">
        <div class="skeleton-table-header">
          <Skeleton width="100%" height="2.5rem" />
        </div>
        <div class="skeleton-table-row" v-for="n in rows" :key="n">
          <Skeleton width="100%" height="2rem" />
        </div>
      </div>
    </template>
    <template v-else-if="variant === 'chart'">
      <div class="skeleton-chart">
        <Skeleton width="40%" height="1.25rem" class="skeleton-item" />
        <Skeleton width="100%" :height="height ?? '200px'" borderRadius="8px" />
      </div>
    </template>
    <template v-else>
      <Skeleton
        :width="width ?? '100%'"
        :height="height ?? '1rem'"
        :borderRadius="borderRadius ?? '4px'"
      />
    </template>
  </div>
</template>

<script setup lang="ts">
import Skeleton from 'primevue/skeleton'

withDefaults(defineProps<{
  variant?: 'default' | 'card' | 'table' | 'chart'
  width?: string
  height?: string
  borderRadius?: string
  count?: number
  rows?: number
  ariaLabel?: string
}>(), {
  variant: 'default',
  count: 3,
  rows: 5,
  ariaLabel: '로딩 중...',
})
</script>

<style scoped>
.loading-skeleton-container {
  width: 100%;
}

.skeleton-card {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  padding: 1.25rem;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #ffffff;
  margin-bottom: 1rem;
}

.skeleton-item {
  display: block;
}

.skeleton-table {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  padding: 1rem;
  background: #ffffff;
}

.skeleton-table-header {
  margin-bottom: 0.25rem;
}

.skeleton-table-row {
  padding: 0.25rem 0;
}

.skeleton-chart {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
  padding: 1.25rem;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  background: #ffffff;
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border-width: 0;
}
</style>
