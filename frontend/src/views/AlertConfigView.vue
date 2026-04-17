<template>
  <div class="alert-config-container">
    <h1 class="page-title">알림 설정</h1>
    <p class="page-description">알림 임계값 및 알림 채널을 설정합니다.</p>

    <div class="alert-sections">
      <!-- Alert Threshold Configuration -->
      <section class="alert-section">
        <h2 class="section-title">알림 임계값 설정</h2>
        <div class="config-grid">
          <div v-for="config in alertConfigs" :key="config.alertType" class="config-card">
            <div class="config-header">
              <div class="config-icon" :class="typeIconClass(config.alertType)">
                <i :class="typeIcon(config.alertType)"></i>
              </div>
              <div class="config-info">
                <h3 class="config-name">{{ typeLabel(config.alertType) }}</h3>
                <p class="config-desc">{{ typeDescription(config.alertType) }}</p>
              </div>
            </div>

            <div class="config-body">
              <div class="config-field">
                <label class="field-label">임계값</label>
                <div class="field-input-group">
                  <InputNumber
                    v-model="config.thresholdValue"
                    :min="0"
                    :max="100"
                    :suffix="thresholdSuffix(config.alertType)"
                    class="field-input"
                  />
                </div>
              </div>

              <div class="config-field">
                <label class="field-label">심각도</label>
                <Dropdown
                  v-model="config.severity"
                  :options="severityOptions"
                  optionLabel="label"
                  optionValue="value"
                  class="field-input"
                />
              </div>

              <div class="config-field">
                <label class="field-label">알림 채널</label>
                <div class="channel-toggles">
                  <div class="channel-toggle">
                    <InputSwitch v-model="config.emailEnabled" />
                    <span class="channel-label"><i class="pi pi-envelope"></i> 이메일</span>
                  </div>
                  <div class="channel-toggle">
                    <InputSwitch v-model="config.smsEnabled" />
                    <span class="channel-label"><i class="pi pi-mobile"></i> SMS</span>
                  </div>
                  <div class="channel-toggle">
                    <InputSwitch v-model="config.pushEnabled" />
                    <span class="channel-label"><i class="pi pi-bell"></i> 푸시</span>
                  </div>
                </div>
              </div>

              <button class="btn-save-config" @click="saveConfig(config)" :disabled="isSaving">
                <i v-if="isSaving" class="pi pi-spin pi-spinner"></i>
                설정 저장
              </button>
            </div>
          </div>
        </div>
      </section>

      <!-- Active Alerts -->
      <section class="alert-section">
        <h2 class="section-title">
          활성 알림
          <span class="alert-count" v-if="alertStore.activeAlerts.length > 0">
            {{ alertStore.activeAlerts.length }}
          </span>
        </h2>

        <div v-if="alertStore.isLoading" class="loading-state">
          <i class="pi pi-spin pi-spinner"></i> 알림 로딩 중...
        </div>

        <div v-else-if="alertStore.activeAlerts.length === 0" class="empty-state">
          <i class="pi pi-check-circle empty-icon"></i>
          <p>활성 알림이 없습니다.</p>
        </div>

        <div v-else class="alert-list">
          <div
            v-for="alert in alertStore.alerts"
            :key="alert.id"
            class="alert-item"
            :class="[`severity-${alert.severity.toLowerCase()}`, { acknowledged: alert.isAcknowledged }]"
          >
            <div class="alert-item-icon">
              <i :class="severityIcon(alert.severity)"></i>
            </div>
            <div class="alert-item-body">
              <div class="alert-item-header">
                <span class="alert-item-title">{{ alert.title }}</span>
                <span class="alert-item-severity" :class="`badge-${alert.severity.toLowerCase()}`">
                  {{ severityLabel(alert.severity) }}
                </span>
              </div>
              <p class="alert-item-message">{{ alert.message }}</p>
              <span class="alert-item-time">
                <i class="pi pi-clock"></i>
                {{ formatDate(alert.triggeredAt) }}
              </span>
            </div>
            <button
              v-if="!alert.isAcknowledged"
              class="btn-acknowledge"
              @click="alertStore.acknowledgeAlert(alert.id)"
            >
              확인
            </button>
            <span v-else class="acknowledged-label">
              <i class="pi pi-check"></i> 확인됨
            </span>
          </div>
        </div>
      </section>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import InputNumber from 'primevue/inputnumber'
