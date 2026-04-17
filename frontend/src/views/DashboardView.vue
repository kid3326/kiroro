<template>
  <div class="dashboard-container">
    <OfflineBanner :last-updated="dashboardStore.lastUpdated" />

    <!-- Alert banners -->
    <div class="alert-section" v-if="activeAlerts.length > 0">
      <AlertBanner
        v-for="alert in activeAlerts.slice(0, 3)"
        :key="alert.id"
        :message="alert.message"
        :severity="alert.severity"
        :title="alert.title"
        :timestamp="alert.triggeredAt"
        :visible="true"
        acknowledge-label="확인"
        @acknowledge="acknowledgeAlert(alert.id)"
        @close="dismissAlert(alert.id)"
      />
    </div>

    <!-- Filter bar -->
    <div class="filter-bar">
      <DateRangePicker
        v-model="filterStore.dateRange"
        @update:model-value="onFilterChange"
      />
      <MultiFilter
        :categories="filterOptions.categories"
        :brands="filterOptions.brands"
        :skus="filterOptions.skus"
        :channels="filterOptions.channels"
        v-model:selected-categories="filterStore.selectedCategories"
        v-model:selected-brands="filterStore.selectedBrands"
        v-model:selected-skus="filterStore.selectedSkus"
        v-model:selected-channels="filterStore.selectedChannels"
        @change="onFilterChange"
      />
      <SavedFilters @load="onFilterChange" />
      <div class="filter-actions">
        <button class="btn-export" @click="showExportDialog = true">
          <i class="pi pi-download"></i> 내보내기
        </button>
      </div>
    </div>

    <!-- KPI Cards (above-the-fold) -->
    <section class="kpi-section" aria-label="주요 KPI">
      <KpiCard
        v-for="kpi in dashboardStore.kpiMetrics"
        :key="kpi.label"
        :title="kpi.label"
        :value="kpi.value"
        :format="kpi.format"
        :change-percent="kpi.changePercent"
        :loading="dashboardStore.isLoading"
      />
      <template v-if="dashboardStore.isLoading && dashboardStore.kpiMetrics.length === 0">
        <KpiCard v-for="n in 6" :key="'skel-' + n" title="" :value="0" :loading="true" />
      </template>
    </section>

    <!-- Charts section (lazy loaded) -->
    <section class="charts-section" aria-label="차트">
      <div class="chart-row">
        <div class="chart-col chart-col-wide">
          <Suspense>
            <TimeSeriesChart
              title="매출 추이"
              :series="revenueSeries"
              :show-granularity="true"
              v-model="dashboardStore.granularity"
              @update:model-value="dashboardStore.setGranularity($event as 'daily' | 'weekly' | 'monthly' | 'quarterly' | 'yearly')"
            />
            <template #fallback>
              <LoadingSkeleton variant="chart" height="400px" />
            </template>
          </Suspense>
        </div>
        <div class="chart-col chart-col-narrow">
          <Suspense>
            <PieCompositionChart
              title="채널별 광고비"
              :data="dashboardStore.adSpendByChannel"
              :donut="true"
            />
            <template #fallback>
              <LoadingSkeleton variant="chart" height="400px" />
            </template>
          </Suspense>
        </div>
      </div>

      <div class="chart-row">
        <div class="chart-col chart-col-half">
          <Suspense>
            <BarComparisonChart
              title="YoY / MoM 비교"
              :labels="dashboardStore.comparisonData.labels"
              :current-data="dashboardStore.comparisonData.current"
              :previous-data="dashboardStore.comparisonData.previous"
              :budget-data="dashboardStore.comparisonData.budget"
            />
            <template #fallback>
              <LoadingSkeleton variant="chart" height="400px" />
            </template>
          </Suspense>
        </div>
        <div class="chart-col chart-col-half">
          <Suspense>
            <DrillDownChart
              title="카테고리별 매출"
              :data="drillDownData"
            />
            <template #fallback>
              <LoadingSkeleton variant="chart" height="400px" />
            </template>
          </Suspense>
        </div>
      </div>
    </section>

    <!-- P&L Summary -->
    <section class="pnl-section" aria-label="P&L 요약">
      <PnlSummary :rows="dashboardStore.pnlRows" :loading="dashboardStore.isLoading" />
    </section>

    <!-- Product hierarchy & data table -->
    <section class="product-section" aria-label="상품 데이터">
      <div class="product-row">
        <div class="product-col-tree">
          <ProductHierarchy
            :nodes="dashboardStore.productHierarchy"
            :loading="dashboardStore.isLoading"
            @select="onProductSelect"
          />
        </div>
        <div class="product-col-table">
          <div class="product-table-card">
            <div class="table-header">
              <h3 class="table-title">상품별 데이터</h3>
              <div class="product-search">
                <AutoComplete
                  v-model="productSearchQuery"
                  :suggestions="productSuggestions"
                  @complete="searchProducts"
                  placeholder="상품명 또는 SKU 검색"
                  field="name"
                  :delay="300"
                  class="product-autocomplete"
                />
              </div>
            </div>
            <DataTable
              :value="dashboardStore.productTableData.content"
              :paginator="true"
              :rows="50"
              :totalRecords="dashboardStore.productTableData.totalElements"
              :lazy="true"
              :loading="dashboardStore.isLoading"
              @page="onPageChange"
              sortMode="single"
              removableSort
              stripedRows
              class="p-datatable-sm"
              aria-label="상품 데이터 테이블"
            >
              <Column field="sku" header="SKU" sortable style="min-width: 100px" />
              <Column field="name" header="상품명" sortable style="min-width: 150px" />
              <Column field="category" header="카테고리" sortable style="min-width: 100px" />
              <Column field="brand" header="브랜드" sortable style="min-width: 100px" />
              <Column field="salesVolume" header="판매량" sortable style="min-width: 80px">
                <template #body="{ data }">
                  {{ data.salesVolume.toLocaleString('ko-KR') }}
                </template>
              </Column>
              <Column field="revenue" header="매출" sortable style="min-width: 120px">
                <template #body="{ data }">
                  {{ formatCurrency(data.revenue) }}
                </template>
              </Column>
              <Column field="grossProfit" header="매출총이익" sortable style="min-width: 120px">
                <template #body="{ data }">
                  {{ formatCurrency(data.grossProfit) }}
                </template>
              </Column>
              <Column field="inventoryQty" header="재고" sortable style="min-width: 80px">
                <template #body="{ data }">
                  {{ data.inventoryQty.toLocaleString('ko-KR') }}
                </template>
              </Column>
              <Column field="turnoverRatio" header="회전율" sortable style="min-width: 80px">
                <template #body="{ data }">
                  {{ data.turnoverRatio.toFixed(2) }}
                </template>
              </Column>
            </DataTable>
          </div>
        </div>
      </div>
    </section>

    <!-- Export Dialog -->
    <ExportDialog v-model:visible="showExportDialog" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, defineAsyncComponent } from 'vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import AutoComplete from 'primevue/autocomplete'
