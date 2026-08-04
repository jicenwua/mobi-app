<template>
	<view class="mine-panel-content" :class="isDark ? 'theme-dark' : 'theme-light'">
		<view
			class="profile-card"
			hover-class="tap-hover-row"
			:hover-stay-time="70"
			@click="emit('profile')"
		>
			<image
				class="profile-avatar"
				:src="userProfile.avatar"
				mode="aspectFill"
				lazy-load
			/>
			<view class="profile-meta">
				<text class="profile-name">{{ displayNickname }}</text>
				<text class="profile-sub">{{ profileSubText }}</text>
			</view>
			<text class="cell-arrow">›</text>
		</view>

		<view class="menu-card">
			<view class="menu-row" hover-class="tap-hover-row" :hover-stay-time="70" @click="emit('coupons')">
				<text class="menu-label">折扣券</text>
				<view class="menu-row-right">
					<text class="menu-value">{{ couponCount }} 张</text>
					<text class="cell-arrow">›</text>
				</view>
			</view>
			<view class="menu-divider" />
			<view class="menu-row" hover-class="tap-hover-row" :hover-stay-time="70" @click="emit('pay-qrcode')">
				<text class="menu-label">付款码</text>
				<view class="menu-row-right">
					<text class="menu-value menu-value--action">出示</text>
					<text class="cell-arrow">›</text>
				</view>
			</view>
			<view class="menu-divider" />
			<view class="menu-row" hover-class="tap-hover-row" :hover-stay-time="70" @click="emit('set-password')">
				<text class="menu-label">支付密码</text>
				<view class="menu-row-right">
					<text class="menu-value">{{ payPasswordLabel }}</text>
					<text class="cell-arrow">›</text>
				</view>
			</view>
			<view class="menu-divider" />
			<view class="menu-row" hover-class="tap-hover-row" :hover-stay-time="70" @click="emit('tickets')">
				<text class="menu-label">客服</text>
				<view class="menu-row-right">
					<view v-if="ticketUnreadCount > 0" class="menu-unread-badge">
						<text class="menu-unread-badge-text">{{ unreadBadgeText }}</text>
					</view>
					<text class="cell-arrow">›</text>
				</view>
			</view>
		</view>

		<text v-if="loginError" class="login-result">{{ loginError }}</text>
	</view>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
	userProfile: {
		type: Object,
		required: true
	},
	displayNickname: {
		type: String,
		required: true
	},
	profileSubText: {
		type: String,
		required: true
	},
	couponCount: {
		type: Number,
		required: true
	},
	payPasswordLabel: {
		type: String,
		required: true
	},
	ticketUnreadCount: {
		type: Number,
		default: 0
	},
	loginError: {
		type: String,
		default: ''
	},
	isDark: {
		type: Boolean,
		required: true
	}
})

const emit = defineEmits(['profile', 'coupons', 'pay-qrcode', 'set-password', 'tickets'])

const unreadBadgeText = computed(() => {
	const count = props.ticketUnreadCount
	if (count > 99) return '99+'
	return String(count)
})
</script>

<script>
export default {
	options: {
		virtualHost: true
	}
}
</script>

<style scoped>
.mine-panel-content {
	width: 100%;
	display: flex;
	flex-direction: column;
	gap: 16px;
}

.profile-card {
	display: flex;
	align-items: center;
	padding: 20px 16px;
	border-radius: 12px;
	gap: 14px;
}

.theme-light .profile-card,
.theme-light .menu-card {
	background-color: #ffffff;
	box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.theme-dark .profile-card,
.theme-dark .menu-card {
	background-color: #1e1e1e;
	box-shadow: 0 1px 4px rgba(0, 0, 0, 0.25);
}

.profile-avatar {
	width: 64px;
	height: 64px;
	border-radius: 50%;
	flex-shrink: 0;
}

.profile-avatar--placeholder {
	background: linear-gradient(135deg, #6eb5ff, #007aff);
	display: flex;
	align-items: center;
	justify-content: center;
}

.profile-avatar-letter {
	font-size: 26px;
	color: #ffffff;
	font-weight: 600;
}

.profile-meta {
	flex: 1;
	min-width: 0;
	display: flex;
	flex-direction: column;
	gap: 6px;
}

.profile-name {
	font-size: 18px;
	font-weight: 600;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.profile-sub {
	font-size: 12px;
}

.theme-light .profile-sub {
	color: #666666;
}

.theme-dark .profile-sub {
	color: #a8a8a8;
}

.menu-card {
	border-radius: 12px;
	overflow: hidden;
}

.menu-row {
	display: flex;
	align-items: center;
	justify-content: space-between;
	padding: 16px;
	min-height: 52px;
	box-sizing: border-box;
}

.menu-row--btn {
	margin: 0;
	padding: 16px;
	width: 100%;
	border: none;
	border-radius: 0;
	background-color: transparent;
	line-height: normal;
	font-size: inherit;
	text-align: left;
	display: flex;
	align-items: center;
	justify-content: space-between;
	color: inherit;
}

.menu-row--btn::after {
	border: none;
}

.menu-row-right {
	display: flex;
	align-items: center;
	gap: 6px;
}

.menu-unread-badge {
	min-width: 18px;
	height: 18px;
	padding: 0 5px;
	border-radius: 9px;
	background: #f56c6c;
	flex-shrink: 0;
	display: flex;
	align-items: center;
	justify-content: center;
	box-sizing: border-box;
}

.menu-unread-badge-text {
	font-size: 11px;
	line-height: 1;
	color: #fff;
	font-weight: 600;
}

.menu-label {
	font-size: 15px;
}

.menu-value {
	font-size: 14px;
}

.theme-light .menu-value {
	color: #666666;
}

.theme-dark .menu-value {
	color: #a8a8a8;
}

.menu-divider {
	height: 1px;
	margin-left: 16px;
}

.theme-light .menu-divider {
	background-color: #f0f0f0;
}

.theme-dark .menu-divider {
	background-color: #2a2a2a;
}

.cell-arrow {
	font-size: 20px;
	line-height: 1;
	flex-shrink: 0;
}

.theme-light .cell-arrow {
	color: #999999;
}

.theme-dark .cell-arrow {
	color: #777777;
}

.login-result {
	margin-top: 4px;
	font-size: 12px;
	line-height: 1.5;
	opacity: 0.8;
	word-break: break-all;
	text-align: center;
}
</style>
