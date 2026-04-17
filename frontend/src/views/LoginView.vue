<template>
  <div class="login-page">
    <div class="login-card">
      <div class="login-header">
        <i class="pi pi-chart-bar login-icon"></i>
        <h1 class="login-title">Retail P&amp;L Dashboard</h1>
        <p class="login-subtitle">로그인하여 대시보드에 접속하세요.</p>
      </div>

      <form class="login-form" @submit.prevent="handleLogin" aria-label="로그인 폼">
        <div class="field">
          <label for="username" class="field-label">사용자명</label>
          <span class="p-input-icon-left field-input-wrapper">
            <i class="pi pi-user" />
            <InputText
              id="username"
              v-model="username"
              placeholder="사용자명을 입력하세요"
              class="field-input"
              :disabled="authStore.loading"
              autocomplete="username"
              aria-required="true"
            />
          </span>
        </div>

        <div class="field">
          <label for="password" class="field-label">비밀번호</label>
          <Password
            id="password"
            v-model="password"
            placeholder="비밀번호를 입력하세요"
            class="field-input"
            :feedback="false"
            :toggleMask="true"
            :disabled="authStore.loading"
            inputClass="field-input-inner"
            autocomplete="current-password"
            aria-required="true"
          />
        </div>

        <div v-if="authStore.loginError" class="login-error" role="alert">
          <Message
            :severity="authStore.loginError.locked ? 'warn' : 'error'"
            :closable="false"
          >
            <template #default>
              <div class="error-content">
                <span>{{ authStore.loginError.message }}</span>
                <span v-if="authStore.loginError.locked && authStore.loginError.lockedUntil" class="lock-info">
                  잠금 해제 시간: {{ formatLockedUntil(authStore.loginError.lockedUntil) }}
                </span>
              </div>
            </template>
          </Message>
        </div>

        <Button
          type="submit"
          label="로그인"
          icon="pi pi-sign-in"
          class="login-button"
          :loading="authStore.loading"
          :disabled="!isFormValid || authStore.loading"
        />
      </form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/authStore'
import InputText from 'primevue/inputtext'
import Password from 'primevue/password'
import Button from 'primevue/button'
import Message from 'primevue/message'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const username = ref('')
const password = ref('')

const isFormValid = computed(() => username.value.trim() !== '' && password.value !== '')

function formatLockedUntil(lockedUntil: string): string {
  try {
    return new Date(lockedUntil).toLocaleString('ko-KR')
  } catch {
    return lockedUntil
  }
}

async function handleLogin() {
  if (!isFormValid.value) return

  const success = await authStore.login(username.value.trim(), password.value)
  if (success) {
    const redirect = (route.query.redirect as string) || '/'
    router.push(redirect)
  }
}
</script>

<style scoped>
.login-page {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  background: linear-gradient(135deg, #1e293b 0%, #334155 100%);
  padding: 1rem;
}

.login-card {
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
  padding: 2.5rem;
  width: 100%;
  max-width: 420px;
}

.login-header {
  text-align: center;
  margin-bottom: 2rem;
}

.login-icon {
  font-size: 2.5rem;
  color: #3b82f6;
  margin-bottom: 0.75rem;
  display: block;
}

.login-title {
  font-size: 1.5rem;
  font-weight: 700;
  color: #1e293b;
  margin: 0 0 0.5rem 0;
}

.login-subtitle {
  font-size: 0.875rem;
  color: #64748b;
  margin: 0;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 1.25rem;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}

.field-label {
  font-size: 0.875rem;
  font-weight: 600;
  color: #334155;
}

.field-input-wrapper {
  width: 100%;
}

.field-input {
  width: 100%;
}

:deep(.field-input-inner) {
  width: 100%;
}

:deep(.p-password) {
  width: 100%;
}

:deep(.p-password .p-inputtext) {
  width: 100%;
}

.login-error {
  margin: 0;
}

.error-content {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.lock-info {
  font-size: 0.8125rem;
  opacity: 0.85;
}

.login-button {
  width: 100%;
  justify-content: center;
  padding: 0.75rem;
  font-size: 1rem;
  margin-top: 0.5rem;
}
</style>
