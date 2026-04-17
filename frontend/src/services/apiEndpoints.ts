/**
 * 중앙 집중식 API 엔드포인트 정의.
 * 모든 백엔드 API 엔드포인트를 하나의 파일에서 관리합니다.
 *
 * 사용법:
 *   import apiClient from './apiService'
 *   import { API } from './apiEndpoints'
 *   const response = await apiClient.get(API.dashboard.summary, { params })
 *
 * Requirements: 2.1, 6.2, 7.3, 10.1, 12.7
 */
import apiClient from './apiService'
import type { AxiosResponse } from 'axios'

// ============================================
// API 경로 상수
// ============================================
export const API = {
  auth: {
    login: '/auth/login',
    logout: '/auth/logout',
    session: '/auth/session',
  },
  dashboard: {
    summary: '/dashboard/summary',
    revenue: '/dashboard/revenue',
    costs: '/dashboard/costs',
    advertising: '/dashboard/advertising',
    products: '/dashboard/products',
    inventory: '/dashboard/inventory',
    comparison: '/dashboard/comparison',
  },
  filters: {
    list: '/filters',
    create: '/filters',
    get: (id: number) => `/filters/${id}`,
    delete: (id: number) => `/filters/${id}`,
    searchProducts: '/search/products',
  },
  exports: {
    excel: '/export/excel',
    pdf: '/export/pdf',
    ppt: '/export/ppt',
    download: (id: string) => `/export/${id}/download`,
  },
  reports: {
    templates: '/reports/templates',
    schedules: '/reports/schedules',
    history: '/reports/history',
    download: (id: string) => `/reports/${id}/download`,
  },
  alerts: {
    list: '/alerts',
    config: '/alerts/config',
    acknowledge: (id: number) => `/alerts/${id}/acknowledge`,
  },
  config: {
    get: '/config',
    update: '/config',
    validate: '/config/validate',
  },
  audit: {
    logs: '/audit/logs',
    export: '/audit/logs/export',
  },
  compliance: {
    consent: '/compliance/consent',
    retentionStatus: '/compliance/retention-status',
    auditExport: '/compliance/audit-export',
  },
} as const

// ============================================
// 공통 타입 정의
// ============================================
export interface DateRangeParams {
  from: string
  to: string
}

export interface PaginationParams {
  page?: number
  size?: number
}

export interface DashboardFilterParams extends DateRangeParams, PaginationParams {
  category?: string
  brand?: string
  sku?: string
  channel?: string
  granularity?: 'daily' | 'weekly' | 'monthly' | 'quarterly' | 'yearly'
}

// ============================================
// 인증 API
// ============================================
export const authApi = {
  login(username: string, password: string): Promise<AxiosResponse> {
    return apiClient.post(API.auth.login, { username, password })
  },

  logout(): Promise<AxiosResponse> {
    return apiClient.post(API.auth.logout)
  },

  checkSession(): Promise<AxiosResponse> {
    return apiClient.get(API.auth.session)
  },
}

// ============================================
// 대시보드 API
// ============================================
export const dashboardApi = {
  getSummary(params: DashboardFilterParams): Promise<AxiosResponse> {
    return apiClient.get(API.dashboard.summary, { params })
  },

  getRevenue(params: DashboardFilterParams): Promise<AxiosResponse> {
    return apiClient.get(API.dashboard.revenue, { params })
  },

  getCosts(params: DashboardFilterParams): Promise<AxiosResponse> {
    return apiClient.get(API.dashboard.costs, { params })
  },

  getAdvertising(params: DashboardFilterParams): Promise<AxiosResponse> {
    return apiClient.get(API.dashboard.advertising, { params })
  },

  getProducts(params: DashboardFilterParams): Promise<AxiosResponse> {
    return apiClient.get(API.dashboard.products, { params })
  },

  getInventory(params: DashboardFilterParams): Promise<AxiosResponse> {
    return apiClient.get(API.dashboard.inventory, { params })
  },

  getComparison(params: DashboardFilterParams & { comparisonType?: 'yoy' | 'mom' | 'budget' }): Promise<AxiosResponse> {
    return apiClient.get(API.dashboard.comparison, { params })
  },
}

// ============================================
// 필터 API
// ============================================
export interface SavedFilterPayload {
  name: string
  filterCriteria: Record<string, unknown>
}