import { useDashboardStore } from '@/stores/dashboardStore'
import { useFilterStore } from '@/stores/filterStore'
import { useAlertStore } from '@/stores/alertStore'
import apiClient from '@/services/apiService'
import KpiCard from '@/components/dashboard/KpiCard.vue'
import PnlSummary from '@/components/dashboard/PnlSummary.vue'
import ProductHierarchy from '@/components/dashboard/ProductHierarchy.vue'
import AlertBanner from '@/components/dashboard/AlertBanner.vue'
import OfflineBanner from '@/components/common/OfflineBanner.vue'
import LoadingSkeleton from '@/components/common/LoadingSkeleton.vue'
import DateRangePicker from '@/components/filters/DateRangePicker.vue'
import MultiFilter from '@/components/filters/MultiFilter.vue'
import SavedFilters from '@/components/filters/SavedFilters.vue'

// Lazy load heavy chart components
const TimeSeriesChart = defineAsyncComponent(() => import('@/components/charts/TimeSeriesChart.vue'))
const BarComparisonChart = defineAsyncComponent(() => import('@/components/charts/BarComparisonChart.vue'))
const PieCompositionChart = defineAsyncComponent(() => import('@/components/charts/PieCompositionChart.vue'))
const DrillDownChart = defineAsyncComponent(() => import('@/components/charts/DrillDownChart.vue'))
const ExportDialog = defineAsyncComponent(() => import('@/components/export/ExportDialog.vue'))

const dashboardStore = useDashboardStore()
const filterStore = useFilterStore()
const alertStore = useAlertStore()

