import { readonly, ref } from 'vue'

const currentContext = ref(null)

export function setAiAssistantContext(context) {
  currentContext.value = context ? JSON.parse(JSON.stringify(context)) : null
}

export function clearAiAssistantContext() {
  currentContext.value = null
}

export function useAiAssistantContext() {
  return {
    context: readonly(currentContext)
  }
}
