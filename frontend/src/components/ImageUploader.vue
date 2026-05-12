<template>
  <div class="uploader-wrap">
    <el-upload
      class="image-uploader"
      drag
      multiple
      :action="action"
      :headers="headers"
      :limit="limit"
      :file-list="fileList"
      :on-success="handleSuccess"
      :on-remove="handleRemove"
      :on-error="handleError"
      accept=".jpg,.jpeg,.png,.webp"
    >
      <UploadCloud :size="28" />
      <div class="upload-copy">拖拽图片或点击上传</div>
      <template #tip>
        <div class="el-upload__tip">支持 JPG/PNG/WEBP，单张不超过 5MB</div>
      </template>
    </el-upload>
    <div v-if="modelValue.length" class="thumb-grid">
      <img v-for="url in modelValue" :key="url" :src="url" alt="上传图片预览" />
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadCloud } from 'lucide-vue-next'

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

watch(
  () => props.modelValue,
  (urls) => {
    fileList.value = urls.map((url, index) => ({ name: `image-${index + 1}`, url }))
  },
  { immediate: true }
)

function handleSuccess(response) {
  if (!response?.success) {
    ElMessage.error(response?.message || '上传失败')
    return
  }
  emit('update:modelValue', [...props.modelValue, response.data.url])
}

function handleRemove(file) {
  const url = file.url || file.response?.data?.url
  emit('update:modelValue', props.modelValue.filter((item) => item !== url))
}

function handleError() {
  ElMessage.error('图片上传失败')
}
</script>
