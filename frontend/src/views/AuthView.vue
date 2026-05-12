<template>
  <section class="view-narrow page auth-layout">
    <div class="auth-art surface">
      <p class="eyebrow"><ShieldCheck :size="16" /> 登录后可发布、申请与查看进度</p>
      <h1>把分散的信息，变成可追踪的流程。</h1>
    </div>
    <div class="auth-card surface">
      <el-tabs v-model="mode" stretch>
        <el-tab-pane label="账号登录" name="login">
          <el-form ref="loginRef" :model="loginForm" :rules="loginRules" label-position="top">
            <el-form-item label="账号" prop="account">
              <el-input v-model="loginForm.account" size="large" placeholder="请输入账号" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="loginForm.password" size="large" type="password" show-password placeholder="请输入密码" />
            </el-form-item>
            <el-button :loading="loading" :icon="LogIn" type="primary" size="large" style="width: 100%" @click="submitLogin">
              登录
            </el-button>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="用户注册" name="register">
          <el-form ref="registerRef" :model="registerForm" :rules="registerRules" label-position="top">
            <el-form-item label="昵称" prop="nickname">
              <el-input v-model="registerForm.nickname" size="large" />
            </el-form-item>
            <el-form-item label="账号" prop="account">
              <el-input v-model="registerForm.account" size="large" />
            </el-form-item>
            <el-form-item label="手机号" prop="phone">
              <el-input v-model="registerForm.phone" size="large" />
            </el-form-item>
            <el-form-item label="密码" prop="password">
              <el-input v-model="registerForm.password" size="large" type="password" show-password />
            </el-form-item>
            <el-form-item label="确认密码" prop="confirmPassword">
              <el-input v-model="registerForm.confirmPassword" size="large" type="password" show-password />
            </el-form-item>
            <el-button :loading="loading" :icon="UserPlus" type="primary" size="large" style="width: 100%" @click="submitRegister">
              注册
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
import { LogIn, ShieldCheck, UserPlus } from 'lucide-vue-next'
import { useAuth } from '../stores/auth'
import { notifyError } from '../api/http'

const route = useRoute()
const router = useRouter()
const auth = useAuth()
const loading = ref(false)
const mode = ref('login')
const loginRef = ref()
const registerRef = ref()

const loginForm = reactive({ account: '', password: '' })
const registerForm = reactive({ account: '', password: '', confirmPassword: '', phone: '', nickname: '' })

const required = [{ required: true, message: '请填写必填信息', trigger: 'blur' }]
const loginRules = { account: required, password: required }
const registerRules = {
  nickname: required,
  account: required,
  phone: [{ required: true, message: '请输入手机号', trigger: 'blur' }, { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }, { min: 6, max: 32, message: '密码长度需为6-32位', trigger: 'blur' }],
  confirmPassword: required
}

async function submitLogin() {
  await loginRef.value.validate()
  loading.value = true
  try {
    await auth.login(loginForm)
    ElMessage.success('登录成功')
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
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  loading.value = true
  try {
    await auth.register(registerForm)
    ElMessage.success('注册成功，请登录')
    mode.value = 'login'
    loginForm.account = registerForm.account
  } catch (error) {
    notifyError(error)
  } finally {
    loading.value = false
  }
}
</script>
