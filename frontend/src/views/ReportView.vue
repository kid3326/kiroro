<template>
  <div class="report-container">
    <h1 class="page-title">리포트</h1>
    <p class="page-description">자동 리포트 생성 및 스케줄링을 관리합니다.</p>

    <div class="report-sections">
      <!-- Report Templates -->
      <section class="report-section">
        <h2 class="section-title">리포트 템플릿</h2>
        <div class="template-grid">
          <div v-for="template in templates" :key="template.name" class="template-card">
            <div class="template-icon">
              <i :class="template.icon"></i>
            </div>
            <div class="template-info">
              <h3 class="template-name">{{ template.name }}</h3>
              <p class="template-desc">{{ template.description }}</p>
            </div>
            <button class="btn-schedule" @click="openScheduleForm(template.name)">
              <i class="pi pi-calendar-plus"></i> 스케줄 생성
            </button>
          </div>
        </div>
      </section>

      <!-- Schedule Creation Form -->
      <section v-if="showScheduleForm" class="report-section">
        <h2 class="section-title">스케줄 생성</h2>
        <div class="schedule-form-card">
          <div class="form-row">
            <label class="form-label">템플릿</label>
            <InputText v-model="scheduleForm.templateName" disabled class="form-input" />
          </div>
          <div class="form-row">
            <label class="form-label">빈도</label>
            <Dropdown
              v-model="scheduleForm.frequency"
              :options="frequencyOptions"
              optionLabel="label"
              optionValue="value"
              placeholder="빈도 선택"
              class="form-input"
            />
          </div>
          <div class="form-row">
            <label class="form-label">실행 시간</label>
            <Calendar
              v-model="scheduleForm.scheduledTime"
              :timeOnly="true"
              hourFormat="24"
              placeholder="시간 선택"
              class="form-input"
            />
          </div>
          <div class="form-row">
            <label class="form-label">수신자 (이메일)</label>
            <Chips
              v-model="scheduleForm.recipients"
              placeholder="이메일 입력 후 Enter"
              class="form-input"
            />
          </div>
          <div class="form-actions">
            <button class="btn-cancel" @click="showScheduleForm = false">취소</button>
            <button class="btn-submit" @click="createSchedule" :disabled="isSubmitting">
              <i v-if="isSubmitting" class="pi pi-spin pi-spinner"></i>
              {{ isSubmitting ? '생성 중...' : '스케줄 생성' }}
            </button>
          </div>
        </div>
      </section>

      <!-- Report History -->
      <section class="report-section">
        <h2 class="section-title">리포트 이력</h2>
        <DataTable
          :value="reportHistory"
          :paginator="true"
          :rows="10"
          :loading="isLoadingHistory"
          stripedRows
          class="p-datatable-sm"
          aria-label="리포트 이력 테이블"
        >
          <Column field="templateName" header="템플릿" sortable style="min-width: 150px" />
          <Column field="generatedAt" header="생성일" sortable style="min-width: 150px">
            <template #body="{ data }">
              {{ formatDate(data.generatedAt) }}
            </template>
          </Column>
          <Column field="status" header="상태" sortable style="min-width: 100px">
            <template #body="{ data }">
              <span class="status-badge" :class="statusClass(data.status)">
                {{ statusLabel(data.status) }}
              </span>
            </template>
          </Column>
          <Column field="fileSizeBytes" header="파일 크기" style="min-width: 100px">
            <template #body="{ data }">
              {{ formatFileSize(data.fileSizeBytes) }}
            </template>
          </Column>
          <Column header="다운로드" style="min-width: 100px">
            <template #body="{ data }">
              <a
                v-if="data.fileUrl"
                :href="data.fileUrl"
                class="btn-download-link"
                target="_blank"
                rel="noopener"
              >
                <i class="pi pi-download"></i> 다운로드
              </a>
              <span v-else class="no-download">-</span>
            </template>
          </Column>
        </DataTable>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import DataTable from 'primevue/datatable'
import Column from 'primevue/column'
import InputText from 'primevue/inputtext'
import Dropdown from 'primevue/dropdown'
import Calendar from 'primevue/calendar'
import Chips from 'primevue/chips'
import apiClient from '@/services/apiService'

interface ReportTemplate {
  name: string
  description: string
  icon: string
}

interface ReportHistoryItem {
  id: number
  templateName: string
  generatedAt: string
  status: 'GENERATED' | 'SENT' | 'FAILED'
  fileSizeBytes: number
  fileUrl: string | null
}

const templates: ReportTemplate[] = [
  {
    name: 'Monthly Executive Report',
    description: '매출, 이익, EBITDA, KPI 요약을 포함한 월간 경영 리포트',
    icon: 'pi pi-chart-line',
  },
  {
    name: 'Weekly Marketing Report',
    description: '채널별 광고비, ROAS, CAC, 전환 메트릭을 포함한 주간 마케팅 리포트',
    icon: 'pi pi-megaphone',
  },
]

const frequencyOptions = [
  { label: '일별', value: 'DAILY' },
  { label: '주별', value: 'WEEKLY' },
]

