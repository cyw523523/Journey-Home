<template>
  <div class="app-shell">
    <header class="topbar">
      <RouterLink class="brand" to="/">
        <span class="brand-mark"><HeartHandshake :size="22" /></span>
        <span>
          <strong>{{ $t('nav.brand') }}</strong>
          <small>{{ $t('nav.brandSub') }}</small>
        </span>
      </RouterLink>

      <nav class="nav-links">
        <RouterLink to="/">{{ $t('nav.home') }}</RouterLink>
        <RouterLink to="/community">{{ $t('nav.community') }}</RouterLink>
        <RouterLink to="/notices">{{ $t('nav.notices') }}</RouterLink>
        <RouterLink to="/animals">{{ $t('nav.animals') }}</RouterLink>
        <RouterLink to="/rescues">{{ $t('nav.rescues') }}</RouterLink>
        <RouterLink v-if="auth.isLoggedIn.value" to="/profile">{{ $t('nav.profile') }}</RouterLink>
        <RouterLink v-if="auth.isAdmin.value" to="/admin">{{ $t('nav.admin') }}</RouterLink>
      </nav>

      <div class="top-actions">
        <el-dropdown @command="changeLanguage">
          <button class="lang-switch">
            <Globe :size="17" />
            <span>{{ currentLanguageLabel }}</span>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="zh">{{ $t('common.chinese') }}</el-dropdown-item>
              <el-dropdown-item command="en">{{ $t('common.english') }}</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <el-button v-if="!auth.isLoggedIn.value" :icon="LogIn" type="primary" @click="$router.push('/auth')">
          {{ $t('nav.login') }}
        </el-button>
        <el-dropdown v-else>
          <button class="user-chip">
            <UserRound :size="17" />
            <span>{{ auth.state.user?.nickname || auth.state.user?.account }}</span>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="$router.push('/profile')">{{ $t('nav.profile') }}</el-dropdown-item>
              <el-dropdown-item v-if="auth.isAdmin.value" @click="$router.push('/admin')">{{ $t('nav.admin') }}</el-dropdown-item>
              <el-dropdown-item divided @click="logout">{{ $t('nav.logout') }}</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <main class="page-stage">
      <RouterView />
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { Globe, HeartHandshake, LogIn, UserRound } from 'lucide-vue-next'
import { useAuth } from '../stores/auth'

const { locale, t } = useI18n()
const auth = useAuth()
const router = useRouter()

const currentLanguageLabel = computed(() => {
  return locale.value === 'zh' ? t('common.chinese') : t('common.english')
})

function changeLanguage(lang) {
  locale.value = lang
  localStorage.setItem('language', lang)
}

function logout() {
  auth.logout()
  router.push('/')
}
</script>
