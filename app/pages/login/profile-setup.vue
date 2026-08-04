<template>
	<view class="page">
		<view class="nav-bar" :style="navBarStyle">
			<text class="nav-title">完善资料</text>
		</view>
		<view class="head">
			<text class="title">完善个人资料</text>
			<text class="desc">设置头像和昵称，将同步保存至您的账号</text>
		</view>

		<view class="profile-setup">
			<!-- #ifdef MP-WEIXIN -->
			<button class="avatar-picker" open-type="chooseAvatar" @chooseavatar="onChooseAvatar">
				<image
					v-if="avatarDisplayUrl"
					class="avatar-picker-img"
					:src="avatarDisplayUrl"
					mode="aspectFill"
				/>
				<view v-else class="avatar-picker-placeholder">
					<text class="avatar-picker-icon">+</text>
				</view>
				<text class="avatar-picker-hint">{{ avatarPath ? '点击更换头像' : '点击设置头像' }}</text>
			</button>
			<view class="nickname-field">
				<text class="nickname-label">昵称</text>
				<input
					class="nickname-input"
					type="nickname"
					:value="nickname"
					placeholder="点击输入或选择微信昵称"
					maxlength="20"
					:disabled="saving"
					@input="onNicknameInput"
					@blur="onNicknameBlur"
				/>
			</view>
			<!-- #endif -->
		</view>

		<view class="actions">
			<button
				class="submit-btn"
				type="primary"
				:loading="saving"
				:disabled="saving"
				@click="onSubmit"
			>
				{{ saving ? '保存中…' : '完成设置' }}
			</button>
			<text v-if="errorMsg" class="error-text">{{ errorMsg }}</text>
		</view>
	</view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getToken } from '@/api/modules/auth.js'
import { updateUserInfo } from '@/api/modules/user.js'
import { applyManualProfile, hasCompletedProfileSetup } from '@/services/user-profile.js'
import { isWxPlaceholderProfile } from '@/services/wx-user-profile.js'
import { resolveAvatarUrl } from '@/utils/avatar.js'
import { DEFAULT_AVATAR_URL } from '@/config/env.js'

const statusBarHeight = ref(0)
const navBarStyle = computed(() => ({
	paddingTop: `${statusBarHeight.value}px`
}))

const avatarPath = ref('')
const nickname = ref('')
const saving = ref(false)
const errorMsg = ref('')

const avatarDisplayUrl = computed(() => {
	if (avatarPath.value) return avatarPath.value
	return DEFAULT_AVATAR_URL
})

onLoad(() => {
	statusBarHeight.value = uni.getSystemInfoSync().statusBarHeight || 20
	if (!getToken()) {
		uni.reLaunch({ url: '/pages/login/login' })
		return
	}
	if (hasCompletedProfileSetup()) {
		uni.reLaunch({ url: '/pages/main/main' })
	}
})

function onChooseAvatar(e) {
	const url = (e.detail?.avatarUrl || '').trim()
	if (url) avatarPath.value = url
}

function onNicknameInput(e) {
	nickname.value = e.detail?.value ?? ''
}

function onNicknameBlur() {
	nickname.value = (nickname.value || '').trim()
}

async function onSubmit() {
	if (saving.value) return
	errorMsg.value = ''

	const nickName = (nickname.value || '').trim()
	const localAvatar = (avatarPath.value || '').trim()
	const profile = { nickName, avatarUrl: localAvatar }

	if (!localAvatar) {
		errorMsg.value = '请先设置头像'
		uni.showToast({ title: errorMsg.value, icon: 'none' })
		return
	}
	if (!nickName) {
		errorMsg.value = '请填写昵称'
		uni.showToast({ title: errorMsg.value, icon: 'none' })
		return
	}
	if (isWxPlaceholderProfile(profile)) {
		errorMsg.value = '请填写真实昵称，不能使用「微信用户」'
		uni.showToast({ title: errorMsg.value, icon: 'none' })
		return
	}

	saving.value = true
	try {
		const result = await updateUserInfo({ nickName, avatarPath: localAvatar })
		if (!result.ok) {
			errorMsg.value = result.msg || '保存失败'
			uni.showToast({ title: errorMsg.value, icon: 'none' })
			return
		}

		const displayAvatar = resolveAvatarUrl(localAvatar)
		applyManualProfile({
			nickname: nickName,
			avatar: displayAvatar,
			isDefault: false
		})
		uni.showToast({ title: '设置成功', icon: 'success' })
		setTimeout(() => {
			uni.reLaunch({ url: '/pages/main/main' })
		}, 350)
	} finally {
		saving.value = false
	}
}
</script>

<style scoped>
.page {
	min-height: 100vh;
	display: flex;
	flex-direction: column;
	align-items: center;
	padding: 0 32px 48px;
	box-sizing: border-box;
	background: linear-gradient(180deg, #f0f6ff 0%, #f5f5f5 45%);
}

.nav-bar {
	width: 100%;
	height: 44px;
	display: flex;
	align-items: center;
	justify-content: center;
	box-sizing: content-box;
}

.nav-title {
	font-size: 17px;
	font-weight: 600;
	color: #1a1a1a;
}

.head {
	display: flex;
	flex-direction: column;
	align-items: center;
	margin-top: 6vh;
}

.title {
	font-size: 22px;
	font-weight: 600;
	color: #1a1a1a;
}

.desc {
	margin-top: 10px;
	font-size: 14px;
	color: #888888;
	text-align: center;
	line-height: 1.5;
}

.profile-setup {
	width: 100%;
	max-width: 320px;
	margin-top: 36px;
	display: flex;
	flex-direction: column;
	align-items: center;
}

.avatar-picker {
	margin: 0;
	padding: 0;
	background: transparent;
	border: none;
	display: flex;
	flex-direction: column;
	align-items: center;
	line-height: normal;
}

.avatar-picker::after {
	border: none;
}

.avatar-picker-img,
.avatar-picker-placeholder {
	width: 96px;
	height: 96px;
	border-radius: 50%;
}

.avatar-picker-placeholder {
	background-color: #e8eef5;
	display: flex;
	align-items: center;
	justify-content: center;
}

.avatar-picker-icon {
	font-size: 36px;
	color: #8aa0b8;
	line-height: 1;
}

.avatar-picker-hint {
	margin-top: 10px;
	font-size: 12px;
	color: #888888;
}

.nickname-field {
	margin-top: 24px;
	width: 100%;
}

.nickname-label {
	display: block;
	margin-bottom: 8px;
	font-size: 13px;
	color: #666666;
}

.nickname-input {
	width: 100%;
	height: 44px;
	padding: 0 12px;
	box-sizing: border-box;
	font-size: 15px;
	color: #333333;
	background-color: #ffffff;
	border-radius: 8px;
}

.actions {
	width: 100%;
	max-width: 320px;
	margin-top: 32px;
}

.submit-btn {
	width: 100%;
	height: 48px;
	line-height: 48px;
	font-size: 16px;
	font-weight: 500;
	border-radius: 24px;
}

.error-text {
	display: block;
	margin-top: 12px;
	font-size: 13px;
	color: #e64340;
	text-align: center;
	line-height: 1.5;
}
</style>
