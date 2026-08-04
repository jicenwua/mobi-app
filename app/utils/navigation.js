/** Toast 后延迟返回上一页（权限拦截、提交成功等场景） */
export function navigateBackDelayed(ms = 800, delta = 1) {
	setTimeout(() => uni.navigateBack({ delta }), ms)
}
