import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import apiClient from '@/services/apiService'

export interface Alert {
  id: number
  severity: 'CRITICAL' | 'IMPORTANT' | 'INFORMATIONAL'
  title: string
  message: string
  isAcknowledged: boolean
  triggeredAt: string
  acknowledgedAt?: string
}

export interface AlertConfig {
  id?: number
  alertType: 'REVENUE' | 'AD_SPEND' | 'ANOMALY' | 'INVENTORY'
  thresholdValue: number
  severity: 'CRITICAL' | 'IMPORTANT' | 'INFORMATIONAL'
  emailEnabled: boolean
  smsEnabled: boolean
  pushEnabled: boolean
}

export const useAlertStore = defineStore('alert', () => {
  const alerts = ref<Alert[]>([])
  const alertConfigs = ref<AlertConfig[]>([])
  const isLoading = ref(false)
  const isLoadingConfigs = ref(false)
  const error = ref<string | null>(null)

  const activeAlerts = computed(() => alerts.value.filter((a) => !a.isAcknowledged))
  const criticalAlerts = computed(() => activeAlerts.value.filter((a) => a.severity === 'CRITICAL'))
  const importantAlerts = computed(() => activeAlerts.value.filter((a) => a.severity === 'IMPORTANT'))
  const informationalAlerts = computed(() => activeAlerts.value.filter((a) => a.severity === 'INFORMATIONAL'))

  async function fetchAlerts() {
    isLoading.value = true
    error.value = null
    try {
      const response = await apiClient.get('/alerts')
      alerts.value = response.data ?? []
    } catch (err: unknown) {
      error.value = err instanceof Error ? err.message : '알림을 불러오는 중 오류가 발생했습니다.'
    } finally {
      isLoading.value = false
    }
  }

  async function acknowledgeAlert(id: number) {
    try {
      await apiClient.post(`/alerts/${id}/acknowledge`)
      const alert = alerts.value.find((a) => a.id === id)
      if (alert) {
        alert.isAcknowledged = true
        alert.acknowledgedAt = new Date().toISOString()
      }
    } catch (err: unknown) {
      error.value = err instanceof Error ? err.message : '알림 확인 중 오류가 발생했습니다.'
    }
  }

  async function fetchAlertConfigs() {
    isLoadingConfigs.value = true
    try {
      const response = await apiClient.get('/alerts/config')
      alertConfigs.value = response.data ?? []
    } catch {
      alertConfigs.value = []
    } finally {
      isLoadingConfigs.value = false
    }
  }

  async function updateAlertConfig(config: AlertConfig) {
    try {
      const response = await apiClient.put('/alerts/config', config)
      const idx = alertConfigs.value.findIndex((c) => c.alertType === config.alertType)
      if (idx >= 0) {
        alertConfigs.value[idx] = response.data
      } else {
        alertConfigs.value.push(response.data)
      }
    } catch (err: unknown) {
      error.value = err instanceof Error ? err.message : '알림 설정 변경 중 오류가 발생했습니다.'
      throw err
    }
  }

  return {
    alerts,
    alertConfigs,
    isLoading,
    isLoadingConfigs,
    error,
    activeAlerts,
    criticalAlerts,
    importantAlerts,
    informationalAlerts,
    fetchAlerts,
    acknowledgeAlert,
    fetchAlertConfigs,
    updateAlertConfig,
  }
})
