import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import apiClient from '@/services/apiService'

export interface SavedFilter {
  id: number
  name: string
  filterCriteria: {
    dateRange?: { from: string; to: string } | null
    categories?: string[]
    brands?: string[]
    skus?: string[]
    channels?: string[]
  }
  createdAt: string
}

export const useFilterStore = defineStore('filter', () => {
  const dateRange = ref<{ from: string; to: string } | null>(null)
  const selectedCategories = ref<string[]>([])
  const selectedBrands = ref<string[]>([])
  const selectedSkus = ref<string[]>([])
  const selectedChannels = ref<string[]>([])

  const savedFilters = ref<SavedFilter[]>([])
  const isLoadingSaved = ref(false)

  const hasActiveFilters = computed(() => {
    return (
      dateRange.value !== null ||
      selectedCategories.value.length > 0 ||
      selectedBrands.value.length > 0 ||
      selectedSkus.value.length > 0 ||
      selectedChannels.value.length > 0
    )
  })

  const currentFilterCriteria = computed(() => ({
    dateRange: dateRange.value,
    categories: selectedCategories.value,
    brands: selectedBrands.value,
    skus: selectedSkus.value,
    channels: selectedChannels.value,
  }))

  function resetFilters() {
    dateRange.value = null
    selectedCategories.value = []
    selectedBrands.value = []
    selectedSkus.value = []
    selectedChannels.value = []
    syncToUrl()
  }

  function syncToUrl() {
    const params = new URLSearchParams()
    if (dateRange.value?.from) params.set('from', dateRange.value.from)
    if (dateRange.value?.to) params.set('to', dateRange.value.to)
    if (selectedCategories.value.length) params.set('category', selectedCategories.value.join(','))
    if (selectedBrands.value.length) params.set('brand', selectedBrands.value.join(','))
    if (selectedSkus.value.length) params.set('sku', selectedSkus.value.join(','))
    if (selectedChannels.value.length) params.set('channel', selectedChannels.value.join(','))

    const queryString = params.toString()
    const newUrl = queryString
      ? `${window.location.pathname}?${queryString}`
      : window.location.pathname
    window.history.replaceState({}, '', newUrl)
  }

  function syncFromUrl() {
    const params = new URLSearchParams(window.location.search)
    const from = params.get('from')
    const to = params.get('to')
    if (from && to) {
      dateRange.value = { from, to }
    }
    const category = params.get('category')
    if (category) selectedCategories.value = category.split(',')
    const brand = params.get('brand')
    if (brand) selectedBrands.value = brand.split(',')
    const sku = params.get('sku')
    if (sku) selectedSkus.value = sku.split(',')
    const channel = params.get('channel')
    if (channel) selectedChannels.value = channel.split(',')
  }

  async function fetchSavedFilters() {
    isLoadingSaved.value = true
    try {
      const response = await apiClient.get('/filters')
      savedFilters.value = response.data ?? []
    } catch {
      savedFilters.value = []
    } finally {
      isLoadingSaved.value = false
    }
  }

  async function saveFilter(name: string) {
    const response = await apiClient.post('/filters', {
      name,
      filterCriteria: currentFilterCriteria.value,
    })
    savedFilters.value.push(response.data)
    return response.data
  }

  async function loadFilter(filter: SavedFilter) {
    const criteria = filter.filterCriteria
    dateRange.value = criteria.dateRange ?? null
    selectedCategories.value = criteria.categories ?? []
    selectedBrands.value = criteria.brands ?? []
    selectedSkus.value = criteria.skus ?? []
    selectedChannels.value = criteria.channels ?? []
    syncToUrl()
  }

  async function deleteFilter(id: number) {
    await apiClient.delete(`/filters/${id}`)
    savedFilters.value = savedFilters.value.filter((f) => f.id !== id)
  }

  return {
    dateRange,
    selectedCategories,
    selectedBrands,
    selectedSkus,
    selectedChannels,
    savedFilters,
    isLoadingSaved,
    hasActiveFilters,
    currentFilterCriteria,
    resetFilters,
    syncToUrl,
    syncFromUrl,
    fetchSavedFilters,
    saveFilter,
    loadFilter,
    deleteFilter,
  }
})
