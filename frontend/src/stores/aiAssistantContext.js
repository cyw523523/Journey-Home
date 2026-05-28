import { readonly, ref } from 'vue'

const currentContext = ref(null)
const contextRegistry = new Map()

function cloneContext(context) {
  return context ? JSON.parse(JSON.stringify(context)) : null
}

function syncCurrentContext() {
  const values = [...contextRegistry.values()]
  currentContext.value = values.length ? values[values.length - 1] : null
}

export function setAiAssistantContext(ownerOrContext, maybeContext) {
  const hasOwner = arguments.length === 2
  const owner = hasOwner ? ownerOrContext : 'global'
  const context = hasOwner ? maybeContext : ownerOrContext

  contextRegistry.delete(owner)
  if (context) {
    contextRegistry.set(owner, cloneContext(context))
  }
  syncCurrentContext()
}

export function clearAiAssistantContext(owner = 'global') {
  contextRegistry.delete(owner)
  syncCurrentContext()
}

export function useAiAssistantContext() {
  return {
    context: readonly(currentContext)
  }
}
