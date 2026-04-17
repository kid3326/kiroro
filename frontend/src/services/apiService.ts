import axios from 'axios'
import router from '@/router'
import { useAuthStore } from '@/stores/authStore'

const apiClient = axios.create({
  baseURL: '/api/v1',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Request interceptor: 세션 토큰을 요청 헤더에 포함
apiClient.interceptors.request.use(
  (config) => {
    const authStore = useAuthStore()
    if (authStore.sessionToken) {
      config.headers['X-Session-Token'] = authStore.sessionToken
    }
    return config
  },
  (error) => Promise.reject(error),
)

// Response interceptor: 401 응답 시 세션 정리 및 로그인 페이지 리다이렉트
apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      const authStore = useAuthStore()
      authStore.clearUser()
      // 이미 로그인 페이지에 있으면 리다이렉트하지 않음
      if (router.currentRoute.value.name !== 'Login') {
        router.push({ name: 'Login', query: { redirect: router.currentRoute.value.fullPath } })
      }
    }
    return Promise.reject(error)
  },
)

export default apiClient
