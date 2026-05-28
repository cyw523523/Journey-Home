import { onBeforeUnmount, watchEffect } from 'vue'
import { clearAiAssistantContext, setAiAssistantContext } from '../stores/aiAssistantContext'

export function useAiAssistantPageContext(factory) {
  const ownerId = Symbol('ai-assistant-page-context')

  watchEffect(() => {
    setAiAssistantContext(ownerId, factory())
  })

  onBeforeUnmount(() => {
    clearAiAssistantContext(ownerId)
  })
}
