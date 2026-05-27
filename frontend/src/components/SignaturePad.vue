<template>
  <div class="signature-pad">
    <div class="signature-pad__header">
      <div>
        <h4 class="signature-pad__title">{{ title }}</h4>
        <p class="signature-pad__hint">{{ hint }}</p>
      </div>
      <button
        class="signature-pad__clear"
        type="button"
        @click="clearPad"
      >
        {{ clearText }}
      </button>
    </div>

    <div
      ref="surfaceRef"
      class="signature-pad__surface"
    >
      <canvas
        ref="canvasRef"
        class="signature-pad__canvas"
        @pointerdown="handlePointerDown"
        @pointermove="handlePointerMove"
        @pointerup="handlePointerUp"
        @pointerleave="handlePointerUp"
        @pointercancel="handlePointerUp"
      />
      <div
        v-if="!hasSignature"
        class="signature-pad__placeholder"
      >
        {{ placeholder }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'

const props = defineProps({
  modelValue: {
    type: String,
    default: '',
  },
  title: {
    type: String,
    default: '手写签名',
  },
  hint: {
    type: String,
    default: '请在下方签名区域手写签署，提交后将作为协议签名图保存。',
  },
  clearText: {
    type: String,
    default: '清空重签',
  },
  placeholder: {
    type: String,
    default: '请在这里手写签名',
  },
  lineColor: {
    type: String,
    default: '#245447',
  },
})

const emit = defineEmits(['update:modelValue', 'change'])

const surfaceRef = ref(null)
const canvasRef = ref(null)
const hasSignature = ref(Boolean(props.modelValue))
const isDrawing = ref(false)

let context = null
let resizeObserver = null

function setupCanvas() {
  const canvas = canvasRef.value
  const surface = surfaceRef.value
  if (!canvas || !surface) {
    return
  }

  const ratio = window.devicePixelRatio || 1
  const width = Math.max(surface.clientWidth, 280)
  const height = Math.max(surface.clientHeight, 180)

  canvas.width = width * ratio
  canvas.height = height * ratio
  canvas.style.width = `${width}px`
  canvas.style.height = `${height}px`

  context = canvas.getContext('2d')
  context.scale(ratio, ratio)
  context.lineWidth = 3
  context.lineCap = 'round'
  context.lineJoin = 'round'
  context.strokeStyle = props.lineColor

  if (props.modelValue) {
    restoreSignature(props.modelValue)
  } else {
    clearCanvasOnly()
  }
}

function clearCanvasOnly() {
  const canvas = canvasRef.value
  if (!canvas || !context) {
    return
  }
  context.clearRect(0, 0, canvas.width, canvas.height)
}

function restoreSignature(dataUrl) {
  const image = new Image()
  image.onload = () => {
    clearCanvasOnly()
    const canvas = canvasRef.value
    if (!canvas || !context) {
      return
    }
    context.drawImage(image, 0, 0, canvas.clientWidth, canvas.clientHeight)
    hasSignature.value = true
  }
  image.src = dataUrl
}

function getPoint(event) {
  const canvas = canvasRef.value
  if (!canvas) {
    return { x: 0, y: 0 }
  }
  const rect = canvas.getBoundingClientRect()
  return {
    x: event.clientX - rect.left,
    y: event.clientY - rect.top,
  }
}

function handlePointerDown(event) {
  if (!context) {
    return
  }
  isDrawing.value = true
  const point = getPoint(event)
  context.beginPath()
  context.moveTo(point.x, point.y)
}

function handlePointerMove(event) {
  if (!isDrawing.value || !context) {
    return
  }
  const point = getPoint(event)
  context.lineTo(point.x, point.y)
  context.stroke()
  if (!hasSignature.value) {
    hasSignature.value = true
  }
}

function handlePointerUp() {
  if (!isDrawing.value) {
    return
  }
  isDrawing.value = false
  commitSignature()
}

function commitSignature() {
  const canvas = canvasRef.value
  if (!canvas) {
    return
  }
  const value = hasSignature.value ? canvas.toDataURL('image/png') : ''
  emit('update:modelValue', value)
  emit('change', value)
}

function clearPad() {
  hasSignature.value = false
  clearCanvasOnly()
  emit('update:modelValue', '')
  emit('change', '')
}

onMounted(async () => {
  await nextTick()
  setupCanvas()
  resizeObserver = new ResizeObserver(() => {
    const currentValue = props.modelValue
    setupCanvas()
    if (currentValue) {
      restoreSignature(currentValue)
    }
  })
  if (surfaceRef.value) {
    resizeObserver.observe(surfaceRef.value)
  }
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
})

watch(
  () => props.modelValue,
  (value) => {
    if (!canvasRef.value) {
      return
    }
    if (!value) {
      hasSignature.value = false
      clearCanvasOnly()
      return
    }
    restoreSignature(value)
  }
)
</script>

<style scoped>
.signature-pad {
  display: grid;
  gap: 12px;
}

.signature-pad__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.signature-pad__title {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #1f3c34;
}

.signature-pad__hint {
  margin: 4px 0 0;
  font-size: 13px;
  line-height: 1.5;
  color: rgba(31, 60, 52, 0.72);
}

.signature-pad__clear {
  border: 1px solid rgba(36, 84, 71, 0.18);
  border-radius: 999px;
  background: #fff;
  color: #245447;
  padding: 8px 14px;
  font-size: 13px;
  cursor: pointer;
}

.signature-pad__surface {
  position: relative;
  min-height: 200px;
  border: 1px dashed rgba(36, 84, 71, 0.28);
  border-radius: 18px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.96), rgba(246, 250, 247, 0.92)),
    repeating-linear-gradient(
      to bottom,
      transparent 0,
      transparent 34px,
      rgba(36, 84, 71, 0.05) 34px,
      rgba(36, 84, 71, 0.05) 35px
    );
  overflow: hidden;
}

.signature-pad__canvas {
  position: relative;
  z-index: 1;
  width: 100%;
  height: 200px;
  touch-action: none;
  cursor: crosshair;
}

.signature-pad__placeholder {
  position: absolute;
  inset: 0;
  display: grid;
  place-items: center;
  color: rgba(31, 60, 52, 0.35);
  font-size: 14px;
  pointer-events: none;
}

@media (max-width: 640px) {
  .signature-pad__header {
    flex-direction: column;
  }

  .signature-pad__clear {
    width: 100%;
  }
}
</style>
