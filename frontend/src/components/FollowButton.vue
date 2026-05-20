<template>
  <el-button
    :type="isFollowed ? 'default' : 'primary'"
    :loading="loading"
    :plain="!isFollowed"
    size="small"
    @click="toggleFollow"
  >
    {{ isFollowed ? '已关注' : '关注' }}
  </el-button>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { rescueStationApi } from '../api'

const props = defineProps({
  stationUserId: { type: Number, required: true }
})

const loading = ref(false)
const isFollowed = ref(false)

async function checkStatus() {
  try {
    const data = await rescueStationApi.isFollowing(props.stationUserId)
    isFollowed.value = !!data
  } catch {
    isFollowed.value = false
  }
}

async function toggleFollow() {
  if (!isFollowed.value) {
    loading.value = true
    try {
      await rescueStationApi.follow(props.stationUserId)
      ElMessage.success('已关注')
      isFollowed.value = true
    } catch (error) {
      ElMessage.error(error?.response?.data?.message || '关注失败')
    } finally {
      loading.value = false
    }
  } else {
    loading.value = true
    try {
      await rescueStationApi.unfollow(props.stationUserId)
      ElMessage.success('已取消关注')
      isFollowed.value = false
    } catch (error) {
      ElMessage.error(error?.response?.data?.message || '操作失败')
    } finally {
      loading.value = false
    }
  }
}

onMounted(() => {
  checkStatus()
})
</script>
