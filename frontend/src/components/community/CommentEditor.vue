<template>
  <div class="comment-editor">
    <el-input v-model="text" type="textarea" :rows="3" maxlength="5000" show-word-limit :placeholder="placeholder" />
    <div class="editor-actions">
      <EmojiPicker @select="(e) => text += e" />
      <ImageUploader v-model="images" usage="community-comment" :limit="3" />
      <el-button :loading="submitting" :icon="Send" type="primary" size="small" @click="doSubmit">发表</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { Send } from 'lucide-vue-next'
import EmojiPicker from '../EmojiPicker.vue'
import ImageUploader from '../ImageUploader.vue'

const props = defineProps({
  placeholder: { type: String, default: '写下你的评论...' }
})
const emit = defineEmits(['submit'])

const text = ref('')
const images = ref([])
const submitting = ref(false)

function doSubmit() {
  if (!text.value.trim()) return
  emit('submit', { content: text.value, imageUrls: [...images.value] })
}

function reset() {
  text.value = ''
  images.value = []
}

defineExpose({ text, images, submitting, reset })
</script>

<style scoped>
.comment-editor { margin: 16px 0; }
.editor-actions { display: flex; align-items: center; gap: 8px; margin-top: 8px; }
.editor-actions > :last-child { margin-left: auto; }
</style>
