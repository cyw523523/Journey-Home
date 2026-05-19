<template>
  <div class="ai-assistant-widget">
    <div
      class="ai-float-btn"
      :style="{ left: position.x + 'px', top: position.y + 'px' }"
      @mousedown="startDrag"
      @touchstart="startDrag"
      @click="handleClick"
    >
      <Bot :size="24" />
      <span class="ai-float-label">AI助手</span>
    </div>

    <Teleport to="body">
      <Transition name="ai-dialog">
        <div v-if="isOpen" class="ai-dialog-overlay" @click.self="closeDialog">
          <div class="ai-dialog" :style="{ left: dialogPosition.x + 'px', top: dialogPosition.y + 'px' }">
            <div class="ai-dialog-header" @mousedown="startDialogDrag" @touchstart="startDialogDrag">
              <div class="ai-dialog-title">
                <Bot :size="20" />
                <span>AI智能助手</span>
              </div>
              <button class="ai-dialog-close" @click="closeDialog">
                <X :size="18" />
              </button>
            </div>

            <div ref="messagesContainer" class="ai-dialog-messages">
              <div v-if="messages.length === 0" class="ai-welcome">
                <Bot :size="48" />
                <h3>你好！我是归途AI助手</h3>
                <p>我可以帮助你了解流浪动物救助、领养相关知识，解答平台使用问题。</p>
                <div class="ai-quick-questions">
                  <button v-for="q in quickQuestions" :key="q" class="ai-quick-btn" @click="sendQuickQuestion(q)">
                    {{ q }}
                  </button>
                </div>
              </div>
              <div v-for="(msg, idx) in messages" :key="idx" class="ai-message" :class="msg.role">
                <div class="ai-message-avatar">
                  <User v-if="msg.role === 'user'" :size="16" />
                  <Bot v-else :size="16" />
                </div>
                <div class="ai-message-content">{{ msg.content }}</div>
              </div>
              <div v-if="loading" class="ai-message assistant">
                <div class="ai-message-avatar">
                  <Bot :size="16" />
                </div>
                <div class="ai-message-content ai-typing">
                  <span></span><span></span><span></span>
                </div>
              </div>
            </div>

            <div class="ai-dialog-input">
              <el-input
                v-model="inputMessage"
                placeholder="输入你的问题..."
                :disabled="loading"
                @keyup.enter="sendMessage"
              />
              <el-button type="primary" :loading="loading" @click="sendMessage">
                <Send :size="16" />
              </el-button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<script setup>
import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { Bot, Send, User, X } from 'lucide-vue-next'
import { aiAssistantApi } from '../api'
import { notifyError } from '../api/http'

const isOpen = ref(false)
const loading = ref(false)
const inputMessage = ref('')
const messages = ref([])
const messagesContainer = ref(null)

const position = ref({ x: window.innerWidth - 100, y: window.innerHeight - 120 })
const dialogPosition = ref({ x: 0, y: 0 })
const isDragging = ref(false)
const isDialogDragging = ref(false)
const dragOffset = ref({ x: 0, y: 0 })
const hasMoved = ref(false)

const quickQuestions = [
  '如何领养流浪动物？',
  '领养需要什么条件？',
  '如何发布救助信息？'
]

function handleClick(e) {
  if (hasMoved.value) {
    hasMoved.value = false
    return
  }
  if (!isOpen.value) {
    openDialog()
  }
}

function openDialog() {
  isOpen.value = true
  const dialogWidth = 380
  const dialogHeight = 500
  dialogPosition.value = {
    x: Math.max(20, Math.min(window.innerWidth - dialogWidth - 20, position.value.x - dialogWidth / 2)),
    y: Math.max(20, Math.min(window.innerHeight - dialogHeight - 20, position.value.y - dialogHeight - 20))
  }
}

function closeDialog() {
  isOpen.value = false
}

function startDrag(e) {
  if (isOpen.value) return
  isDragging.value = true
  hasMoved.value = false
  const clientX = e.type === 'touchstart' ? e.touches[0].clientX : e.clientX
  const clientY = e.type === 'touchstart' ? e.touches[0].clientY : e.clientY
  dragOffset.value = {
    x: clientX - position.value.x,
    y: clientY - position.value.y
  }
  e.preventDefault()
}

function startDialogDrag(e) {
  isDialogDragging.value = true
  const clientX = e.type === 'touchstart' ? e.touches[0].clientX : e.clientX
  const clientY = e.type === 'touchstart' ? e.touches[0].clientY : e.clientY
  dragOffset.value = {
    x: clientX - dialogPosition.value.x,
    y: clientY - dialogPosition.value.y
  }
  e.preventDefault()
}

function handleMove(e) {
  if (isDragging.value) {
    const clientX = e.type === 'touchmove' ? e.touches[0].clientX : e.clientX
    const clientY = e.type === 'touchmove' ? e.touches[0].clientY : e.clientY
    const newX = clientX - dragOffset.value.x
    const newY = clientY - dragOffset.value.y
    if (Math.abs(newX - position.value.x) > 5 || Math.abs(newY - position.value.y) > 5) {
      hasMoved.value = true
    }
    position.value = {
      x: Math.max(20, Math.min(window.innerWidth - 80, newX)),
      y: Math.max(100, Math.min(window.innerHeight - 80, newY))
    }
  }
  if (isDialogDragging.value) {
    const clientX = e.type === 'touchmove' ? e.touches[0].clientX : e.clientX
    const clientY = e.type === 'touchmove' ? e.touches[0].clientY : e.clientY
    dialogPosition.value = {
      x: Math.max(20, Math.min(window.innerWidth - 400, clientX - dragOffset.value.x)),
      y: Math.max(20, Math.min(window.innerHeight - 100, clientY - dragOffset.value.y))
    }
  }
}

