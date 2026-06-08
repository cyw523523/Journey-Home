<template>
  <el-tag :type="optionType(options, value)" effect="light" round>
    {{ displayText }}
  </el-tag>
</template>

<script setup>
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { optionText, optionType } from '../utils/status'

const props = defineProps({
  value: { type: String, default: '' },
  text: { type: String, default: '' },
  options: { type: Array, required: true }
})

const { locale } = useI18n()

const displayText = computed(() => {
  const localized = optionText(props.options, props.value)
  if (locale.value !== 'zh') {
    return localized
  }
  return props.text || localized
})
</script>
