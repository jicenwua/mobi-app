<template>
	<view class="profile-form">
		<!-- #ifdef MP-WEIXIN -->
		<button
			class="avatar-picker"
			hover-class="tap-hover-opacity-mid"
			:hover-stay-time="70"
			open-type="chooseAvatar"
			:disabled="loading"
			@chooseavatar="onChooseAvatar"
		>
			<view class="avatar-picker-inner">
				<image
					v-if="avatarDisplayUrl"
					class="avatar-image"
					:src="avatarDisplayUrl"
					mode="aspectFill"
				/>
				<view v-else class="avatar-placeholder">
					<text class="avatar-placeholder-letter">{{ avatarLetter }}</text>
				</view>
				<view class="avatar-edit-badge">
					<text class="avatar-edit-icon">✎</text>
				</view>
			</view>
			<text class="avatar-hint">{{ avatarHint }}</text>
		</button>
		<!-- #endif -->
		<!-- #ifndef MP-WEIXIN -->
		<view
			class="avatar-picker avatar-picker--tap"
			hover-class="tap-hover-opacity-mid"
			:hover-stay-time="70"
			@click="onPickAvatarFallback"
		>
			<view class="avatar-picker-inner">
				<image
					v-if="avatarDisplayUrl"
					class="avatar-image"
					:src="avatarDisplayUrl"
					mode="aspectFill"
				/>
				<view v-else class="avatar-placeholder">
					<text class="avatar-placeholder-letter">{{ avatarLetter }}</text>
				</view>
				<view class="avatar-edit-badge">
					<text class="avatar-edit-icon">✎</text>
				</view>
			</view>
			<text class="avatar-hint">{{ avatarHint }}</text>
		</view>
		<!-- #endif -->

		<view class="field">
			<text class="field-label">昵称</text>
			<input
				class="weui-input"
				:value="nickname"
				placeholder="请输入昵称"
				maxlength="20"
				:disabled="loading"
				@input="onNicknameInput"
				@blur="onNicknameBlur"
			/>
		</view>

		<button
			class="submit-btn"
			type="primary"
			:loading="loading"
			:disabled="loading || !canSubmit"
			@click="onFormSubmit"
		>
			{{ loading ? '保存中…' : submitText }}
		</button>
	</view>
</template>

<script setup>
import { ref, watch, computed, toRef } from 'vue'
import { isDefaultAvatarUrl } from '@/utils/avatar.js'

const props = defineProps({
	submitText: { type: String, default: '确认' },
	loading: { type: Boolean, default: false },
	initialAvatar: { type: String, default: '' },
	initialNickname: { type: String, default: '' }
})

const emit = defineEmits(['submit'])

const nickname = ref((props.initialNickname || '').trim())
const avatarPath = ref('')
const initialAvatar = toRef(props, 'initialAvatar')

watch(
	() => props.initialNickname,
	(v) => {
		nickname.value = (v || '').trim()
	}
)

watch(
	() => props.initialAvatar,
	() => {
		avatarPath.value = ''
	}
)

const avatarLetter = computed(() => {
	const name = (nickname.value || '').trim()
	return name ? name.slice(0, 1) : '我'
})

const avatarDisplayUrl = computed(() => {
	const picked = (avatarPath.value || '').trim()
	if (picked) return picked
	const url = (initialAvatar.value || '').trim()
	if (url && !isDefaultAvatarUrl(url)) return url
	return ''
})

const avatarChanged = computed(() => !!(avatarPath.value || '').trim())

const avatarHint = computed(() => {
	if (avatarChanged.value) return '已选择新头像，保存后生效'
	if (avatarDisplayUrl.value) return '点击头像可更换'
	return '点击设置头像'
})

const canSubmit = computed(() => !!(nickname.value || '').trim())

function onChooseAvatar(e) {
	const url = (e.detail?.avatarUrl || '').trim()
	if (url) avatarPath.value = url
}

function onPickAvatarFallback() {
	if (props.loading) return
	uni.chooseImage({
		count: 1,
		sizeType: ['compressed'],
		sourceType: ['album', 'camera'],
		success: (res) => {
			const path = res.tempFilePaths?.[0]
			if (path) avatarPath.value = path
		}
	})
}

function onNicknameInput(e) {
	nickname.value = e.detail?.value ?? ''
}

function onNicknameBlur() {
	nickname.value = (nickname.value || '').trim()
}

/** 校验后向父组件提交昵称与头像路径 */
function onFormSubmit() {
	const name = (nickname.value || '').trim()
	if (!name) {
		uni.showToast({ title: '请输入昵称', icon: 'none' })
		return
	}
	const picked = (avatarPath.value || '').trim()
	const avatar = picked || (initialAvatar.value || '').trim()
	emit('submit', {
		nickname: name,
		avatarPath: avatar
	})
}
</script>

<style scoped>
.profile-form {
	width: 100%;
	box-sizing: border-box;
}

.avatar-picker {
	width: 100%;
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	padding: 16px 0 8px;
	margin: 0;
	background: transparent;
	border: none;
	line-height: normal;
}

.avatar-picker::after {
	border: none;
}

.avatar-picker-inner {
	position: relative;
	width: 96px;
	height: 96px;
	flex-shrink: 0;
}

.avatar-image,
.avatar-placeholder {
	width: 96px;
	height: 96px;
	border-radius: 50%;
}

.avatar-placeholder {
	background: linear-gradient(145deg, #6eb5ff, #007aff);
	display: flex;
	align-items: center;
	justify-content: center;
	overflow: hidden;
}

.avatar-placeholder-letter {
	font-size: 36px;
	color: #ffffff;
	font-weight: 600;
	line-height: 1;
}

.avatar-edit-badge {
	position: absolute;
	right: 0;
	bottom: 0;
	width: 28px;
	height: 28px;
	border-radius: 50%;
	background: #07c160;
	border: 2px solid #ffffff;
	display: flex;
	align-items: center;
	justify-content: center;
	box-shadow: 0 2px 6px rgba(0, 0, 0, 0.12);
}

.avatar-edit-icon {
	font-size: 13px;
	color: #ffffff;
	line-height: 1;
	margin-top: -1px;
}

.avatar-hint {
	margin-top: 12px;
	font-size: 12px;
	color: #888888;
	text-align: center;
	line-height: 1.4;
}

.field {
	margin-top: 20px;
	width: 100%;
}

.field-label {
	display: block;
	margin-bottom: 8px;
	font-size: 13px;
	color: #666666;
}

.weui-input {
	width: 100%;
	height: 44px;
	padding: 0 12px;
	box-sizing: border-box;
	font-size: 15px;
	color: #333333;
	background-color: #f7f7f7;
	border-radius: 8px;
}

.submit-btn {
	margin-top: 28px;
	width: 100%;
	font-size: 16px;
	border-radius: 8px;
}
</style>
