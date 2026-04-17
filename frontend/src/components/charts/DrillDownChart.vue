<template>
  <div class="drill-down-chart">
    <div class="chart-header">
      <h4 class="chart-title">{{ title ?? '상세 분석' }}</h4>
      <div class="breadcrumb" v-if="breadcrumb.length > 0">
        <button class="breadcrumb-item" @click="goToRoot">전체</button>
        <template v-for="(crumb, idx) in breadcrumb" :key="idx">
          <i class="pi pi-chevron-right breadcrumb-sep"></i>
          <button
            class="breadcrumb-item"
            :class="{ active: idx === breadcrumb.length - 1 }"
            @click="goToLevel(idx)"
          >
            {{ crumb }}
          </button>
        </template>
      </div>
    </div>
    <v-chart
      ref="chartRef"
      :option="chartOption"
      autoresize
      :style="{ height: height }"
      @click="handleClick"
      aria-label="드릴다운 차트"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, TitleComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

use([BarChart, GridComponent, TooltipComponent, TitleComponent, CanvasRenderer])

export interface DrillDownItem {
  name: string
  value: number
  children?: DrillDownItem[]
}

const props = withDefaults(defineProps<{
  title?: string
  data: DrillDownItem[]
  height?: string
}>(), {
  height: '400px',
})

const emit = defineEmits<{
  drillDown: [path: string[]]
}>()

const breadcrumb = ref<string[]>([])
const currentData = ref<DrillDownItem[]>(props.data)

function formatValue(value: number) {
  if (Math.abs(value) >= 1e8) return `${(value / 1e8).toFixed(1)}억`
  if (Math.abs(value) >= 1e4) return `${(value / 1e4).toFixed(0)}만`
  return value.toLocaleString('ko-KR')
}

const chartOption = computed(() => ({
  tooltip: {
    trigger: 'axis',
    axisPointer: { type: 'shadow' },
    formatter: (params: Array<{ name: string; value: number }>) => {
      if (!params.length) return ''
      const p = params[0]
      const item = currentData.value.find((d) => d.name === p.name)
      const hasChildren = item?.children && item.children.length > 0
      return `<div>
        <div style="font-weight:600">${p.name}</div>
        <div>${formatValue(p.value)}</div>
        ${hasChildren ? '<div style="font-size:11px;color:#94a3b8;margin-top:4px">클릭하여 상세 보기</div>' : ''}
      </div>`
    },
  },
  grid: {
    left: '3%',
    right: '4%',
    bottom: '8%',
    top: '8%',
    containLabel: true,
  },
  xAxis: {
    type: 'category',
    data: currentData.value.map((d) => d.name),
    axisLabel: { color: '#94a3b8', fontSize: 11, rotate: currentData.value.length > 8 ? 30 : 0 },
    axisLine: { lineStyle: { color: '#e2e8f0' } },
  },
  yAxis: {
    type: 'value',
    axisLabel: {
      color: '#94a3b8',
      fontSize: 11,
      formatter: (v: number) => formatValue(v),
    },
    splitLine: { lineStyle: { color: '#f1f5f9' } },
  },
  series: [
    {
      type: 'bar',
      data: currentData.value.map((d) => ({
        value: d.value,
        itemStyle: {
          color: d.children?.length ? '#3b82f6' : '#94a3b8',
          borderRadius: [4, 4, 0, 0],
        },
      })),
      barMaxWidth: 50,
      emphasis: {
        itemStyle: { color: '#2563eb' },
      },
    },
  ],
}))

function handleClick(params: { name: string }) {
  const item = currentData.value.find((d) => d.name === params.name)
  if (item?.children && item.children.length > 0) {
    breadcrumb.value.push(item.name)
    currentData.value = item.children
    emit('drillDown', [...breadcrumb.value])
  }
}

function goToRoot() {
  breadcrumb.value = []
  currentData.value = props.data
  emit('drillDown', [])
}

function goToLevel(idx: number) {
  if (idx === breadcrumb.value.length - 1) return
  let data = props.data
  for (let i = 0; i <= idx; i++) {
    const found = data.find((d) => d.name === breadcrumb.value[i])
    if (found?.children) {
      data = found.children
    }
  }
  breadcrumb.value = breadcrumb.value.slice(0, idx + 1)
  currentData.value = data
  emit('drillDown', [...breadcrumb.value])
}
</script>

<style scoped>
.drill-down-chart {
  background: #ffffff;
  border-radius: 12px;
  padding: 1.25rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  border: 1px solid #e2e8f0;
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 0.75rem;
  flex-wrap: wrap;
  gap: 0.5rem;
}

.chart-title {
  font-size: 0.9375rem;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
}

.breadcrumb {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.breadcrumb-item {
  background: none;
  border: none;
  color: #3b82f6;
  cursor: pointer;
  font-size: 0.8125rem;
  padding: 0.125rem 0.375rem;
  border-radius: 4px;
  transition: background-color 0.15s;
}

.breadcrumb-item:hover {
  background-color: #eff6ff;
}

.breadcrumb-item.active {
  color: #0f172a;
  font-weight: 600;
  cursor: default;
}

.breadcrumb-item.active:hover {
  background: none;
}

.breadcrumb-sep {
  font-size: 0.625rem;
  color: #94a3b8;
}
</style>
