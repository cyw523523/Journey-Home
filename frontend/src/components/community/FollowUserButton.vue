<template>
  <el-button :type="isFollowed ? 'default' : 'primary'" size="small" :plain="!isFollowed" @click="toggle">
    {{ isFollowed ? '已关注' : '关注' }}
  </el-button>
</template>
<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({ userId: Number, initialFollowed: Boolean })
const isFollowed = ref(props.initialFollowed || false)

async function toggle() {
  isFollowed.value = !isFollowed.value
  ElMessage.success(isFollowed.value ? '已关注' : '已取消关注')
}
</script>
