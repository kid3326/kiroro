<template>
  <Dialog
    :visible="visible"
    @update:visible="$emit('update:visible', $event)"
    header="데이터 내보내기"
    :modal="true"
    :style="{ width: '480px' }"
    :closable="!isExporting"
    class="export-dialog"
  >
    <div class="export-content">
      <p class="export-description">현재 필터가 적용된 데이터를 내보냅니다.</p>

      <div class="format-selection">
        <label class="format-label">내보내기 형식</label>
        <div class="format-options">
          <div
            v-for="fmt in formats"
            :key="fmt.value"
            class="format-card"
            :class="{ selected: selectedFormat === fmt.value }"
            @click="selectedFormat = fmt.value"
            role="radio"
            :aria-checked="selectedFormat === fmt.value"
          >
            <i :class="fmt.icon" class="format-icon"></i>
            <div class="format-info">
              <span class="format-name">{{ fmt.label }}</span>
              <span class="format-desc">{{ fmt.description }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- Progress -->
      <div v-if="isExporting" class="export-progress">
        <div class="progress-bar">
          <div class="progress-fill" :style="{ width: `${progress}%` }"></div>
        </div>
        <span class="progress-text">내보내기 중... {{ progress }}%</span>
      </div>

      <!-- Download link -->
      <div v-if="downloadUrl" class="export-result">
        <i class="pi pi-check-circle result-icon"></i>
        <span class="result-text">내보내기가 완료되었습니다.</span>
        <a :href="downloadUrl" class="btn-download" target="_blank" rel="noopener">
          <i class="pi pi-download"></i> 다운로드
        </a>
        <span class="result-expiry">링크는 24시간 동안 유효합니다.</span>
      </div>

      <!-- Error -->
      <div v-if="exportError" class="export-error">
        <i class="pi pi-exclamation-triangle"></i>
        <span>{{ exportError }}</span>
      </div>
    </div>

    <template #footer>
      <button class="btn-cancel" @click="$emit('update:visible', false)" :disabled="isExporting">
        {{ downloadUrl ? '닫기' : '취소' }}
      </button>
      <button
        v-if="!downloadUrl"
        class="btn-export"
        @click="startExport"
        :disabled="isExporting"
      >
        <i v-if="isExporting" class="pi pi-spin pi-spinner"></i>
        <i v-else class="pi pi-download"></i>
        {{ isExporting ? '내보내기 중...' : '내보내기' }}
      </button>
    </template>
  </Dialog>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import Dialog from 'primevue/dialog'
import apiClient from '@/services/apiService'
import { useFilterStore } from '@/stores/filterStore'

defineProps<{
  visible: boolean
}>()

defineEmits<{
  'update:visible': [value: boolean]
}>()

const filterStore = useFilterStore()

type ExportFormat = 'excel' | 'pdf' | 'ppt'

const formats = [
  { value: 'excel' as ExportFormat, label: 'Excel', icon: 'pi pi-file-excel', description: '카테고리별 시트 분리' },
  { value: 'pdf' as ExportFormat, label: 'PDF', icon: 'pi pi-file-pdf', description: '표지, 목차, 페이지 번호 포함' },
  { value: 'ppt' as ExportFormat, label: 'PowerPoint', icon: 'pi pi-file', description: '차트당 1슬라이드 + 데이터 테이블' },
]

const selectedFormat = ref<ExportFormat>('excel')
const isExporting = ref(false)
const progress = ref(0)
const downloadUrl = ref<string | null>(null)
const exportError = ref<string | null>(null)

async function startExport() {
  isExporting.value = true
  progress.value = 0
  downloadUrl.value = null
  exportError.value = null

  try {
    // Simulate progress
    const progressInterval = setInterval(() => {
      if (progress.value < 90) {
        progress.value += Math.random() * 15
      }
    }, 500)

    const response = await apiClient.post(`/export/${selectedFormat.value}`, {
      filters: filterStore.currentFilterCriteria,
      format: selectedFormat.value.toUpperCase(),
    })

    clearInterval(progressInterval)
    progress.value = 100

    downloadUrl.value = response.data.downloadUrl
  } catch (err: unknown) {
    exportError.value = err instanceof Error ? err.message : '내보내기 중 오류가 발생했습니다.'
  } finally {
    isExporting.value = false
  }
}

watch(
  () => selectedFormat.value,
  () => {
    downloadUrl.value = null
    exportError.value = null
    progress.value = 0
  },
)
</script>

<style scoped>
.export-content {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.export-description {
  font-size: 0.875rem;
  color: #64748b;
  margin: 0;
}

.format-label {
  display: block;
  font-size: 0.8125rem;
  font-weight: 600;
  color: #334155;
  margin-bottom: 0.5rem;
}

.format-options {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.format-card {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  border: 2px solid #e2e8f0;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s;
}

.format-card:hover {
  border-color: #93c5fd;
}

.format-card.selected {
  border-color: #3b82f6;
  background-color: #eff6ff;
}

.format-icon {
  font-size: 1.5rem;
  color: #64748b;
}

.format-card.selected .format-icon {
  color: #3b82f6;
}

.format-info {
  display: flex;
  flex-direction: column;
}

.format-name {
  font-size: 0.875rem;
  font-weight: 600;
  color: #0f172a;
}

.format-desc {
  font-size: 0.75rem;
  color: #94a3b8;
}

.export-progress {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.progress-bar {
  height: 6px;
  background: #e2e8f0;
  border-radius: 3px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: #3b82f6;
  border-radius: 3px;
  transition: width 0.3s ease;
}

.progress-text {
  font-size: 0.8125rem;
  color: #64748b;
  text-align: center;
}

.export-result {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.5rem;
  padding: 1rem;
  background: #f0fdf4;
  border-radius: 8px;
  border: 1px solid #bbf7d0;
}

.result-icon {
  font-size: 2rem;
  color: #22c55e;
}

.result-text {
  font-size: 0.875rem;
  font-weight: 600;
  color: #166534;
}

.btn-download {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.5rem 1rem;
  background: #22c55e;
  color: #ffffff;
  border-radius: 6px;
  text-decoration: none;
  font-size: 0.8125rem;
  font-weight: 600;
  transition: background-color 0.15s;
}

.btn-download:hover {
  background: #16a34a;
}

.result-expiry {
  font-size: 0.75rem;
  color: #94a3b8;
}

.export-error {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.75rem 1rem;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 8px;
  color: #dc2626;
  font-size: 0.8125rem;
}

.btn-cancel,
.btn-export {
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

.btn-export {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  background: #3b82f6;
  border: none;
  color: #ffffff;
  margin-left: 0.5rem;
}

.btn-export:hover {
  background: #2563eb;
}

.btn-export:disabled {
  background: #94a3b8;
  cursor: not-allowed;
}
</style>
