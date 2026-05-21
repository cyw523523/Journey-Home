<template>
  <div class="floor-list">
    <div class="floor-toolbar" v-if="totalComments > 0">
      <span>共 {{ totalComments }} 楼</span>
      <el-button text size="small" :type="onlyAuthor ? 'primary' : ''" @click="toggleOnlyAuthor">只看楼主</el-button>
      <el-button text size="small" :type="desc ? 'primary' : ''" @click="toggleOrder">{{ desc ? '正序' : '倒序' }}</el-button>
      <span style="margin-left:auto;display:flex;align-items:center;gap:6px;font-size:13px">
        跳到第 <el-input-number v-model="jumpFloor" :min="1" :max="totalComments" size="small" style="width:80px" controls-position="right" /> 楼
        <el-button size="small" @click="doJump">跳</el-button>
      </span>
    </div>

    <FloorItem v-for="floor in floors" :key="floor.id" :floor="floor" :highlightFor="highlightFloor"
      @reply="startReply" @reply-to="startSubReply" @expand-replies="expandReplies" />

    <el-pagination v-if="totalPages > 1" v-model:current-page="currentPage" :page-size="pageSize" :total="totalComments"
      layout="prev, pager, next" @current-change="load" style="justify-content:center;margin-top:16px" />
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import FloorItem from './FloorItem.vue'
import { communityApi } from '../../api'

const route = useRoute()
const router = useRouter()
const props = defineProps({ postId: { type: Number, required: true }, totalComments: { type: Number, default: 0 } })
const emit = defineEmits(['reply', 'reply-to', 'expand-replies'])

const floors = ref([])
const onlyAuthor = ref(false)
const desc = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const totalPages = ref(1)
const jumpFloor = ref(1)
const highlightFloor = ref(0)

async function load() {
  try {
    const params = {
      page: currentPage.value - 1,
      size: pageSize.value,
      onlyAuthor: onlyAuthor.value,
      order: desc.value ? 'desc' : 'asc'
    }
    const data = await communityApi.listFloors(props.postId, params)
    floors.value = data.content || []
    totalPages.value = Math.ceil((data.totalElements || 0) / pageSize.value)
  } catch {
    floors.value = []
  }
}

function toggleOnlyAuthor() { onlyAuthor.value = !onlyAuthor.value; currentPage.value = 1; load() }
function toggleOrder() { desc.value = !desc.value; currentPage.value = 1; load() }

function doJump() {
  const targetPage = Math.ceil(jumpFloor.value / pageSize.value)
  currentPage.value = targetPage
  highlightFloor.value = jumpFloor.value
  router.replace({ query: { floor: jumpFloor.value } })
  load()
}

function startReply(floor) { emit('reply', { floor, replyToId: floor.id }) }
function startSubReply(floor, reply) { emit('reply-to', { floor, replyToId: reply.id, replyToNickname: reply.authorNickname }) }

async function expandReplies(floor) {
  try {
    const data = await communityApi.listReplies(floor.id, { page: 0, size: 50 })
    floor._allReplies = data.content || []
  } catch {}
}

const exposedLoad = { load }
defineExpose(exposedLoad)

watch(() => route.query.floor, (val) => { if (val) { jumpFloor.value = parseInt(val); doJump() } })
onMounted(() => {
  if (route.query.floor) { jumpFloor.value = parseInt(route.query.floor); doJump() }
  else if (route.query.page) { currentPage.value = parseInt(route.query.page) }
  else { load() }
})
</script>

<style scoped>
.floor-toolbar { display: flex; align-items: center; gap: 12px; padding: 10px 0; border-bottom: 2px solid var(--primary); margin-bottom: 0; font-size: 14px; }
</style>
