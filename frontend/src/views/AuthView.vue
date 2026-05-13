<template>
  <section class="view-narrow page auth-layout">
    <div class="auth-art surface">
      <p class="eyebrow"><ShieldCheck :size="16" /> {{ $t('auth.loginSubtitle') }}</p>
      <h1>{{ $t('auth.loginTitle') }}</h1>
    </div>
    <div class="auth-card surface">
      <el-tabs v-model="mode" stretch>
        <el-tab-pane :label="$t('auth.accountLogin')" name="login">
          <el-form ref="loginRef" :model="loginForm" :rules="loginRules" label-position="top">
            <el-form-item :label="$t('auth.account')" prop="account">
              <el-input v-model="loginForm.account" size="large" :placeholder="$t('auth.accountPlaceholder')" />
            </el-form-item>
            <el-form-item :label="$t('auth.password')" prop="password">
              <el-input v-model="loginForm.password" size="large" type="password" show-password :placeholder="$t('auth.passwordPlaceholder')" />
            </el-form-item>
            <el-button :loading="loading" :icon="LogIn" type="primary" size="large" style="width: 100%" @click="submitLogin">
              {{ $t('auth.loginBtn') }}
            </el-button>
          </el-form>
        </el-tab-pane>
        <el-tab-pane :label="$t('auth.userRegister')" name="register">
          <el-form ref="registerRef" :model="registerForm" :rules="registerRules" label-position="top">
            <el-form-item :label="$t('auth.nickname')" prop="nickname">
              <el-input v-model="registerForm.nickname" size="large" />
            </el-form-item>
            <el-form-item :label="$t('auth.account')" prop="account">
              <el-input v-model="registerForm.account" size="large" />
            </el-form-item>
            <el-form-item :label="$t('auth.phone')" prop="phone">
              <el-input v-model="registerForm.phone" size="large" />
            </el-form-item>
            <el-form-item :label="$t('auth.password')" prop="password">
              <el-input v-model="registerForm.password" size="large" type="password" show-password />
            </el-form-item>
            <el-form-item :label="$t('auth.confirmPassword')" prop="confirmPassword">
              <el-input v-model="registerForm.confirmPassword" size="large" type="password" show-password />
            </el-form-item>
            <el-button :loading="loading" :icon="UserPlus" type="primary" size="large" style="width: 100%" @click="submitRegister">
              {{ $t('auth.registerBtn') }}
            </el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { LogIn, ShieldCheck, UserPlus } from 'lucide-vue-next'
import { useAuth } from '../stores/auth'
import { notifyError } from '../api/http'

const route = useRoute()
const router = useRouter()
const { t } = useI18n()
const auth = useAuth()
const loading = ref(false)
const mode = ref('login')
const loginRef = ref()
const registerRef = ref()

const loginForm = reactive({ account: '', password: '' })
const registerForm = reactive({ account: '', password: '', confirmPassword: '', phone: '', nickname: '' })

const required = [{ required: true, message: () => t('auth.requiredField'), trigger: 'blur' }]
const loginRules = { account: required, password: required }
const registerRules = {
  nickname: required,
  account: required,
  phone: [{ required: true, message: () => t('auth.phoneInvalid'), trigger: 'blur' }, { pattern: /^1[3-9]\d{9}$/, message: () => t('auth.phoneInvalid'), trigger: 'blur' }],
  password: [{ required: true, message: () => t('auth.requiredField'), trigger: 'blur' }, { min: 6, max: 32, message: () => t('auth.passwordLength'), trigger: 'blur' }],
  confirmPassword: required
}

async function submitLogin() {
  await loginRef.value.validate()
  loading.value = true
  try {
    await auth.login(loginForm)
    ElMessage.success(t('auth.loginSuccess'))
    router.push(route.query.redirect || '/')
  } catch (error) {
    notifyError(error)
  } finally {
    loading.value = false
  }
}

async function submitRegister() {
  await registerRef.value.validate()
  if (registerForm.password !== registerForm.confirmPassword) {
    ElMessage.warning(t('auth.passwordMismatch'))
    return
  }
  loading.value = true
  try {
    await auth.register(registerForm)
    ElMessage.success(t('auth.registerSuccess'))
    mode.value = 'login'
    loginForm.account = registerForm.account
  } catch (error) {
    notifyError(error)
  } finally {
    loading.value = false
  }
}
</script>