import InputSwitch from 'primevue/inputswitch'
import Dropdown from 'primevue/dropdown'
import { useAlertStore, type AlertConfig } from '@/stores/alertStore'

const alertStore = useAlertStore()
const isSaving = ref(false)

const severityOptions = [
  { label: '심각', value: 'CRITICAL' },
  { label: '중요', value: 'IMPORTANT' },
  { label: '정보', value: 'INFORMATIONAL' },
]

const alertConfigs = ref<AlertConfig[]>([
  { alertType: 'REVENUE', thresholdValue: 90, severity: 'CRITICAL', emailEnabled: true, smsEnabled: true, pushEnabled: true },
  { alertType: 'AD_SPEND', thresholdValue: 95, severity: 'IMPORTANT', emailEnabled: true, smsEnabled: false, pushEnabled: true },
  { alertType: 'ANOMALY', thresholdValue: 20, severity: 'IMPORTANT', emailEnabled: true, smsEnabled: false, pushEnabled: true },
  { alertType: 'INVENTORY', thresholdValue: 0, severity: 'CRITICAL', emailEnabled: true, smsEnabled: true, pushEnabled: true },
])

function typeLabel(type: string) {
  const map: Record<string, string> = {
    REVENUE: '매출 임계값',
    AD_SPEND: '광고비 예산',
    ANOMALY: '판매량 이상 감지',
    INVENTORY: '재고 부족',
  }
  return map[type] ?? type
}

function typeDescription(type: string) {
  const map: Record<string, string> = {
    REVENUE: '목표 대비 매출이 설정값 미만일 때 알림',
    AD_SPEND: '예산 대비 광고비가 설정값 초과 시 알림',
    ANOMALY: '7일 이동평균 대비 설정값(%) 이탈 시 알림',
    INVENTORY: '재주문점 이하로 재고가 감소 시 알림',
  }
  return map[type] ?? ''
}

function typeIcon(type: string) {
  const map: Record<string, string> = {
    REVENUE: 'pi pi-chart-line',
    AD_SPEND: 'pi pi-wallet',
    ANOMALY: 'pi pi-exclamation-triangle',
    INVENTORY: 'pi pi-box',
  }
  return map[type] ?? 'pi pi-bell'
}

function typeIconClass(type: string) {
  const map: Record<string, string> = {
    REVENUE: 'icon-revenue',
    AD_SPEND: 'icon-adspend',
    ANOMALY: 'icon-anomaly',
    INVENTORY: 'icon-inventory',
  }
  return map[type] ?? ''
}

function thresholdSuffix(type: string) {
  return type === 'INVENTORY' ? '' : '%'
}

function severityIcon(severity: string) {
  switch (severity) {
    case 'CRITICAL': return 'pi pi-exclamation-triangle'
    case 'IMPORTANT': return 'pi pi-exclamation-circle'
    default: return 'pi pi-info-circle'
  }
}

function severityLabel(severity: string) {
  switch (severity) {
    case 'CRITICAL': return '심각'
    case 'IMPORTANT': return '중요'
    default: return '정보'
  }
}

function formatDate(dateStr: string) {
  return new Date(dateStr).toLocaleString('ko-KR', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
  })
}

async function saveConfig(config: AlertConfig) {
  isSaving.value = true
  try {
    await alertStore.updateAlertConfig(config)
  } finally {
    isSaving.value = false
  }
}

onMounted(async () => {
  await Promise.all([
    alertStore.fetchAlerts(),
    alertStore.fetchAlertConfigs(),
  ])
  // Merge fetched configs with defaults
  if (alertStore.alertConfigs.length > 0) {
    alertConfigs.value = alertConfigs.value.map((defaultConfig) => {
      const fetched = alertStore.alertConfigs.find((c) => c.alertType === defaultConfig.alertType)
      return fetched ? { ...defaultConfig, ...fetched } : defaultConfig
    })
  }
})
</script>

<style scoped>
.alert-config-container {
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

.alert-sections {
  display: flex;
  flex-direction: column;
  gap: 1.5rem;
}

.alert-section {
  background: #ffffff;
  border-radius: 12px;
  padding: 1.5rem;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  border: 1px solid #e2e8f0;
}

.section-title {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  font-size: 1.125rem;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 1rem;
}

.alert-count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 1.5rem;
  height: 1.5rem;
  padding: 0 0.375rem;
  background: #ef4444;
  color: #ffffff;
  border-radius: 9999px;
  font-size: 0.75rem;
  font-weight: 700;
}

