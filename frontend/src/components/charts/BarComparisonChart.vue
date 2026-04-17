<template>
  <div class="bar-comparison-chart">
    <div class="chart-header" v-if="title">
      <h4 class="chart-title">{{ title }}</h4>
    </div>
    <v-chart
      :option="chartOption"
      autoresize
      :style="{ height: height }"
      aria-label="비교 막대 차트"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import VChart from 'vue-echarts'
import { use } from 'echarts/core'
import { BarChart } from 'echarts/charts'
import { GridComponent, TooltipComponent, LegendComponent, TitleComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

use([BarChart, GridComponent, TooltipComponent, LegendComponent, TitleComponent, CanvasRenderer])

const props = withDefaults(defineProps<{
  title?: string
  labels: string[]
  currentData: number[]
  previousData?: number[]
  budgetData?: number[]
  currentLabel?: string
  previousLabel?: string
  budgetLabel?: string
  height?: string
}>(), {
  currentLabel: '당기',
  previousLabel: '전기',
  budgetLabel: '예산',
  height: '400px',
})

function formatValue(value: number) {
  if (Math.abs(value) >= 1e8) return `${(value / 1e8).toFixed(1)}억`
  if (Math.abs(value) >= 1e4) return `${(value / 1e4).toFixed(0)}만`
  return value.toLocaleString('ko-KR')
}

const chartOption = computed(() => {
  const series: Array<{
    name: string
    type: 'bar'
    data: number[]
    itemStyle: { color: string; borderRadius: number[] }
    barMaxWidth: number
  }> = [
    {
      name: props.currentLabel,
      type: 'bar',
      data: props.currentData,
      itemStyle: { color: '#3b82f6', borderRadius: [4, 4, 0, 0] },
      barMaxWidth: 40,
    },
  ]

  if (props.previousData?.length) {
    series.push({
      name: props.previousLabel,
      type: 'bar',
      data: props.previousData,
      itemStyle: { color: '#94a3b8', borderRadius: [4, 4, 0, 0] },
      barMaxWidth: 40,
    })
  }

  if (props.budgetData?.length) {
    series.push({
      name: props.budgetLabel,
      type: 'bar',
      data: props.budgetData,
      itemStyle: { color: '#f59e0b', borderRadius: [4, 4, 0, 0] },
      barMaxWidth: 40,
    })
  }

  return {
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      formatter: (params: Array<{ seriesName: string; value: number; color: string }>) => {
        if (!params.length) return ''
        let html = `<div style="font-weight:600;margin-bottom:4px">${props.labels[params[0].value as unknown as number] ?? ''}</div>`
        params.forEach((p) => {
          html += `<div style="display:flex;align-items:center;gap:6px">
            <span style="display:inline-block;width:10px;height:10px;border-radius:2px;background:${p.color}"></span>
            <span>${p.seriesName}: ${formatValue(p.value)}</span>
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
      bottom: '12%',
      top: '8%',
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: props.labels,
      axisLabel: { color: '#94a3b8', fontSize: 11 },
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
    series,
  }
})
</script>

<style scoped>
.bar-comparison-chart {
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
