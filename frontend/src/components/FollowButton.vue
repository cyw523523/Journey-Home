<template>
  <el-button
    :type="isFollowed ? 'default' : 'primary'"
    :loading="loading"
    :plain="!isFollowed"
    size="small"
    @click="toggleFollow"
  >
    {{ isFollowed ? t('common.followed') : t('common.follow') }}
  </el-button>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useI18n } from 'vue-i18n'
import { rescueStationApi } from '../api'

const props = defineProps({
  stationUserId: { type: Number, required: true }
})

const loading = ref(false)
const isFollowed = ref(false)
const { t } = useI18n()

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
      ElMessage.success(t('common.followed'))
      isFollowed.value = true
    } catch (error) {
      ElMessage.error(error?.response?.data?.message || t('common.operationFailed'))
    } finally {
      loading.value = false
    }
  } else {
    loading.value = true
    try {
      await rescueStationApi.unfollow(props.stationUserId)
      ElMessage.success(t('common.unfollowed'))
      isFollowed.value = false
    } catch (error) {
      ElMessage.error(error?.response?.data?.message || t('common.operationFailed'))
    } finally {
      loading.value = false
    }
  }
}

onMounted(() => {
  checkStatus()
})
</script>
