<template>
  <div class="pie-composition-chart">
    <div class="chart-header" v-if="title">
      <h4 class="chart-title">{{ title }}</h4>
    </div>
    <v-chart
      :option="chartOption"
      autoresize
      :style="{ height: height }"
      aria-label="구성비 파이 차트"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { PieChart } from 'echarts/charts'
import { TooltipComponent, LegendComponent, TitleComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

use([PieChart, TooltipComponent, LegendComponent, TitleComponent, CanvasRenderer])

export interface PieItem {
  name: string
  value: number
}

const props = withDefaults(defineProps<{
  title?: string
  data: PieItem[]
  height?: string
  showLabel?: boolean
  donut?: boolean
}>(), {
  height: '400px',
  showLabel: true,
  donut: false,
})

const COLORS = ['#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899', '#06b6d4', '#84cc16']

function formatValue(value: number) {
  if (Math.abs(value) >= 1e8) return `${(value / 1e8).toFixed(1)}억`
  if (Math.abs(value) >= 1e4) return `${(value / 1e4).toFixed(0)}만`
  return value.toLocaleString('ko-KR')
}

const chartOption = computed(() => ({
  tooltip: {
    trigger: 'item',
    formatter: (params: { name: string; value: number; percent: number; color: string }) => {
      return `<div style="display:flex;align-items:center;gap:6px">
        <span style="display:inline-block;width:10px;height:10px;border-radius:50%;background:${params.color}"></span>
        <span>${params.name}: ${formatValue(params.value)} (${params.percent.toFixed(1)}%)</span>
      </div>`
    },
  },
  legend: {
    orient: 'vertical',
    right: '5%',
    top: 'center',
    textStyle: { fontSize: 12, color: '#64748b' },
  },
  color: COLORS,
  series: [
    {
      type: 'pie',
      radius: props.donut ? ['40%', '70%'] : ['0%', '70%'],
      center: ['40%', '50%'],
      avoidLabelOverlap: true,
      itemStyle: {
        borderRadius: 6,
        borderColor: '#fff',
        borderWidth: 2,
      },
      label: {
        show: props.showLabel,
        formatter: '{b}: {d}%',
        fontSize: 11,
        color: '#64748b',
      },
      emphasis: {
        label: { show: true, fontSize: 13, fontWeight: 'bold' },
        itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0, 0, 0, 0.2)' },
      },
      data: props.data.map((item) => ({
        name: item.name,
        value: item.value,
      })),
    },
  ],
}))
</script>

<style scoped>
.pie-composition-chart {
  background: #ffffff;
  border-radius: 12px;
  padding: 1.25rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  border: 1px solid #e2e8f0;
}

.chart-header {
  margin-bottom: 0.75rem;
}

.chart-title {
  font-size: 0.9375rem;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
}
</style>
