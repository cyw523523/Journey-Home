<template>
  <el-button :type="isFollowed ? 'default' : 'primary'" size="small" :plain="!isFollowed" @click="toggle">
    {{ isFollowed ? '已关注' : '关注' }}
  </el-button>
</template>
<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { communityApi } from '../../api'

const props = defineProps({ userId: Number, initialFollowed: Boolean })
const loading = ref(false)
const isFollowed = ref(props.initialFollowed || false)

async function toggle() {
  loading.value = true
  try {
    if (isFollowed.value) {
      await communityApi.unfollow(props.userId)
      isFollowed.value = false
      ElMessage.success('已取消关注')
    } else {
      await communityApi.follow(props.userId)
      isFollowed.value = true
      ElMessage.success('已关注')
    }
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  } finally {
    loading.value = false
  }
}
</script>
