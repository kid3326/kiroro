<template>
  <div class="pnl-summary">
    <div class="pnl-header">
      <h3 class="pnl-title">P&amp;L 요약</h3>
    </div>
    <LoadingSkeleton v-if="loading" variant="table" :rows="8" aria-label="P&L 요약 로딩 중" />
    <div v-else class="pnl-table-wrapper">
      <table class="pnl-table" aria-label="손익계산서 요약">
        <thead>
          <tr>
            <th class="col-label">항목</th>
            <th class="col-value">당기</th>
            <th class="col-value">전기</th>
            <th class="col-value">예산</th>
            <th class="col-change">전기 대비</th>
            <th class="col-change">예산 대비</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in rows" :key="row.label" :class="{ 'row-highlight': isHighlightRow(row.label) }">
            <td class="col-label">{{ row.label }}</td>
            <td class="col-value">{{ formatCurrency(row.current) }}</td>
            <td class="col-value">{{ formatCurrency(row.previous) }}</td>
            <td class="col-value">{{ formatCurrency(row.budget) }}</td>
            <td class="col-change" :class="changeClass(row.changePercent)">
              {{ row.changePercent > 0 ? '+' : '' }}{{ row.changePercent.toFixed(1) }}%
            </td>
            <td class="col-change" :class="changeClass(row.budgetVariance)">
              {{ row.budgetVariance > 0 ? '+' : '' }}{{ row.budgetVariance.toFixed(1) }}%
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup lang="ts">
import LoadingSkeleton from '@/components/common/LoadingSkeleton.vue'

export interface PnlRow {
  label: string
  current: number
  previous: number
  budget: number
  changePercent: number
  budgetVariance: number
}

withDefaults(defineProps<{
  rows: PnlRow[]
  loading?: boolean
}>(), {
  loading: false,
})

const highlightLabels = ['매출총이익', 'EBITDA', '영업이익', '순이익']

function isHighlightRow(label: string) {
  return highlightLabels.includes(label)
}

function formatCurrency(value: number) {
  return new Intl.NumberFormat('ko-KR', {
    style: 'currency',
    currency: 'KRW',
    maximumFractionDigits: 0,
  }).format(value)
}

function changeClass(percent: number) {
  if (percent > 0) return 'positive'
  if (percent < 0) return 'negative'
  return ''
}
</script>

<style scoped>
.pnl-summary {
  background: #ffffff;
  border-radius: 12px;
  padding: 1.5rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  border: 1px solid #e2e8f0;
}

.pnl-header {
  margin-bottom: 1rem;
}

.pnl-title {
  font-size: 1rem;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
}

.pnl-table-wrapper {
  overflow-x: auto;
}

.pnl-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.875rem;
}

.pnl-table th {
  text-align: right;
  padding: 0.625rem 0.75rem;
  font-weight: 600;
  color: #64748b;
  border-bottom: 2px solid #e2e8f0;
  white-space: nowrap;
}

.pnl-table th.col-label {
  text-align: left;
}

.pnl-table td {
  padding: 0.625rem 0.75rem;
  border-bottom: 1px solid #f1f5f9;
}

.col-label {
  text-align: left;
  font-weight: 500;
  color: #334155;
  min-width: 120px;
}

.col-value {
  text-align: right;
  color: #0f172a;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.col-change {
  text-align: right;
  font-weight: 500;
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.col-change.positive { color: #059669; }
.col-change.negative { color: #dc2626; }

.row-highlight {
  background-color: #f8fafc;
}

.row-highlight .col-label {
  font-weight: 700;
  color: #0f172a;
}
</style>
