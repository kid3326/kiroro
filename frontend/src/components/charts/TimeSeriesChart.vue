<template>
  <div class="time-series-chart">
    <div class="chart-header" v-if="title">
      <h4 class="chart-title">{{ title }}</h4>
      <div class="granularity-selector" v-if="showGranularity">
        <button
          v-for="g in granularities"
          :key="g.value"
          class="gran-btn"
          :class="{ active: modelValue === g.value }"
          @click="$emit('update:modelValue', g.value)"
        >
          {{ g.label }}
        </button>
      </div>
    </div>
    <v-chart
      :option="chartOption"
      autoresize
      :style="{ height: height }"
      aria-label="시계열 차트"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, DataZoomComponent, TitleComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

use([LineChart, GridComponent, TooltipComponent, LegendComponent, DataZoomComponent, TitleComponent, CanvasRenderer])

export interface SeriesData {
  name: string
  data: { date: string; value: number }[]
  color?: string
  type?: 'solid' | 'dashed'
}

const props = withDefaults(defineProps<{
  title?: string
  series: SeriesData[]
  height?: string
  showGranularity?: boolean
  modelValue?: string
  yAxisFormat?: 'currency' | 'number' | 'percent'
}>(), {
  height: '400px',
  showGranularity: false,
  modelValue: 'monthly',
  yAxisFormat: 'currency',
})

defineEmits<{
  'update:modelValue': [value: string]
}>()

const granularities = [
  { label: '일', value: 'daily' },
  { label: '주', value: 'weekly' },
  { label: '월', value: 'monthly' },
  { label: '분기', value: 'quarterly' },
  { label: '연', value: 'yearly' },
]

function formatYValue(value: number) {
  if (props.yAxisFormat === 'currency') {
    if (Math.abs(value) >= 1e8) return `${(value / 1e8).toFixed(1)}억`
    if (Math.abs(value) >= 1e4) return `${(value / 1e4).toFixed(0)}만`
    return value.toLocaleString('ko-KR')
  }
  if (props.yAxisFormat === 'percent') return `${value.toFixed(1)}%`
  return value.toLocaleString('ko-KR')
}

const chartOption = computed(() => ({
  tooltip: {
    trigger: 'axis',
    formatter: (params: Array<{ seriesName: string; value: [string, number]; color: string }>) => {
      if (!params.length) return ''
      let html = `<div style="font-weight:600;margin-bottom:4px">${params[0].value[0]}</div>`
      params.forEach((p) => {
        html += `<div style="display:flex;align-items:center;gap:6px">
          <span style="display:inline-block;width:10px;height:10px;border-radius:50%;background:${p.color}"></span>
          <span>${p.seriesName}: ${formatYValue(p.value[1])}</span>
        </div>`
      })
      return html
    },
  },
  legend: {
    bottom: 0,
    textStyle: { fontSize: 12, color: '#64748b' },
  },
  grid: {
    left: '3%',
    right: '4%',
    bottom: '15%',
    top: '10%',
    containLabel: true,
  },
  xAxis: {
    type: 'time',
    axisLabel: { color: '#94a3b8', fontSize: 11 },
    axisLine: { lineStyle: { color: '#e2e8f0' } },
  },
  yAxis: {
    type: 'value',
    axisLabel: {
      color: '#94a3b8',
      fontSize: 11,
      formatter: (v: number) => formatYValue(v),
    },
    splitLine: { lineStyle: { color: '#f1f5f9' } },
  },
  dataZoom: [
    { type: 'inside', start: 0, end: 100 },
    { type: 'slider', start: 0, end: 100, height: 20, bottom: 30 },
  ],
  series: props.series.map((s) => ({
    name: s.name,
    type: 'line',
    smooth: true,
    symbol: 'circle',
    symbolSize: 6,
    lineStyle: {
      width: 2,
      type: s.type === 'dashed' ? 'dashed' : 'solid',
    },
    itemStyle: { color: s.color },
    data: s.data.map((d) => [d.date, d.value]),
  })),
}))
</script>

<style scoped>
.time-series-chart {
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
}

.chart-title {
  font-size: 0.9375rem;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
}

.granularity-selector {
  display: flex;
  gap: 0.25rem;
  background: #f1f5f9;
  border-radius: 6px;
  padding: 0.125rem;
}

.gran-btn {
  padding: 0.25rem 0.625rem;
  border: none;
  background: transparent;
  border-radius: 4px;
  font-size: 0.75rem;
  color: #64748b;
  cursor: pointer;
  transition: all 0.15s;
}

.gran-btn.active {
  background: #ffffff;
  color: #0f172a;
  font-weight: 600;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.gran-btn:hover:not(.active) {
  color: #334155;
}
</style>
