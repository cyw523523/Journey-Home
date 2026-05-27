import { onBeforeUnmount, watchEffect } from 'vue'
import { clearAiAssistantContext, setAiAssistantContext } from '../stores/aiAssistantContext'

export function useAiAssistantPageContext(factory) {
  watchEffect(() => {
    setAiAssistantContext(factory())
  })

  onBeforeUnmount(() => {
    clearAiAssistantContext()
  })
}
