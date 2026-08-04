<template>
	<view class="page">
		<view class="card">
			<view class="card-head">
				<text class="card-title">编辑资料</text>
				<text class="card-desc">修改头像或昵称后将同步至服务端</text>
			</view>
			<ProfileForm
				submit-text="保存"
				:loading="saving"
				:initial-avatar="initialAvatar"
				:initial-nickname="initialNickname"
				@submit="onFormSubmit"
			/>
		</view>

		<view class="card staff-card">
			<view class="card-head">
				<text class="card-title">店员邀请码</text>
				<text class="card-desc">请店长扫描此码，将你添加为店铺员工</text>
			</view>
			<view v-if="staffQrLoading" class="staff-qr-loading">
				<PageLoading text="生成中…" :size="32" color="#07c160" />
			</view>
			<view v-else-if="staffQrContent" class="staff-qr-wrap">
				<QrcodeCanvas :text="staffQrContent" :size="200" canvas-id="staff-invite-qrcode" />
				<text class="staff-expire-text">{{ staffExpireHint }}</text>
			</view>
			<view v-else class="staff-state">
				<text class="staff-state-text">{{ staffQrError || '无法生成邀请码' }}</text>
			</view>
			<view class="staff-refresh-btn" :class="{ disabled: staffQrLoading }" @click="refreshStaffQr">
				<text class="staff-refresh-text">刷新邀请码</text>
			</view>
		</view>
	</view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad, onShow, onHide, onUnload } from '@dcloudio/uni-app'
import { useExpiringQrcode } from '@/composables/use-expiring-qrcode.js'
import ProfileForm from '@/components/profile/profile-form.vue'
import QrcodeCanvas from '@/components/common/qrcode-canvas.vue'
import PageLoading from '@/components/common/page-loading.vue'
import { updateUserInfo } from '@/api/modules/user.js'
import { generateStaffInviteQrcode } from '@/api/modules/qrcode.js'
import { applyManualProfile } from '@/services/user-profile.js'
import { takeProfileEditCache } from '@/services/profile-cache.js'
import { resolveAvatarUrl, toServerAvatar } from '@/utils/avatar.js'
import { navigateBackDelayed } from '@/utils/navigation.js'

const cached = ref(null)
const initialAvatar = ref('')
const initialNickname = ref('')
const saving = ref(false)

const staffQrContent = ref('')
const staffQrLoading = ref(true)
const staffQrError = ref('')
let staffQrInFlight = false
let profilePageReady = false

const {
	countdownLeft: staffCountdownLeft,
	expireAt: staffExpireAt,
	stopCountdown: stopStaffCountdown,
	syncFromExpireAt: syncStaffCountdownFromExpireAt
} = useExpiringQrcode(() => {
	void refreshStaffQr()
})

const staffExpireHint = computed(() => {
	if (!staffQrContent.value || staffCountdownLeft.value <= 0) return ''
	return `${staffCountdownLeft.value} 秒后自动刷新`
})

function initProfilePage() {
	if (profilePageReady) return
	profilePageReady = true
	cached.value = takeProfileEditCache()
	initialAvatar.value = cached.value?.avatar || cached.value?.avatarUrl || ''
	initialNickname.value = cached.value?.nickname || ''
}

function ensureStaffQrLoaded() {
	if (staffQrInFlight) return
	if (staffQrContent.value && staffCountdownLeft.value > 0) return
	void refreshStaffQr()
}

onLoad(() => {
	initProfilePage()
	ensureStaffQrLoaded()
})

onShow(() => {
	initProfilePage()
	ensureStaffQrLoaded()
})

onHide(() => {
	stopStaffCountdown()
})

onUnload(() => {
	stopStaffCountdown()
})

async function refreshStaffQr() {
	if (staffQrInFlight) return
	staffQrInFlight = true
	staffQrLoading.value = true
	staffQrError.value = ''
	try {
		const res = await generateStaffInviteQrcode()
		if (!res.ok || !res.data?.qrContent) {
			staffQrContent.value = ''
			staffQrError.value = res.msg || '生成失败'
			stopStaffCountdown()
			return
		}
		staffQrContent.value = res.data.qrContent
		staffExpireAt.value = res.data.expireAt || 0
		syncStaffCountdownFromExpireAt(staffExpireAt.value)
	} finally {
		staffQrInFlight = false
		staffQrLoading.value = false
	}
}

/**
 * 保存资料：调用 POST /app/update
 * @param {{ nickname: string, avatarPath: string }} payload
 */
async function onFormSubmit({ nickname, avatarPath }) {
	const name = (nickname || '').trim()
	if (!name) return

	const nextAvatar = toServerAvatar(avatarPath)
	const prevAvatar = toServerAvatar(cached.value?.avatar || cached.value?.avatarUrl || '')
	const unchanged = name === (cached.value?.nickname || '').trim() && nextAvatar === prevAvatar
	if (unchanged) {
		uni.navigateBack()
		return
	}

	saving.value = true
	try {
		const result = await updateUserInfo({ nickName: name, avatarPath })
		if (!result.ok) {
			uni.showToast({ title: result.msg || '保存失败', icon: 'none' })
			return
		}

		const displayAvatar = resolveAvatarUrl(nextAvatar || cached.value?.avatar || '')
		applyManualProfile({
			nickname: name,
			avatar: displayAvatar,
			isDefault: !nextAvatar
		})
		uni.showToast({ title: '已保存', icon: 'success' })
		navigateBackDelayed(350)
	} finally {
		saving.value = false
	}
}
</script>

<style scoped>
.page {
	min-height: 100vh;
	padding: 16px;
	box-sizing: border-box;
	background-color: #f5f5f5;
}

.card {
	background-color: #ffffff;
	border-radius: 12px;
	padding: 20px 16px 24px;
	box-sizing: border-box;
}

.staff-card {
	margin-top: 12px;
}

.card-head {
	margin-bottom: 4px;
}

.card-title {
	display: block;
	font-size: 17px;
	font-weight: 600;
	color: #333333;
}

.card-desc {
	display: block;
	margin-top: 6px;
	font-size: 12px;
	color: #999999;
	line-height: 1.5;
}

.staff-qr-wrap {
	display: flex;
	flex-direction: column;
	align-items: center;
	margin-top: 16px;
}

.staff-expire-text {
	margin-top: 10px;
	font-size: 12px;
	color: #999999;
}

.staff-qr-loading {
	display: flex;
	justify-content: center;
	padding: 24px 0 8px;
	min-height: 200px;
	box-sizing: border-box;
}

.staff-state {
	padding: 32px 0 12px;
	text-align: center;
}

.staff-state-text {
	font-size: 14px;
	color: #999999;
}

.staff-refresh-btn {
	margin-top: 12px;
	padding: 10px 0;
	text-align: center;
	border-radius: 8px;
	background: #f0f3f7;
}

.staff-refresh-btn.disabled {
	opacity: 0.6;
}

.staff-refresh-text {
	font-size: 14px;
	color: #007aff;
	font-weight: 500;
}
</style>
