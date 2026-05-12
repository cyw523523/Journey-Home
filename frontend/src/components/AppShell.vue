<template>
  <div class="app-shell">
    <header class="topbar">
      <RouterLink class="brand" to="/">
        <span class="brand-mark"><HeartHandshake :size="22" /></span>
        <span>
          <strong>归途</strong>
          <small>救助领养管理系统</small>
        </span>
      </RouterLink>

      <nav class="nav-links">
        <RouterLink to="/">首页</RouterLink>
        <RouterLink to="/animals">动物档案</RouterLink>
        <RouterLink to="/rescues">救助信息</RouterLink>
        <RouterLink v-if="auth.isLoggedIn.value" to="/profile">个人中心</RouterLink>
        <RouterLink v-if="auth.isAdmin.value" to="/admin">管理员后台</RouterLink>
      </nav>

      <div class="top-actions">
        <el-button v-if="!auth.isLoggedIn.value" :icon="LogIn" type="primary" @click="$router.push('/auth')">
          登录
        </el-button>
        <el-dropdown v-else>
          <button class="user-chip">
            <UserRound :size="17" />
            <span>{{ auth.state.user?.nickname || auth.state.user?.account }}</span>
          </button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="$router.push('/profile')">个人中心</el-dropdown-item>
              <el-dropdown-item v-if="auth.isAdmin.value" @click="$router.push('/admin')">管理员后台</el-dropdown-item>
              <el-dropdown-item divided @click="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <main class="page-stage">
      <RouterView v-slot="{ Component }">
        <Transition name="page" mode="out-in">
          <component :is="Component" />
        </Transition>
      </RouterView>
    </main>
  </div>
</template>

<script setup>
import { RouterLink, RouterView, useRouter } from 'vue-router'
import { HeartHandshake, LogIn, UserRound } from 'lucide-vue-next'
import { useAuth } from '../stores/auth'

const auth = useAuth()
const router = useRouter()

function logout() {
  auth.logout()
  router.push('/')
}
</script>
