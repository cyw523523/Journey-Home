<template>
  <section class="view-narrow page">
    <div class="section-head">
      <div>
        <h1>领养申请</h1>
        <p>填写申请资料后，可调用 GLM-4-Flash-250414 生成结合动物档案和数据库内容的智能领养建议。</p>
      </div>
    </div>

    <div class="surface form-shell">
      <el-alert v-if="animal.id" type="success" :closable="false" style="margin-bottom: 18px">
        正在申请：{{ animal.typeText || animal.type }}，发现地区：{{ animal.foundRegion }}
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
          <el-input
            v-model="form.reason"
            type="textarea"
            :rows="4"
            placeholder="例如：希望长期陪伴，会负责绝育、疫苗、体检和适应期照护。"
          />
        </el-form-item>

        <el-form-item label="居住条件" prop="livingCondition">
          <el-input
            v-model="form.livingCondition"
            type="textarea"
            :rows="4"
            placeholder="例如：独居公寓，已封窗，白天上班晚上在家，可提供独立活动区。"
          />
        </el-form-item>

        <el-form-item label="饲养经验" prop="experience">
          <el-input
            v-model="form.experience"
            type="textarea"
            :rows="4"
            placeholder="例如：养过猫，熟悉驱虫、疫苗和应激期观察。"
          />
        </el-form-item>

        <div class="ai-adoption-panel">
          <div class="ai-adoption-head">
            <div>
              <p class="eyebrow">GLM-4-Flash-250414</p>
              <h3>AI 智能领养建议</h3>
              <p class="muted">
                后端会把你填写的申请信息和数据库里的动物档案、申请数据一起发给 AI，生成个性化建议。
              </p>
            </div>
            <el-button :loading="matching" type="primary" plain @click="generateSmartMatch">
              生成 AI 建议
            </el-button>
          </div>

          <div class="ai-fields">
            <el-form-item label="住房类型">
              <el-select v-model="preferences.homeType" placeholder="请选择">
                <el-option label="公寓 / 宿舍" value="APARTMENT" />
                <el-option label="独立住房" value="HOUSE" />
                <el-option label="带院住房" value="HOUSE_WITH_YARD" />
                <el-option label="合租环境" value="SHARED_RENTAL" />
              </el-select>
            </el-form-item>

            <el-form-item label="家庭结构">
              <el-select v-model="preferences.familyType" placeholder="请选择">
                <el-option label="独居" value="SOLO" />
                <el-option label="情侣 / 伴侣同住" value="COUPLE" />
                <el-option label="普通家庭" value="FAMILY" />
                <el-option label="家中有儿童" value="FAMILY_WITH_CHILDREN" />
              </el-select>
            </el-form-item>

            <el-form-item label="可投入时间">
              <el-select v-model="preferences.timeBudget" placeholder="请选择">
                <el-option label="较少" value="LOW" />
                <el-option label="中等" value="MEDIUM" />
                <el-option label="较多" value="HIGH" />
              </el-select>
            </el-form-item>

            <el-form-item label="养宠经验等级">
              <el-select v-model="preferences.experienceLevel" placeholder="请选择">
                <el-option label="新手" value="BEGINNER" />
                <el-option label="有一些经验" value="SOME" />
                <el-option label="经验丰富" value="EXPERIENCED" />
                <el-option label="有救助 / 中途经验" value="RESCUE" />
              </el-select>
            </el-form-item>

            <el-form-item label="期望陪伴风格">
              <el-select v-model="preferences.companionExpectation" placeholder="请选择">
                <el-option label="安静稳定" value="QUIET" />
                <el-option label="亲人互动" value="FRIENDLY" />
                <el-option label="活泼有活力" value="ACTIVE" />
                <el-option label="治愈陪伴" value="HEALING" />
              </el-select>
            </el-form-item>
          </div>

          <el-form-item label="想让 AI 重点回答的问题">
            <el-input
              v-model="preferences.question"
              type="textarea"
              :rows="3"
              maxlength="1000"
              show-word-limit
              placeholder="例如：我白天上班，晚上有时间陪伴，这只动物是否适合我？还需要提前准备什么？"
            />
          </el-form-item>

          <div v-if="smartMatch" class="ai-answer-card">
            <div class="ai-answer-head">
              <div>
                <strong>AI 已生成建议</strong>
                <p class="muted">{{ smartMatch.model }} · {{ formatTime(smartMatch.generatedAt) }}</p>
              </div>
              <el-tag type="success" effect="dark">{{ smartMatch.animalTypeText }}</el-tag>
            </div>
            <div class="ai-answer-content ai-rich-text" v-html="smartMatchHtml"></div>
          </div>
        </div>

        <div style="display: flex; justify-content: flex-end; gap: 10px">
          <el-button :icon="ArrowLeft" @click="$router.back()">返回</el-button>
          <el-button :loading="saving" :icon="Send" type="primary" size="large" @click="submit">提交申请</el-button>
        </div>
      </el-form>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
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
const matching = ref(false)
const formRef = ref()
const animal = ref({})
const smartMatch = ref(null)
const smartMatchHtml = computed(() => renderAiAnswer(smartMatch.value?.answer || ''))

