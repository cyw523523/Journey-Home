<template>
  <section class="view-narrow page">
    <div class="section-head">
      <div>
        <h1>领养申请</h1>
        <p>提交后进入管理员审核流程，可在个人中心查看进度。</p>
      </div>
    </div>
    <div class="surface form-shell">
      <el-alert v-if="animal.id" type="success" :closable="false" style="margin-bottom: 18px">
        正在申请：{{ animal.typeText || animal.type }}，发现地区 {{ animal.foundRegion }}
      </el-alert>
      <el-form ref="formRef" :model="form" :rules="rules" label-position="top">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="申请人姓名" prop="applicantName">
              <el-input v-model="form.applicantName" size="large" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系方式" prop="contact">
              <el-input v-model="form.contact" size="large" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="领养理由" prop="reason">
          <el-input v-model="form.reason" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="居住条件" prop="livingCondition">
          <el-input v-model="form.livingCondition" type="textarea" :rows="4" />
        </el-form-item>
        <el-form-item label="饲养经验" prop="experience">
          <el-input v-model="form.experience" type="textarea" :rows="4" />
        </el-form-item>
        <div style="display: flex; justify-content: flex-end; gap: 10px">
          <el-button :icon="ArrowLeft" @click="$router.back()">返回</el-button>
          <el-button :loading="saving" :icon="Send" type="primary" size="large" @click="submit">提交申请</el-button>
        </div>
      </el-form>
    </div>
  </section>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Send } from 'lucide-vue-next'
import { adoptionApi, animalApi } from '../api'
import { notifyError } from '../api/http'
import { useAuth } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const auth = useAuth()
const saving = ref(false)
const formRef = ref()
const animal = ref({})
const form = reactive({
  animalId: Number(route.params.animalId),
  applicantName: auth.state.user?.nickname || '',
  contact: auth.state.user?.phone || '',
  reason: '',
  livingCondition: '',
  experience: ''
})
const rules = {
  applicantName: [{ required: true, message: '请输入申请人姓名', trigger: 'blur' }],
  contact: [{ required: true, message: '请输入联系方式', trigger: 'blur' }],
  reason: [{ required: true, message: '请输入领养理由', trigger: 'blur' }],
  livingCondition: [{ required: true, message: '请输入居住条件', trigger: 'blur' }],
  experience: [{ required: true, message: '请输入饲养经验', trigger: 'blur' }]
}

async function submit() {
  await formRef.value.validate()
  saving.value = true
  try {
    await adoptionApi.create(form)
    ElMessage.success('申请已提交')
    router.push('/profile')
  } catch (error) {
    notifyError(error)
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  try {
    animal.value = await animalApi.detail(route.params.animalId)
  } catch (error) {
    notifyError(error)
  }
})
</script>
