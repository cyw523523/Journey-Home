<template>
  <div class="uploader-wrap">
    <el-upload
      class="image-uploader"
      drag
      :multiple="limit > 1"
      :action="action"
      :headers="headers"
      :limit="limit"
      :file-list="fileList"
      :on-success="handleSuccess"
      :on-remove="handleRemove"
      :on-error="handleError"
      :on-preview="handlePreview"
      accept=".jpg,.jpeg,.png,.webp"
    >
      <UploadCloud :size="28" />
      <div class="upload-copy">{{ t('uploader.prompt') }}</div>
      <template #tip>
        <div class="el-upload__tip">{{ t('uploader.tip') }}</div>
      </template>
    </el-upload>
    <div v-if="modelValue.length" class="thumb-grid">
      <div v-for="(url, index) in modelValue" :key="url" class="thumb-item">
        <img :src="getFullUrl(url)" :alt="t('uploader.previewAlt')" @click="previewImage(url)" />
        <button class="thumb-remove" @click.stop="removeImage(index)">×</button>
      </div>
    </div>

    <el-dialog v-model="previewVisible" :title="t('uploader.previewTitle')" width="600px" append-to-body>
      <div style="text-align: center">
        <img :src="previewUrl" :alt="t('uploader.previewAlt')" style="max-width: 100%; max-height: 70vh; object-fit: contain;" />
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadCloud } from 'lucide-vue-next'
import { useI18n } from 'vue-i18n'

const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  usage: { type: String, default: 'common' },
  limit: { type: Number, default: 6 }
})

const emit = defineEmits(['update:modelValue'])
const action = computed(() => `/api/files/upload?usage=${props.usage}`)
const headers = computed(() => {
  const token = localStorage.getItem('guitu_token')
  return token ? { Authorization: `Bearer ${token}` } : {}
})
const fileList = ref([])
const previewVisible = ref(false)
const previewUrl = ref('')
const { t } = useI18n()

const API_BASE = window.location.origin

function getFullUrl(url) {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://') || url.startsWith('data:')) {
    return url
  }
  return `${API_BASE}${url}`
}

watch(
  () => props.modelValue,
  (urls) => {
    fileList.value = urls.map((url, index) => ({
      name: `image-${index + 1}`,
      url: url
    }))
  },
  { immediate: true }
)

function handleSuccess(response) {
  if (!response?.success) {
    ElMessage.error(response?.message || t('uploader.uploadFailed'))
    return
  }
  const newUrls = [...props.modelValue, response.data.url]
  emit('update:modelValue', newUrls)
  ElMessage.success(t('uploader.uploadSuccess'))
}

function handleRemove(file) {
  const url = file.url || file.response?.data?.url
  emit('update:modelValue', props.modelValue.filter((item) => item !== url))
}

function handleError() {
  ElMessage.error(t('uploader.uploadRetry'))
}

function handlePreview(file) {
  const url = file.url || file.response?.data?.url
  previewUrl.value = getFullUrl(url)
  previewVisible.value = true
}

function previewImage(url) {
  previewUrl.value = getFullUrl(url)
  previewVisible.value = true
}

function removeImage(index) {
  const newUrls = props.modelValue.filter((_, i) => i !== index)
  emit('update:modelValue', newUrls)
}
</script>
