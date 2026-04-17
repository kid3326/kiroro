<template>
  <div class="date-range-picker">
    <label class="picker-label">기간</label>
    <Calendar
      v-model="dateRangeValue"
      selectionMode="range"
      :manualInput="true"
      dateFormat="yy-mm-dd"
      placeholder="날짜 범위 선택"
      :showIcon="true"
      :showButtonBar="true"
      :numberOfMonths="2"
      class="date-calendar"
      @date-select="onDateSelect"
      @clear-click="onClear"
      aria-label="날짜 범위 선택"
    />
    <div class="quick-ranges">
      <button
        v-for="range in quickRanges"
        :key="range.label"
        class="quick-range-btn"
        :class="{ active: activeQuickRange === range.label }"
        @click="applyQuickRange(range)"
      >
        {{ range.label }}
      </button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import Calendar from 'primevue/calendar'

const props = defineProps<{
  modelValue: { from: string; to: string } | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: { from: string; to: string } | null]
}>()

const dateRangeValue = ref<Date[] | null>(null)
const activeQuickRange = ref<string | null>(null)

interface QuickRange {
  label: string
  getDates: () => [Date, Date]
}

const quickRanges: QuickRange[] = [
  {
    label: '오늘',
    getDates: () => {
      const today = new Date()
      return [today, today]
    },
  },
  {
    label: '최근 7일',
    getDates: () => {
      const end = new Date()
      const start = new Date()
      start.setDate(start.getDate() - 6)
      return [start, end]
    },
  },
  {
    label: '최근 30일',
    getDates: () => {
      const end = new Date()
      const start = new Date()
      start.setDate(start.getDate() - 29)
      return [start, end]
    },
  },
  {
    label: '이번 달',
    getDates: () => {
      const now = new Date()
      const start = new Date(now.getFullYear(), now.getMonth(), 1)
      return [start, now]
    },
  },
  {
    label: '이번 분기',
    getDates: () => {
      const now = new Date()
      const quarter = Math.floor(now.getMonth() / 3)
      const start = new Date(now.getFullYear(), quarter * 3, 1)
      return [start, now]
    },
  },
  {
    label: '올해',
    getDates: () => {
      const now = new Date()
      const start = new Date(now.getFullYear(), 0, 1)
      return [start, now]
    },
  },
]

function formatDate(date: Date): string {
  return date.toISOString().split('T')[0]
}

function onDateSelect() {
  activeQuickRange.value = null
  if (dateRangeValue.value && dateRangeValue.value.length === 2 && dateRangeValue.value[1]) {
    emit('update:modelValue', {
      from: formatDate(dateRangeValue.value[0]),
      to: formatDate(dateRangeValue.value[1]),
    })
  }
}

function onClear() {
  activeQuickRange.value = null
  dateRangeValue.value = null
  emit('update:modelValue', null)
}

function applyQuickRange(range: QuickRange) {
  const [start, end] = range.getDates()
  dateRangeValue.value = [start, end]
  activeQuickRange.value = range.label
  emit('update:modelValue', {
    from: formatDate(start),
    to: formatDate(end),
  })
}

// Sync from parent
watch(
  () => props.modelValue,
  (val) => {
    if (val) {
      dateRangeValue.value = [new Date(val.from), new Date(val.to)]
    } else {
      dateRangeValue.value = null
    }
  },
  { immediate: true },
)
</script>

<style scoped>
.date-range-picker {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.picker-label {
  font-size: 0.8125rem;
  font-weight: 600;
  color: #64748b;
  white-space: nowrap;
}

.date-calendar {
  width: 260px;
}

.quick-ranges {
  display: flex;
  gap: 0.25rem;
}

.quick-range-btn {
  padding: 0.25rem 0.5rem;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  border-radius: 4px;
  font-size: 0.75rem;
  color: #64748b;
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
}

.quick-range-btn:hover {
  border-color: #3b82f6;
  color: #3b82f6;
}

.quick-range-btn.active {
  background: #3b82f6;
  color: #ffffff;
  border-color: #3b82f6;
}
</style>