export const filterApi = {
  getFilters(): Promise<AxiosResponse> {
    return apiClient.get(API.filters.list)
  },

  createFilter(payload: SavedFilterPayload): Promise<AxiosResponse> {
    return apiClient.post(API.filters.create, payload)
  },

  getFilter(id: number): Promise<AxiosResponse> {
    return apiClient.get(API.filters.get(id))
  },

  deleteFilter(id: number): Promise<AxiosResponse> {
    return apiClient.delete(API.filters.delete(id))
  },

  searchProducts(query: string): Promise<AxiosResponse> {
    return apiClient.get(API.filters.searchProducts, { params: { q: query } })
  },
}

// ============================================
// 내보내기 API
// ============================================
export interface ExportPayload {
  filters: Record<string, unknown>
  charts?: Array<{ chartId: string; imageData: string }>
}

export const exportApi = {
  exportExcel(payload: ExportPayload): Promise<AxiosResponse> {
    return apiClient.post(API.exports.excel, payload)
  },

  exportPdf(payload: ExportPayload): Promise<AxiosResponse> {
    return apiClient.post(API.exports.pdf, payload)
  },

  exportPpt(payload: ExportPayload): Promise<AxiosResponse> {
    return apiClient.post(API.exports.ppt, payload)
  },

  downloadExport(id: string): Promise<AxiosResponse> {
    return apiClient.get(API.exports.download(id), { responseType: 'blob' })
  },
}

// ============================================
// 리포트 API
// ============================================
export interface ReportSchedulePayload {
  templateName: string
  frequency: 'DAILY' | 'WEEKLY'
  scheduledTime: string
  recipients: Array<{ email?: string; department?: string; role?: string }>
}

export const reportApi = {
  getTemplates(): Promise<AxiosResponse> {
    return apiClient.get(API.reports.templates)
  },

  createSchedule(payload: ReportSchedulePayload): Promise<AxiosResponse> {
    return apiClient.post(API.reports.schedules, payload)
  },

  getHistory(params?: PaginationParams): Promise<AxiosResponse> {
    return apiClient.get(API.reports.history, { params })
  },

  downloadReport(id: string): Promise<AxiosResponse> {
    return apiClient.get(API.reports.download(id), { responseType: 'blob' })
  },
}

// ============================================
// 알림 API
// ============================================
export interface AlertConfigPayload {
  alertType: 'REVENUE' | 'AD_SPEND' | 'ANOMALY' | 'INVENTORY'
  thresholdValue: number
  severity: 'CRITICAL' | 'IMPORTANT' | 'INFORMATIONAL'
  emailEnabled: boolean
  smsEnabled: boolean
  pushEnabled: boolean
}

export const alertApi = {
  getAlerts(): Promise<AxiosResponse> {
    return apiClient.get(API.alerts.list)
  },

  updateConfig(payload: AlertConfigPayload): Promise<AxiosResponse> {
    return apiClient.put(API.alerts.config, payload)
  },

  acknowledgeAlert(id: number): Promise<AxiosResponse> {
    return apiClient.post(API.alerts.acknowledge(id))
  },
}

// ============================================
// 설정 API
// ============================================
export const configApi = {
  getConfig(): Promise<AxiosResponse> {
    return apiClient.get(API.config.get)
  },

  updateConfig(payload: Record<string, unknown>): Promise<AxiosResponse> {
    return apiClient.put(API.config.update, payload)
  },

  validateConfig(payload: Record<string, unknown>): Promise<AxiosResponse> {
    return apiClient.post(API.config.validate, payload)
  },
}

// ============================================
// 감사 로그 API
// ============================================
export const auditApi = {
  getLogs(params: DateRangeParams & PaginationParams): Promise<AxiosResponse> {
    return apiClient.get(API.audit.logs, { params })
  },

  exportLogs(params: DateRangeParams): Promise<AxiosResponse> {
    return apiClient.get(API.audit.export, { params, responseType: 'blob' })
  },
}

// ============================================
// 컴플라이언스 API
// ============================================
export interface ConsentPayload {
  consentType: string
  consented: boolean
}

export const complianceApi = {
  recordConsent(payload: ConsentPayload): Promise<AxiosResponse> {
    return apiClient.post(API.compliance.consent, payload)
  },

  getConsents(): Promise<AxiosResponse> {
    return apiClient.get(API.compliance.consent)
  },

  getRetentionStatus(): Promise<AxiosResponse> {
    return apiClient.get(API.compliance.retentionStatus)
  },

  exportAuditLogs(params: DateRangeParams): Promise<AxiosResponse> {
    return apiClient.get(API.compliance.auditExport, { params, responseType: 'blob' })
  },
}
