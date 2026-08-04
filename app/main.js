// #ifndef VUE3
import Vue from 'vue'
import App from './App'
import { configureAuthRelogin } from '@/services/auth-relogin.js'
import { performQuickLogin } from '@/services/login-flow.js'
import { resetAppSession } from '@/services/app-session.js'
import { fetchCurrentUserInfo } from '@/api/modules/auth.js'

configureAuthRelogin({ performQuickLogin, resetAppSession, fetchCurrentUserInfo })

Vue.config.productionTip = false
App.mpType = 'app'

const app = new Vue({ ...App })
app.$mount()
// #endif

// #ifdef VUE3
import { createSSRApp } from 'vue'
import App from './App.vue'
import { configureAuthRelogin } from '@/services/auth-relogin.js'
import { performQuickLogin } from '@/services/login-flow.js'
import { resetAppSession } from '@/services/app-session.js'
import { fetchCurrentUserInfo } from '@/api/modules/auth.js'

configureAuthRelogin({ performQuickLogin, resetAppSession, fetchCurrentUserInfo })

export function createApp() {
	const app = createSSRApp(App)
	return { app }
}
// #endif
