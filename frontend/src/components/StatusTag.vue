<template>
  <el-tag :type="optionType(options, value)" effect="light" round>
    {{ text || optionText(options, value) }}
  </el-tag>
</template>

<script setup>
import { optionText, optionType } from '../utils/status'

defineProps({
  value: { type: String, default: '' },
  text: { type: String, default: '' },
  options: { type: Array, required: true }
})
</script>
