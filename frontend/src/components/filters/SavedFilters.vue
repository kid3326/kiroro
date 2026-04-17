<template>
  <div class="saved-filters">
    <div class="saved-filters-dropdown">
      <button class="btn-saved" @click="toggleDropdown" aria-label="저장된 필터">
        <i class="pi pi-bookmark"></i>
        저장된 필터
        <i :class="showDropdown ? 'pi pi-chevron-up' : 'pi pi-chevron-down'" class="dropdown-icon"></i>
      </button>
      <div v-if="showDropdown" class="dropdown-menu" role="menu">
        <div class="dropdown-header">
          <span class="dropdown-title">저장된 필터</span>
          <button class="btn-save-new" @click="showSaveDialog = true">
            <i class="pi pi-plus"></i> 현재 필터 저장
          </button>
        </div>
        <div v-if="filterStore.isLoadingSaved" class="dropdown-loading">
          <i class="pi pi-spin pi-spinner"></i> 로딩 중...
        </div>
        <div v-else-if="filterStore.savedFilters.length === 0" class="dropdown-empty">
          저장된 필터가 없습니다.
        </div>
        <div v-else class="dropdown-list">
          <div
            v-for="filter in filterStore.savedFilters"
            :key="filter.id"
            class="dropdown-item"
            role="menuitem"
          >
            <button class="filter-name" @click="loadFilter(filter)">
              <i class="pi pi-filter"></i>
              {{ filter.name }}
            </button>
            <button class="btn-delete" @click.stop="deleteFilter(filter.id)" aria-label="필터 삭제">
              <i class="pi pi-trash"></i>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Save dialog -->
    <Dialog
      v-model:visible="showSaveDialog"
      header="필터 저장"
      :modal="true"
      :style="{ width: '360px' }"
      class="save-dialog"
    >
      <div class="save-form">
        <label class="save-label" for="filter-name">필터 이름</label>
        <InputText
          id="filter-name"
          v-model="newFilterName"
          placeholder="필터 이름을 입력하세요"
          class="save-input"
          @keyup.enter="saveFilter"
        />
      </div>
      <template #footer>
        <button class="btn-cancel" @click="showSaveDialog = false">취소</button>
        <button class="btn-confirm" @click="saveFilter" :disabled="!newFilterName.trim()">저장</button>
      </template>
    </Dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import Dialog from 'primevue/dialog'
import InputText from 'primevue/inputtext'
import { useFilterStore, type SavedFilter } from '@/stores/filterStore'

const emit = defineEmits<{
  load: []
}>()

const filterStore = useFilterStore()
const showDropdown = ref(false)
const showSaveDialog = ref(false)
const newFilterName = ref('')

function toggleDropdown() {
  showDropdown.value = !showDropdown.value
  if (showDropdown.value) {
    filterStore.fetchSavedFilters()
  }
}

async function loadFilter(filter: SavedFilter) {
  await filterStore.loadFilter(filter)
  showDropdown.value = false
  emit('load')
}

async function deleteFilter(id: number) {
  await filterStore.deleteFilter(id)
}

async function saveFilter() {
  if (!newFilterName.value.trim()) return
  await filterStore.saveFilter(newFilterName.value.trim())
  newFilterName.value = ''
  showSaveDialog.value = false
}

onMounted(() => {
  // Close dropdown on outside click
  document.addEventListener('click', (e) => {
    const target = e.target as HTMLElement
    if (!target.closest('.saved-filters-dropdown')) {
      showDropdown.value = false
    }
  })
})
</script>

<style scoped>
.saved-filters {
  position: relative;
}

.saved-filters-dropdown {
  position: relative;
}

.btn-saved {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.375rem 0.75rem;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  border-radius: 6px;
  font-size: 0.8125rem;
  color: #64748b;
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
}

.btn-saved:hover {
  border-color: #3b82f6;
  color: #3b82f6;
}

.dropdown-icon {
  font-size: 0.625rem;
}

.dropdown-menu {
  position: absolute;
  top: calc(100% + 4px);
  left: 0;
  z-index: 100;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  min-width: 280px;
  max-height: 320px;
  overflow-y: auto;
}

.dropdown-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.75rem 1rem;
  border-bottom: 1px solid #f1f5f9;
}

.dropdown-title {
  font-size: 0.8125rem;
  font-weight: 600;
  color: #334155;
}

.btn-save-new {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  background: none;
  border: none;
  color: #3b82f6;
  font-size: 0.75rem;
  cursor: pointer;
  font-weight: 500;
}

.btn-save-new:hover {
  color: #2563eb;
}

.dropdown-loading,
.dropdown-empty {
  padding: 1rem;
  text-align: center;
  font-size: 0.8125rem;
  color: #94a3b8;
}

.dropdown-list {
  padding: 0.25rem 0;
}

.dropdown-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 0.5rem;
}

.filter-name {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem;
  background: none;
  border: none;
  font-size: 0.8125rem;
  color: #334155;
  cursor: pointer;
  text-align: left;
  border-radius: 4px;
  transition: background-color 0.15s;
}

.filter-name:hover {
  background-color: #f8fafc;
}

.btn-delete {
  background: none;
  border: none;
  color: #94a3b8;
  cursor: pointer;
  padding: 0.375rem;
  border-radius: 4px;
  transition: color 0.15s;
}

.btn-delete:hover {
  color: #ef4444;
}

.save-form {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.save-label {
  font-size: 0.8125rem;
  font-weight: 600;
  color: #334155;
}

.save-input {
  width: 100%;
}

.btn-cancel,
.btn-confirm {
  padding: 0.5rem 1rem;
  border-radius: 6px;
  font-size: 0.8125rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.15s;
}

.btn-cancel {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  color: #64748b;
}

.btn-cancel:hover {
  background: #f8fafc;
}

.btn-confirm {
  background: #3b82f6;
  border: none;
  color: #ffffff;
  margin-left: 0.5rem;
}

.btn-confirm:hover {
  background: #2563eb;
}

.btn-confirm:disabled {
  background: #94a3b8;
  cursor: not-allowed;
}
</style>