const showScheduleForm = ref(false)
const isSubmitting = ref(false)
const isLoadingHistory = ref(false)
const reportHistory = ref<ReportHistoryItem[]>([])

const scheduleForm = ref({
  templateName: '',
  frequency: 'WEEKLY',
  scheduledTime: null as Date | null,
  recipients: [] as string[],
})

function openScheduleForm(templateName: string) {
  scheduleForm.value.templateName = templateName
  scheduleForm.value.frequency = 'WEEKLY'
  scheduleForm.value.scheduledTime = null
  scheduleForm.value.recipients = []
  showScheduleForm.value = true
}

async function createSchedule() {
  isSubmitting.value = true
  try {
    const time = scheduleForm.value.scheduledTime
    const timeStr = time ? `${time.getHours().toString().padStart(2, '0')}:${time.getMinutes().toString().padStart(2, '0')}` : '09:00'

    await apiClient.post('/reports/schedules', {
      templateName: scheduleForm.value.templateName,
      frequency: scheduleForm.value.frequency,
      scheduledTime: timeStr,
      recipients: scheduleForm.value.recipients,
    })
    showScheduleForm.value = false
    await fetchHistory()
  } catch {
    // Error handling
  } finally {
    isSubmitting.value = false
  }
}

async function fetchHistory() {
  isLoadingHistory.value = true
  try {
    const response = await apiClient.get('/reports/history')
    reportHistory.value = response.data ?? []
  } catch {
    reportHistory.value = []
  } finally {
    isLoadingHistory.value = false
  }
}

function formatDate(dateStr: string) {
  return new Date(dateStr).toLocaleString('ko-KR', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

function formatFileSize(bytes: number) {
  if (!bytes) return '-'
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`
}

function statusClass(status: string) {
  switch (status) {
    case 'GENERATED': return 'status-generated'
    case 'SENT': return 'status-sent'
    case 'FAILED': return 'status-failed'
    default: return ''
  }
}

function statusLabel(status: string) {
  switch (status) {
    case 'GENERATED': return '생성됨'
    case 'SENT': return '발송됨'
    case 'FAILED': return '실패'
    default: return status
  }
}

onMounted(() => {
  fetchHistory()
})
</script>

<style scoped>
.report-container {
  padding: 1.5rem;
  max-width: 1200px;
  margin: 0 auto;
}

.page-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 0.25rem;
}

.page-description {
  font-size: 0.875rem;
  color: #64748b;
  margin: 0 0 1.5rem;
}

.report-sections {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.report-section {
  background: #ffffff;
  border-radius: 12px;
  padding: 1.5rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  border: 1px solid #e2e8f0;
}

.section-title {
  font-size: 1.125rem;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 1rem;
}

.template-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 1rem;
}

.template-card {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem 1.25rem;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  transition: border-color 0.15s;
}

.template-card:hover {
  border-color: #93c5fd;
}

.template-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #eff6ff;
  border-radius: 10px;
  flex-shrink: 0;
}

.template-icon i {
  font-size: 1.25rem;
  color: #3b82f6;
}

.template-info {
  flex: 1;
  min-width: 0;
}

.template-name {
  font-size: 0.875rem;
  font-weight: 600;
  color: #0f172a;
  margin: 0 0 0.25rem;
}

.template-desc {
  font-size: 0.75rem;
  color: #94a3b8;
  margin: 0;
}

.btn-schedule {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  padding: 0.5rem 0.75rem;
  background: #3b82f6;
  color: #ffffff;
  border: none;
  border-radius: 6px;
  font-size: 0.75rem;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  transition: background-color 0.15s;
}

.btn-schedule:hover {
  background: #2563eb;
}

.schedule-form-card {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  max-width: 500px;
}

.form-row {
  display: flex;
  flex-direction: column;
  gap: 0.375rem;
}

.form-label {
  font-size: 0.8125rem;
  font-weight: 600;
  color: #334155;
}

.form-input {
  width: 100%;
}

.form-actions {
  display: flex;
  gap: 0.5rem;
  justify-content: flex-end;
  margin-top: 0.5rem;
}

.btn-cancel,
.btn-submit {
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

.btn-submit {
  display: flex;
  align-items: center;
  gap: 0.375rem;
  background: #3b82f6;
  border: none;
  color: #ffffff;
}

.btn-submit:hover { background: #2563eb; }
.btn-submit:disabled { background: #94a3b8; cursor: not-allowed; }

.status-badge {
  display: inline-block;
  padding: 0.125rem 0.5rem;
  border-radius: 9999px;
  font-size: 0.75rem;
  font-weight: 600;
}

.status-generated { background: #dbeafe; color: #1d4ed8; }
.status-sent { background: #dcfce7; color: #166534; }
.status-failed { background: #fee2e2; color: #991b1b; }

.btn-download-link {
  display: inline-flex;
  align-items: center;
  gap: 0.25rem;
  color: #3b82f6;
  text-decoration: none;
  font-size: 0.8125rem;
  font-weight: 500;
}

.btn-download-link:hover {
  color: #2563eb;
}

.no-download {
  color: #94a3b8;
}
</style>
