import { nextTick, ref } from 'vue'

const DEFAULT_THRESHOLD = 48

/**
 * 聊天列表自动滚底：默认跟随最新消息；用户手动上滑查看历史时不打断。
 */
export function useChatAutoScroll(threshold = DEFAULT_THRESHOLD) {
  const scrollRef = ref(null)
  const stickToBottom = ref(true)

  function isNearBottom(el) {
    if (!el) return true
    return el.scrollHeight - el.scrollTop - el.clientHeight <= threshold
  }

  function onScroll() {
    stickToBottom.value = isNearBottom(scrollRef.value)
  }

  async function scrollToBottom(force = false) {
    if (!force && !stickToBottom.value) {
      return
    }
    await nextTick()
    const el = scrollRef.value
    if (!el) {
      return
    }
    el.scrollTop = el.scrollHeight
    stickToBottom.value = true
  }

  return {
    scrollRef,
    stickToBottom,
    onScroll,
    scrollToBottom
  }
}