const form = reactive({
  animalId: Number(route.params.animalId),
  applicantName: auth.state.user?.nickname || '',
  contact: auth.state.user?.phone || '',
  reason: '',
  livingCondition: '',
  experience: ''
})

const preferences = reactive({
  homeType: 'APARTMENT',
  familyType: 'SOLO',
  timeBudget: 'MEDIUM',
  experienceLevel: 'SOME',
  companionExpectation: 'FRIENDLY',
  question: ''
})

const rules = {
  applicantName: [{ required: true, message: '请输入申请人姓名', trigger: 'blur' }],
  contact: [{ required: true, message: '请输入联系方式', trigger: 'blur' }],
  reason: [{ required: true, message: '请输入领养理由', trigger: 'blur' }],
  livingCondition: [{ required: true, message: '请输入居住条件', trigger: 'blur' }],
  experience: [{ required: true, message: '请输入饲养经验', trigger: 'blur' }]
}

function formatTime(value) {
  if (!value) return '刚刚'
  return new Date(value).toLocaleString()
}

function escapeHtml(value) {
  return value
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;')
}

function renderInline(value) {
  return escapeHtml(value).replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
}

function renderAiAnswer(value) {
  const lines = value.replace(/\r\n/g, '\n').split('\n')
  const blocks = []
  let paragraph = []
  let listType = null
  let listItems = []

  function flushParagraph() {
    if (!paragraph.length) return
    blocks.push(`<p>${paragraph.map(renderInline).join('<br />')}</p>`)
    paragraph = []
  }

  function flushList() {
    if (!listType || !listItems.length) return
    const tag = listType === 'ol' ? 'ol' : 'ul'
    blocks.push(`<${tag}>${listItems.map((item) => `<li>${renderInline(item)}</li>`).join('')}</${tag}>`)
    listType = null
    listItems = []
  }

  for (const rawLine of lines) {
    const line = rawLine.trim()

    if (!line) {
      flushParagraph()
      flushList()
      continue
    }

    if (/^-{3,}$/.test(line)) {
      flushParagraph()
      flushList()
      blocks.push('<hr />')
      continue
    }

    const headingMatch = line.match(/^#{1,6}\s+(.+)$/)
    if (headingMatch) {
      flushParagraph()
      flushList()
      blocks.push(`<h3>${renderInline(headingMatch[1])}</h3>`)
      continue
    }

    const orderedMatch = line.match(/^\d+\.\s+(.+)$/)
    if (orderedMatch) {
      flushParagraph()
      if (listType !== 'ol') {
        flushList()
        listType = 'ol'
      }
      listItems.push(orderedMatch[1])
      continue
    }

    const bulletMatch = line.match(/^[-*]\s+(.+)$/)
    if (bulletMatch) {
      flushParagraph()
      if (listType !== 'ul') {
        flushList()
        listType = 'ul'
      }
      listItems.push(bulletMatch[1])
      continue
    }

    flushList()
    paragraph.push(line)
  }

  flushParagraph()
  flushList()

  return blocks.join('')
}

async function generateSmartMatch() {
  const missingFields = []
  if (!form.reason.trim()) missingFields.push('领养理由')
  if (!form.livingCondition.trim()) missingFields.push('居住条件')
  if (!form.experience.trim()) missingFields.push('饲养经验')

  if (missingFields.length) {
    ElMessage.warning(`请先填写：${missingFields.join('、')}`)
    return
  }

  matching.value = true
  try {
    smartMatch.value = await adoptionApi.smartMatch({
      animalId: form.animalId,
      applicantName: form.applicantName,
      contact: form.contact,
      reason: form.reason,
      livingCondition: form.livingCondition,
      experience: form.experience,
      homeType: preferences.homeType,
      familyType: preferences.familyType,
      timeBudget: preferences.timeBudget,
      experienceLevel: preferences.experienceLevel,
      companionExpectation: preferences.companionExpectation,
      question: preferences.question
    })
    ElMessage.success('已生成 AI 智能领养建议')
  } catch (error) {
    notifyError(error)
  } finally {
    matching.value = false
  }
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