function handleMoveEnd() {
  isDragging.value = false
  isDialogDragging.value = false
}

async function sendMessage() {
  const msg = inputMessage.value.trim()
  if (!msg || loading.value) return

  messages.value.push({ role: 'user', content: msg })
  inputMessage.value = ''
  loading.value = true

  await nextTick()
  scrollToBottom()

  try {
    const res = await aiAssistantApi.chat({ message: msg })
    messages.value.push({ role: 'assistant', content: res.reply })
  } catch (error) {
    notifyError(error)
    messages.value.push({ role: 'assistant', content: '抱歉，我暂时无法回答这个问题，请稍后再试。' })
  } finally {
    loading.value = false
    await nextTick()
    scrollToBottom()
  }
}

function sendQuickQuestion(q) {
  inputMessage.value = q
  sendMessage()
}

function scrollToBottom() {
  if (messagesContainer.value) {
    messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
  }
}

onMounted(() => {
  window.addEventListener('mousemove', handleMove)
  window.addEventListener('mouseup', handleMoveEnd)
  window.addEventListener('touchmove', handleMove)
  window.addEventListener('touchend', handleMoveEnd)
})

onUnmounted(() => {
  window.removeEventListener('mousemove', handleMove)
  window.removeEventListener('mouseup', handleMoveEnd)
  window.removeEventListener('touchmove', handleMove)
  window.removeEventListener('touchend', handleMoveEnd)
})
</script>

<style scoped>
.ai-float-btn {
  position: fixed;
  z-index: 1000;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 12px 16px;
  border: 1px solid rgba(36, 92, 80, 0.2);
  border-radius: 999px;
  background: linear-gradient(135deg, #245c50, #378b75);
  color: #fff;
  cursor: pointer;
  box-shadow: 0 8px 24px rgba(36, 92, 80, 0.3);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
  user-select: none;
}

.ai-float-btn:hover {
  transform: scale(1.05);
  box-shadow: 0 12px 32px rgba(36, 92, 80, 0.4);
}

.ai-float-label {
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.05em;
}

.ai-dialog-overlay {
  position: fixed;
  inset: 0;
  z-index: 2000;
  pointer-events: auto;
}

.ai-dialog {
  position: absolute;
  width: 380px;
  max-width: calc(100vw - 40px);
  border: 1px solid rgba(91, 77, 57, 0.12);
  border-radius: 24px;
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(252, 248, 242, 0.96));
  box-shadow: 0 24px 80px rgba(50, 37, 20, 0.2);
  overflow: hidden;
  pointer-events: auto;
}

.ai-dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 18px;
  background: linear-gradient(135deg, #245c50, #378b75);
  color: #fff;
  cursor: move;
  user-select: none;
}

.ai-dialog-title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 700;
  font-size: 15px;
}

.ai-dialog-close {
  display: grid;
  width: 28px;
  height: 28px;
  place-items: center;
  border: none;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.15);
  color: #fff;
  cursor: pointer;
  transition: background 0.2s ease;
}

.ai-dialog-close:hover {
  background: rgba(255, 255, 255, 0.25);
}

.ai-dialog-messages {
  height: 340px;
  padding: 16px;
  overflow-y: auto;
  background: rgba(248, 244, 238, 0.5);
}

.ai-welcome {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  padding: 24px 16px;
  text-align: center;
  color: #5a554d;
}

.ai-welcome svg {
  color: #245c50;
}

.ai-welcome h3 {
  margin: 0;
  color: #1d211c;
  font-size: 18px;
}

.ai-welcome p {
  margin: 0;
  font-size: 14px;
  line-height: 1.6;
}

.ai-quick-questions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
  margin-top: 8px;
}

.ai-quick-btn {
  padding: 8px 14px;
  border: 1px solid rgba(36, 92, 80, 0.2);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.8);
  color: #245c50;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s ease;
}

.ai-quick-btn:hover {
  background: #245c50;
  color: #fff;
}

.ai-message {
  display: flex;
  gap: 10px;
  margin-bottom: 14px;
}

.ai-message.user {
  flex-direction: row-reverse;
}

.ai-message-avatar {
  display: grid;
  width: 32px;
  height: 32px;
  flex: 0 0 32px;
  place-items: center;
  border-radius: 50%;
  background: #245c50;
  color: #fff;
}

.ai-message.user .ai-message-avatar {
  background: #e86f51;
}

.ai-message-content {
  max-width: 75%;
  padding: 12px 16px;
  border-radius: 18px;
  background: #fff;
  color: #1d211c;
  font-size: 14px;
  line-height: 1.6;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.ai-message.user .ai-message-content {
  background: #245c50;
  color: #fff;
}

.ai-typing {
  display: flex;
  gap: 4px;
  padding: 16px;
}

.ai-typing span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #245c50;
  animation: typing 1.4s infinite ease-in-out;
}

.ai-typing span:nth-child(2) {
  animation-delay: 0.2s;
}

.ai-typing span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%, 80%, 100% {
    transform: scale(0.6);
    opacity: 0.4;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

.ai-dialog-input {
  display: flex;
  gap: 10px;
  padding: 14px 16px;
  border-top: 1px solid rgba(91, 77, 57, 0.08);
  background: #fff;
}

.ai-dialog-input .el-input {
  flex: 1;
}

.ai-dialog-enter-active,
.ai-dialog-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}

.ai-dialog-enter-from,
.ai-dialog-leave-to {
  opacity: 0;
  transform: scale(0.95);
}
</style>
