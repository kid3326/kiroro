<template>
  <div class="multi-filter">
    <div class="filter-group" v-if="categories.length > 0">
      <MultiSelect
        v-model="localCategories"
        :options="categories"
        placeholder="카테고리"
        :maxSelectedLabels="2"
        selectedItemsLabel="{0}개 선택"
        class="filter-select"
        @change="onCategoryChange"
        aria-label="카테고리 필터"
      />
    </div>
    <div class="filter-group" v-if="brands.length > 0">
      <MultiSelect
        v-model="localBrands"
        :options="brands"
        placeholder="브랜드"
        :maxSelectedLabels="2"
        selectedItemsLabel="{0}개 선택"
        class="filter-select"
        @change="onBrandChange"
        aria-label="브랜드 필터"
      />
    </div>
    <div class="filter-group" v-if="skus.length > 0">
      <MultiSelect
        v-model="localSkus"
        :options="skus"
        placeholder="SKU"
        :maxSelectedLabels="1"
        selectedItemsLabel="{0}개 선택"
        :filter="true"
        class="filter-select"
        @change="onSkuChange"
        aria-label="SKU 필터"
      />
    </div>
    <div class="filter-group">
      <MultiSelect
        v-model="localChannels"
        :options="channels"
        placeholder="채널"
        :maxSelectedLabels="2"
        selectedItemsLabel="{0}개 선택"
        class="filter-select"
        @change="onChannelChange"
        aria-label="채널 필터"
      />
    </div>
    <button v-if="hasFilters" class="btn-reset" @click="resetAll" aria-label="필터 초기화">
      <i class="pi pi-filter-slash"></i>
      초기화
    </button>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import MultiSelect from 'primevue/multiselect'

const props = withDefaults(defineProps<{
  categories: string[]
  brands: string[]
  skus: string[]
  channels: string[]
  selectedCategories: string[]
  selectedBrands: string[]
  selectedSkus: string[]
  selectedChannels: string[]
}>(), {
  categories: () => [],
  brands: () => [],
  skus: () => [],
  channels: () => ['Naver', 'Google', 'Meta', 'Others'],
})

const emit = defineEmits<{
  'update:selectedCategories': [value: string[]]
  'update:selectedBrands': [value: string[]]
  'update:selectedSkus': [value: string[]]
  'update:selectedChannels': [value: string[]]
  change: []
}>()

const localCategories = ref<string[]>([...props.selectedCategories])
const localBrands = ref<string[]>([...props.selectedBrands])
const localSkus = ref<string[]>([...props.selectedSkus])
const localChannels = ref<string[]>([...props.selectedChannels])

const hasFilters = computed(() =>
  localCategories.value.length > 0 ||
  localBrands.value.length > 0 ||
  localSkus.value.length > 0 ||
  localChannels.value.length > 0,
)

watch(() => props.selectedCategories, (v) => { localCategories.value = [...v] })
watch(() => props.selectedBrands, (v) => { localBrands.value = [...v] })
watch(() => props.selectedSkus, (v) => { localSkus.value = [...v] })
watch(() => props.selectedChannels, (v) => { localChannels.value = [...v] })

function onCategoryChange() {
  emit('update:selectedCategories', localCategories.value)
  emit('change')
}

function onBrandChange() {
  emit('update:selectedBrands', localBrands.value)
  emit('change')
}

function onSkuChange() {
  emit('update:selectedSkus', localSkus.value)
  emit('change')
}

function onChannelChange() {
  emit('update:selectedChannels', localChannels.value)
  emit('change')
}

function resetAll() {
  localCategories.value = []
  localBrands.value = []
  localSkus.value = []
  localChannels.value = []
  emit('update:selectedCategories', [])
  emit('update:selectedBrands', [])
  emit('update:selectedSkus', [])
  emit('update:selectedChannels', [])
  emit('change')
}
</script>

<style scoped>
.multi-filter {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  flex-wrap: wrap;
}

.filter-select {
  width: 160px;
}

.btn-reset {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  padding: 0.375rem 0.75rem;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  border-radius: 6px;
  font-size: 0.8125rem;
  color: #64748b;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-reset:hover {
  border-color: #ef4444;
  color: #ef4444;
}
</style>
