import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import apiClient from '@/services/apiService'
import { useFilterStore } from '@/stores/filterStore'
import { cacheService } from '@/services/cacheService'

export interface KpiMetric {
  label: string
  value: number
  format: 'currency' | 'number' | 'percent'
  changePercent?: number
}

export interface PnlRow {
  label: string
  current: number
  previous: number
  budget: number
  changePercent: number
  budgetVariance: number
}

export interface TimeSeriesPoint {
  date: string
  value: number
}

export interface ComparisonData {
  labels: string[]
  current: number[]
  previous: number[]
  budget: number[]
}

export interface PieData {
  name: string
  value: number
}

export interface ProductNode {
  id: number
  name: string
  level: 'category' | 'subcategory' | 'brand' | 'sku'
  revenue: number
  salesVolume: number
  children?: ProductNode[]
}

export interface ProductTableRow {
  sku: string
  name: string
  category: string
  brand: string
  salesVolume: number
  revenue: number
  grossProfit: number
  inventoryQty: number
  turnoverRatio: number
}

export interface ProductTableResponse {
  content: ProductTableRow[]
  totalElements: number
  totalPages: number
  page: number
  size: number
}

export const useDashboardStore = defineStore('dashboard', () => {
  const isLoading = ref(false)
  const error = ref<string | null>(null)
  const lastUpdated = ref<number | null>(null)

  // KPI data
  const kpiMetrics = ref<KpiMetric[]>([])

  // P&L summary
  const pnlRows = ref<PnlRow[]>([])

  // Chart data
  const revenueTimeSeries = ref<TimeSeriesPoint[]>([])
  const comparisonData = ref<ComparisonData>({ labels: [], current: [], previous: [], budget: [] })
  const adSpendByChannel = ref<PieData[]>([])
  const revenueByCategoryPie = ref<PieData[]>([])

  // Product data
  const productHierarchy = ref<ProductNode[]>([])
  const productTableData = ref<ProductTableResponse>({
    content: [],
    totalElements: 0,
    totalPages: 0,
    page: 0,
    size: 50,
  })

  // Granularity
  const granularity = ref<'daily' | 'weekly' | 'monthly' | 'quarterly' | 'yearly'>('monthly')

  const hasData = computed(() => kpiMetrics.value.length > 0)

  function buildQueryParams() {
    const filterStore = useFilterStore()
    const params: Record<string, string> = {}
    if (filterStore.dateRange?.from) params.from = filterStore.dateRange.from
    if (filterStore.dateRange?.to) params.to = filterStore.dateRange.to
    if (filterStore.selectedCategories.length) params.category = filterStore.selectedCategories.join(',')
    if (filterStore.selectedBrands.length) params.brand = filterStore.selectedBrands.join(',')
    if (filterStore.selectedSkus.length) params.sku = filterStore.selectedSkus.join(',')
    if (filterStore.selectedChannels.length) params.channel = filterStore.selectedChannels.join(',')
    params.granularity = granularity.value
    return params
  }

  async function fetchSummary() {
    try {
      const params = buildQueryParams()
      const response = await apiClient.get('/dashboard/summary', { params })
      const data = response.data

      kpiMetrics.value = [
        { label: '총 매출', value: data.totalRevenue ?? 0, format: 'currency', changePercent: data.revenueChange },
        { label: '순매출', value: data.netRevenue ?? 0, format: 'currency', changePercent: data.netRevenueChange },
        { label: '매출총이익', value: data.grossProfit ?? 0, format: 'currency', changePercent: data.grossProfitChange },
        { label: 'EBITDA', value: data.ebitda ?? 0, format: 'currency', changePercent: data.ebitdaChange },
        { label: '영업이익', value: data.operatingProfit ?? 0, format: 'currency', changePercent: data.operatingProfitChange },
        { label: '순이익', value: data.netProfit ?? 0, format: 'currency', changePercent: data.netProfitChange },
      ]

      pnlRows.value = data.pnlSummary ?? []
      lastUpdated.value = Date.now()
      cacheService.set('dashboard_summary', { kpiMetrics: kpiMetrics.value, pnlRows: pnlRows.value })
    } catch (err) {
      // Try cache fallback
      const cached = cacheService.get<{ kpiMetrics: KpiMetric[]; pnlRows: PnlRow[] }>('dashboard_summary')
      if (cached) {
        kpiMetrics.value = cached.kpiMetrics
        pnlRows.value = cached.pnlRows
        lastUpdated.value = cacheService.getTimestamp('dashboard_summary')
      }
      throw err
    }
  }

  async function fetchRevenueSeries() {
    try {
      const params = buildQueryParams()
      const response = await apiClient.get('/dashboard/revenue', { params })
      revenueTimeSeries.value = response.data.series ?? []
      cacheService.set('dashboard_revenue', revenueTimeSeries.value)
    } catch {
      const cached = cacheService.get<TimeSeriesPoint[]>('dashboard_revenue')
      if (cached) revenueTimeSeries.value = cached
    }
  }

  async function fetchComparison() {
    try {
      const params = buildQueryParams()
      const response = await apiClient.get('/dashboard/comparison', { params })
      comparisonData.value = response.data ?? { labels: [], current: [], previous: [], budget: [] }
      cacheService.set('dashboard_comparison', comparisonData.value)
    } catch {
      const cached = cacheService.get<ComparisonData>('dashboard_comparison')
      if (cached) comparisonData.value = cached
    }
  }

  async function fetchAdvertising() {
    try {
      const params = buildQueryParams()
      const response = await apiClient.get('/dashboard/advertising', { params })
      adSpendByChannel.value = response.data.byChannel ?? []
      cacheService.set('dashboard_advertising', adSpendByChannel.value)
    } catch {
      const cached = cacheService.get<PieData[]>('dashboard_advertising')
      if (cached) adSpendByChannel.value = cached
    }
  }

  async function fetchProducts(page = 0, size = 50) {
    try {
      const params = { ...buildQueryParams(), page: String(page), size: String(size) }
      const response = await apiClient.get('/dashboard/products', { params })
      productTableData.value = response.data
      productHierarchy.value = response.data.hierarchy ?? []
      cacheService.set('dashboard_products', { table: productTableData.value, hierarchy: productHierarchy.value })
    } catch {
      const cached = cacheService.get<{ table: ProductTableResponse; hierarchy: ProductNode[] }>('dashboard_products')
      if (cached) {
        productTableData.value = cached.table
        productHierarchy.value = cached.hierarchy
      }
    }
  }

  async function fetchAll() {
    isLoading.value = true
    error.value = null
    try {
      await Promise.all([
        fetchSummary(),
        fetchRevenueSeries(),
        fetchComparison(),
        fetchAdvertising(),
        fetchProducts(),
      ])
    } catch (err: unknown) {
      error.value = err instanceof Error ? err.message : '데이터를 불러오는 중 오류가 발생했습니다.'
    } finally {
      isLoading.value = false
    }
  }

  function setGranularity(g: typeof granularity.value) {
    granularity.value = g
    fetchRevenueSeries()
    fetchComparison()
  }

  return {
    isLoading,
    error,
    lastUpdated,
    kpiMetrics,
    pnlRows,
    revenueTimeSeries,
    comparisonData,
    adSpendByChannel,
    revenueByCategoryPie,
    productHierarchy,
    productTableData,
    granularity,
    hasData,
    fetchSummary,
    fetchRevenueSeries,
    fetchComparison,
    fetchAdvertising,
    fetchProducts,
    fetchAll,
    setGranularity,
    buildQueryParams,
  }
})
