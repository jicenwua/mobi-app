/**
 * 包装异步函数，执行期间忽略后续调用（防连点、重复弹窗）
 * @param {(...args: unknown[]) => Promise<unknown> | unknown} fn
 */
export function withClickLock(fn) {
  let locked = false
  return async function lockedFn(...args) {
    if (locked) return
    locked = true
    try {
      return await fn.apply(this, args)
    } finally {
      locked = false
    }
  }
}
