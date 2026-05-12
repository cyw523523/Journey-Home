import { computed, reactive } from 'vue'
import { authApi } from '../api'

const state = reactive({
  token: localStorage.getItem('guitu_token') || '',
  user: JSON.parse(localStorage.getItem('guitu_user') || 'null'),
  ready: false
})

export function useAuth() {
  const isLoggedIn = computed(() => Boolean(state.token))
  const isAdmin = computed(() => state.user?.role === 'ADMIN')

  async function login(payload) {
    const data = await authApi.login(payload)
    state.token = data.token
    state.user = data.user
    localStorage.setItem('guitu_token', data.token)
    localStorage.setItem('guitu_user', JSON.stringify(data.user))
    return data.user
  }

  async function register(payload) {
    return authApi.register(payload)
  }

  async function fetchMe() {
    if (!state.token) {
      state.ready = true
      return null
    }
    try {
      state.user = await authApi.me()
      localStorage.setItem('guitu_user', JSON.stringify(state.user))
      return state.user
    } finally {
      state.ready = true
    }
  }

  function logout() {
    state.token = ''
    state.user = null
    state.ready = true
    localStorage.removeItem('guitu_token')
    localStorage.removeItem('guitu_user')
  }

  return { state, isLoggedIn, isAdmin, login, register, fetchMe, logout }
}
