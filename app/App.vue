<script>
import PrivacyPopup from '@/components/privacy/privacy-popup.vue'
import { setupPrivacyAuthorization } from '@/utils/wx-privacy.js'
import { getToken } from '@/api/modules/auth.js'
import { connectNotifySocket } from '@/services/notify-socket.js'
import {
	parseLaunchShopInvite,
	savePendingShopCode,
	savePendingShopInvite
} from '@/utils/qrcode-scan.js'

function saveLaunchShopInvite(options) {
	const parsed = parseLaunchShopInvite(options)
	if (parsed?.token) {
		savePendingShopInvite(parsed.token)
	} else if (parsed?.shopCode) {
		savePendingShopCode(parsed.shopCode)
	}
}

export default {
	components: { PrivacyPopup },
	onLaunch(options) {
		setupPrivacyAuthorization()
		saveLaunchShopInvite(options?.query || options)
		// #ifdef MP-WEIXIN
		try {
			const launch = uni.getLaunchOptionsSync?.()
			saveLaunchShopInvite(launch?.query || launch)
		} catch {
			/* ignore */
		}
		// #endif
	},
	onShow() {
		if (getToken()) {
			connectNotifySocket()
		}
	}
}
</script>

<template>
	<PrivacyPopup />
</template>

<style lang="scss">
	@import '@/uni_modules/uni-scss/index.scss';

	/* #ifndef APP-NVUE */
	page {
		background-color: #f5f5f5;
	}

	/* 小程序点击态：使用 hover-class，勿用 CSS :active */
	.tap-hover-opacity {
		opacity: 0.75 !important;
	}

	.tap-hover-opacity-mid {
		opacity: 0.85 !important;
	}

	.tap-hover-opacity-light {
		opacity: 0.65 !important;
	}

	.tap-hover-opacity-strong {
		opacity: 0.88 !important;
	}

	.tap-hover-scale {
		opacity: 0.88 !important;
		transform: scale(0.96) !important;
	}

	.tap-hover-scale-soft {
		opacity: 0.88 !important;
		transform: scale(0.985) !important;
	}

	.tap-hover-nav {
		background: rgba(255, 255, 255, 0.35) !important;
	}

	.tap-hover-upload {
		background: #eef4fb !important;
		border-color: #007aff !important;
	}

	.tap-hover-row {
		background-color: rgba(0, 0, 0, 0.04) !important;
	}

	.theme-dark .tap-hover-row {
		background-color: rgba(255, 255, 255, 0.06) !important;
	}
	/* #endif */
</style>
