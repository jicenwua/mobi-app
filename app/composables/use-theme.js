import { ref } from 'vue'

const THEME_KEY = 'app_theme_is_dark'

export function loadThemeFromStorage() {
	try {
		const stored = uni.getStorageSync(THEME_KEY)
		if (stored === 'dark') return true
		if (stored === 'light') return false
	} catch {
		/* 主题缓存读取失败时使用默认浅色 */
	}
	return false
}

export function saveThemeToStorage(isDark) {
	try {
		uni.setStorageSync(THEME_KEY, isDark ? 'dark' : 'light')
	} catch {
		/* 忽略存储失败 */
	}
}

/** 日间 / 夜间主题（读写 localStorage） */
export function useTheme() {
	const isDark = ref(loadThemeFromStorage())

	function loadTheme() {
		isDark.value = loadThemeFromStorage()
	}

	function toggleTheme() {
		isDark.value = !isDark.value
		saveThemeToStorage(isDark.value)
	}

	return { isDark, loadTheme, toggleTheme }
}