.config-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 1rem;
}

.config-card {
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  overflow: hidden;
}

.config-header {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 1rem;
  background: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
}

.config-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  flex-shrink: 0;
}

.config-icon i { font-size: 1.125rem; }

.icon-revenue { background: #dbeafe; color: #2563eb; }
.icon-adspend { background: #fef3c7; color: #d97706; }
.icon-anomaly { background: #fee2e2; color: #dc2626; }
.icon-inventory { background: #dcfce7; color: #16a34a; }

.config-info { flex: 1; min-width: 0; }

.config-name {
  font-size: 0.875rem;
  font-weight: 600;
  color: #0f172a;
  margin: 0 0 0.125rem;
}

.config-desc {
  font-size: 0.75rem;
  color: #94a3b8;
  margin: 0;
}

.config-body {
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.config-field {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.field-label {
  font-size: 0.75rem;
  font-weight: 600;
  color: #64748b;
}

.field-input {
  width: 100%;
}

.channel-toggles {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.channel-toggle {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.channel-label {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.8125rem;
  color: #334155;
}

.btn-save-config {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.375rem;
  padding: 0.5rem;
  background: #3b82f6;
  color: #ffffff;
  border: none;
  border-radius: 6px;
  font-size: 0.8125rem;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.15s;
  margin-top: 0.25rem;
}

.btn-save-config:hover { background: #2563eb; }
.btn-save-config:disabled { background: #94a3b8; cursor: not-allowed; }

/* Active Alerts */
.loading-state {
  text-align: center;
  padding: 2rem;
  color: #64748b;
  font-size: 0.875rem;
}

.empty-state {
  text-align: center;
  padding: 2rem;
  color: #94a3b8;
}

.empty-icon {
  font-size: 2.5rem;
  color: #22c55e;
  display: block;
  margin-bottom: 0.5rem;
}

.alert-list {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.alert-item {
  display: flex;
  align-items: flex-start;
  gap: 0.75rem;
  padding: 0.875rem 1rem;
  border-radius: 8px;
  border-left: 4px solid transparent;
}

.alert-item.severity-critical {
  background: #fef2f2;
  border-left-color: #ef4444;
}

.alert-item.severity-important {
  background: #fffbeb;
  border-left-color: #f59e0b;
}

.alert-item.severity-informational {
  background: #eff6ff;
  border-left-color: #3b82f6;
}

.alert-item.acknowledged {
  opacity: 0.6;
}

.alert-item-icon {
  flex-shrink: 0;
  margin-top: 0.125rem;
}

.severity-critical .alert-item-icon { color: #ef4444; }
.severity-important .alert-item-icon { color: #f59e0b; }
.severity-informational .alert-item-icon { color: #3b82f6; }

.alert-item-body {
  flex: 1;
  min-width: 0;
}

.alert-item-header {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  margin-bottom: 0.25rem;
}

.alert-item-title {
  font-size: 0.875rem;
  font-weight: 600;
  color: #0f172a;
}

.alert-item-severity {
  font-size: 0.6875rem;
  font-weight: 600;
  padding: 0.0625rem 0.375rem;
  border-radius: 9999px;
}

.badge-critical { background: #fee2e2; color: #991b1b; }
.badge-important { background: #fef3c7; color: #92400e; }
.badge-informational { background: #dbeafe; color: #1e40af; }

.alert-item-message {
  font-size: 0.8125rem;
  color: #475569;
  margin: 0 0 0.25rem;
}

.alert-item-time {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.75rem;
  color: #94a3b8;
}

.btn-acknowledge {
  padding: 0.375rem 0.75rem;
  background: #3b82f6;
  color: #ffffff;
  border: none;
  border-radius: 6px;
  font-size: 0.75rem;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  flex-shrink: 0;
  transition: background-color 0.15s;
}

.btn-acknowledge:hover { background: #2563eb; }

.acknowledged-label {
  display: flex;
  align-items: center;
  gap: 0.25rem;
  font-size: 0.75rem;
  color: #22c55e;
  font-weight: 500;
  white-space: nowrap;
  flex-shrink: 0;
}
</style>
