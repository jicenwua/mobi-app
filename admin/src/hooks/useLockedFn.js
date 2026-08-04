import { ref } from 'vue'

/**
 * 异步操作防重复执行（提交按钮、确认操作等）
 * @param {(...args: unknown[]) => Promise<unknown> | unknown} fn
 */
export function useLockedFn(fn) {
  const loading = ref(false)

  async function run(...args) {
    if (loading.value) return
    loading.value = true
    try {
      return await fn(...args)
    } finally {
      loading.value = false
    }
  }

  return { run, loading }
}
