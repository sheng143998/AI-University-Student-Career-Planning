import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import * as authApi from '@/api/auth'
import { clearToken, getStoredUser, getToken, setStoredUser, setToken, type StoredUser } from '@/lib/authToken'
import { isApiSuccess } from '@/api/client'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(getToken())
  const user = ref<StoredUser | null>(getStoredUser())

  const isLoggedIn = computed(() => !!token.value)

  function applySession(t: string, u?: authApi.AuthUser | null) {
    setToken(t)
    token.value = t
    if (u) {
      const su: StoredUser = { id: String(u.id), name: u.name }
      setStoredUser(su)
      user.value = su
    }
  }

  function clearSession() {
    clearToken()
    token.value = null
    user.value = null
  }

  async function login(username: string, password: string) {
    const r = await authApi.login({ username, password })
    if (isApiSuccess(r.code) && r.data?.token) {
      const d = r.data
      applySession(d.token, d.user ?? null)
    }
    return r
  }

  async function register(username: string, password: string, userimage?: string) {
    const r = await authApi.register({
      username,
      password,
      ...(userimage ? { userImage: userimage } : {}),
    })
    return r
  }

  async function logout() {
    try {
      await authApi.logout()
    } finally {
      clearSession()
    }
  }

  return {
    token,
    user,
    isLoggedIn,
    login,
    register,
    logout,
    clearSession,
  }
})