const showExportDialog = ref(false)
const productSearchQuery = ref('')
const productSuggestions = ref<Array<{ name: string; sku: string }>>([])
const dismissedAlerts = ref<Set<number>>(new Set())

const filterOptions = ref({
  categories: [] as string[],
  brands: [] as string[],
  skus: [] as string[],
  channels: ['Naver', 'Google', 'Meta', 'Others'],
})

const activeAlerts = computed(() =>
  alertStore.alerts.filter((a) => !a.isAcknowledged && !dismissedAlerts.value.has(a.id)),
)

const revenueSeries = computed(() => {
  if (!dashboardStore.revenueTimeSeries.length) return []
  return [
    {
      name: '매출',
      data: dashboardStore.revenueTimeSeries,
      color: '#3b82f6',
    },
  ]
})

const drillDownData = computed(() => {
  return dashboardStore.productHierarchy.map((node) => ({
    name: node.name,
    value: node.revenue,
    children: node.children?.map((child) => ({
      name: child.name,
      value: child.revenue,
      children: child.children?.map((grandchild) => ({
        name: grandchild.name,
        value: grandchild.revenue,
      })),
    })),
  }))
})

function formatCurrency(value: number) {
  return new Intl.NumberFormat('ko-KR', { style: 'currency', currency: 'KRW', maximumFractionDigits: 0 }).format(value)
}

function onFilterChange() {
  filterStore.syncToUrl()
  dashboardStore.fetchAll()
}

function onProductSelect(node: { name: string }) {
  productSearchQuery.value = node.name
}

async function searchProducts(event: { query: string }) {
  try {
    const response = await apiClient.get('/search/products', { params: { q: event.query } })
    productSuggestions.value = response.data ?? []
  } catch {
    productSuggestions.value = []
  }
}

function onPageChange(event: { page: number; rows: number }) {
  dashboardStore.fetchProducts(event.page, event.rows)
}

async function acknowledgeAlert(id: number) {
  await alertStore.acknowledgeAlert(id)
}

function dismissAlert(id: number) {
  dismissedAlerts.value.add(id)
}

onMounted(async () => {
  filterStore.syncFromUrl()
  await Promise.all([
    dashboardStore.fetchAll(),
    alertStore.fetchAlerts(),
  ])
})
</script>

<style scoped>
.dashboard-container {
  padding: 1.5rem;
  max-width: 1440px;
  margin: 0 auto;
}

.alert-section {
  margin-bottom: 1rem;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  margin-bottom: 1.5rem;
  flex-wrap: wrap;
  background: #ffffff;
  padding: 0.75rem 1rem;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  border: 1px solid #e2e8f0;
}

.filter-actions {
  margin-left: auto;
}

.btn-export {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.5rem 1rem;
  background: #3b82f6;
  color: #ffffff;
  border: none;
  border-radius: 6px;
  font-size: 0.8125rem;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.15s;
}

.btn-export:hover {
  background: #2563eb;
}

.kpi-section {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 1rem;
  margin-bottom: 1.5rem;
}

.charts-section {
  margin-bottom: 1.5rem;
}

.chart-row {
  display: flex;
  gap: 1rem;
  margin-bottom: 1rem;
}

.chart-col-wide { flex: 2; min-width: 0; }
.chart-col-narrow { flex: 1; min-width: 0; }
.chart-col-half { flex: 1; min-width: 0; }

.pnl-section {
  margin-bottom: 1.5rem;
}

.product-section {
  margin-bottom: 1.5rem;
}

.product-row {
  display: flex;
  gap: 1rem;
}

.product-col-tree {
  flex: 0 0 320px;
  min-width: 0;
}

.product-col-table {
  flex: 1;
  min-width: 0;
}

.product-table-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 1.5rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  border: 1px solid #e2e8f0;
}

.table-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.table-title {
  font-size: 1rem;
  font-weight: 700;
  color: #0f172a;
  margin: 0;
}

.product-autocomplete {
  width: 280px;
}

/* Responsive */
@media (max-width: 1024px) {
  .chart-row {
    flex-direction: column;
  }

  .product-row {
    flex-direction: column;
  }

  .product-col-tree {
    flex: none;
  }
}

@media (max-width: 768px) {
  .dashboard-container {
    padding: 1rem;
  }

  .kpi-section {
    grid-template-columns: repeat(2, 1fr);
  }

  .filter-bar {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-actions {
    margin-left: 0;
  }
}
</style>
