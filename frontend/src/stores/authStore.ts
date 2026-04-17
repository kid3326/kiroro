import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import apiClient from '@/services/apiService'
import type { AxiosError } from 'axios'

export interface User {
  id: string
  username: string
  email: string
  role: 'CEO' | 'EXECUTIVE' | 'MARKETING' | 'FINANCE' | 'PRODUCT'
  assignedBrand: string | null
}

export interface LoginError {
  message: string
  locked: boolean
  lockedUntil?: string
}

export const useAuthStore = defineStore('auth', () => {
  const user = ref<User | null>(null)
  const isAuthenticated = ref(false)
  const sessionToken = ref<string | null>(null)
  const loading = ref(false)
  const loginError = ref<LoginError | null>(null)

  const userRole = computed(() => user.value?.role ?? null)
  const assignedBrand = computed(() => user.value?.assignedBrand ?? null)

  function setUser(userData: User, token: string) {
    user.value = userData
    sessionToken.value = token
    isAuthenticated.value = true
    loginError.value = null
    localStorage.setItem('sessionToken', token)
  }

  function clearUser() {
    user.value = null
    sessionToken.value = null
    isAuthenticated.value = false
    loginError.value = null
    localStorage.removeItem('sessionToken')
  }

  async function login(username: string, password: string): Promise<boolean> {
    loading.value = true
    loginError.value = null
    try {
      const response = await apiClient.post('/auth/login', { username, password })
      const { user: userData, sessionToken: token } = response.data
      setUser(userData, token)
      return true
    } catch (err) {
      const axiosError = err as AxiosError<{ message?: string; locked?: boolean; lockedUntil?: string }>
      const data = axiosError.response?.data
      if (axiosError.response?.status === 423 || data?.locked) {
        loginError.value = {
          message: data?.message ?? '계정이 잠겼습니다. 잠시 후 다시 시도해주세요.',
          locked: true,
          lockedUntil: data?.lockedUntil,
        }
      } else if (axiosError.response?.status === 401) {
        loginError.value = {
          message: data?.message ?? '사용자명 또는 비밀번호가 올바르지 않습니다.',
          locked: false,
        }
      } else {
        loginError.value = {
          message: '로그인 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.',
          locked: false,
        }
      }
      return false
    } finally {
      loading.value = false
    }
  }

  async function logout(): Promise<void> {
    try {
      await apiClient.post('/auth/logout')
    } catch {
      // 로그아웃 실패해도 로컬 세션은 정리
    } finally {
      clearUser()
    }
  }

  async function checkSession(): Promise<boolean> {
    const storedToken = localStorage.getItem('sessionToken')
    if (!storedToken) {
      clearUser()
      return false
    }
    sessionToken.value = storedToken
    try {
      const response = await apiClient.get('/auth/session')
      const { user: userData, sessionToken: token } = response.data
      setUser(userData, token ?? storedToken)
      return true
    } catch {
      clearUser()
      return false
    }
  }

  return {
    user,
    isAuthenticated,
    sessionToken,
    loading,
    loginError,
    userRole,
    assignedBrand,
    setUser,
    clearUser,
    login,
    logout,
    checkSession,
  }
})
