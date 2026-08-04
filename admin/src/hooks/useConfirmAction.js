import { ElMessage, ElMessageBox } from 'element-plus'
import { withClickLock } from '@/utils/withClickLock'

/**
 * 确认框 + 异步操作 + 成功提示（列表删除/停用等）
 * @param {{ onSuccess?: () => void | Promise<void> }} options
 */
export function useConfirmAction(options = {}) {
  const { onSuccess } = options

  const run = withClickLock(async ({
    message,
    title = '提示',
    action,
    successMsg = '操作成功'
  }) => {
    try {
      await ElMessageBox.confirm(message, title, { type: 'warning' })
      await action()
      ElMessage.success(successMsg)
      await onSuccess?.()
    } catch {
      // 用户取消或请求失败（错误提示由 request 拦截器处理）
    }
  })

  return { run }
}
