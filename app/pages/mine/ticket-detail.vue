<template>
	<view class="page">
		<view v-if="loading" class="state-wrap">
			<text class="state-text">加载中…</text>
		</view>

		<template v-else-if="detail">
			<view class="header-float">
				<view class="header-float-card">
					<text class="header-title">{{ detail.title }}</text>
					<text class="header-status" :class="statusClass(detail.status)">{{ detail.statusLabel }}</text>
				</view>
			</view>

			<scroll-view
				class="message-scroll"
				scroll-y
				:scroll-top="scrollTop"
				:scroll-with-animation="scrollWithAnimation"
				@scroll="handleScroll"
			>
				<view class="message-list">
					<view
						v-for="msg in detail.messages"
						:key="msg.messageId"
						class="message-row"
						:class="isSelfMessage(msg) ? 'message-row--self' : 'message-row--other'"
					>
						<view v-if="!isSelfMessage(msg)" class="message-side">
							<text class="message-name">{{ msg.senderLabel }}</text>
							<image class="avatar" :src="resolveAvatar(msg)" mode="aspectFill" />
						</view>
						<view class="message-main">
							<view class="message-bubble">
								<text class="message-body">{{ msg.content }}</text>
							</view>
							<text class="message-time">{{ formatDateTime(msg.createTime) }}</text>
						</view>
						<view v-if="isSelfMessage(msg)" class="message-side">
							<text class="message-name">{{ msg.senderLabel }}</text>
							<image class="avatar" :src="resolveAvatar(msg)" mode="aspectFill" />
						</view>
					</view>
					<view id="msg-bottom-anchor" class="message-bottom-anchor" />
				</view>
			</scroll-view>

			<view v-if="canReply" class="composer">
				<input
					v-model="replyContent"
					class="composer-input"
					maxlength="2000"
					placeholder="继续描述问题…"
					placeholder-class="placeholder"
					confirm-type="send"
					@confirm="sendReply"
				/>
				<button
					class="send-btn"
					:loading="sending"
					:disabled="sending"
					hover-class="tap-hover-opacity-light"
					:hover-stay-time="70"
					@click="sendReply"
				>
					发送
				</button>
			</view>
			<view v-else class="composer composer--disabled">
				<text class="closed-text">工单已完成，感谢您的反馈</text>
			</view>
		</template>
	</view>
</template>

<script setup>
import { computed, getCurrentInstance, ref, watch } from 'vue'
import { onLoad, onReady, onShow, onUnload } from '@dcloudio/uni-app'
import { fetchTicketDetail, replyTicket } from '@/api/modules/ticket.js'
import { connectNotifySocket } from '@/services/notify-socket.js'
import { useTicketNotify } from '@/composables/use-ticket-notify.js'
import { useMpChatAutoScroll } from '@/utils/chat-auto-scroll.js'
import { formatDateTime } from '@/utils/datetime-format.js'
import { ticketStatusClass } from '@/utils/ticket-status.js'
import { resolveAvatarUrl } from '@/utils/avatar.js'
import { SERVER_DEFAULT_AVATAR_URL } from '@/config/env.js'
import { getUserProfile } from '@/services/user-profile.js'

const STAFF_AVATAR_URL = SERVER_DEFAULT_AVATAR_URL

const ticketId = ref(null)
const detail = ref(null)
const loading = ref(false)
const sending = ref(false)
const replyContent = ref('')
const scrollWithAnimation = ref(false)
const pendingMessages = []

const instance = getCurrentInstance()
const pageContext = computed(() => instance?.proxy)

const { scrollTop, handleScroll, measure, scrollToBottom } = useMpChatAutoScroll()

const { subscribe: subscribeTicketNotify } = useTicketNotify(handleNotifyPayload)

const canReply = computed(() => detail.value && detail.value.status !== 2)

onLoad((options) => {
	ticketId.value = Number(options?.id)
	if (!ticketId.value) {
		uni.showToast({ title: '工单不存在', icon: 'none' })
		return
	}
	connectNotifySocket()
	subscribeTicketNotify()
	void loadDetail()
})

onReady(() => {
	if (detail.value) {
		void forceScrollToBottom()
	}
})

onShow(() => {
	connectNotifySocket()
	if (detail.value) {
		void forceScrollToBottom()
	}
})

watch(
	() => detail.value?.messages?.length ?? 0,
	() => {
		if (!detail.value || loading.value) {
			return
		}
		scrollWithAnimation.value = true
		void scrollToBottom(false, pageContext.value)
	}
)

watch(loading, (isLoading, wasLoading) => {
	if (wasLoading && !isLoading && detail.value) {
		void forceScrollToBottom()
	}
})

async function forceScrollToBottom() {
	scrollWithAnimation.value = false
	await measure(pageContext.value)
	await scrollToBottom(true, pageContext.value)
}

function statusClass(status) {
	return ticketStatusClass(status)
}

function isSelfMessage(msg) {
	return msg.senderType === 1
}

function resolveAvatar(msg) {
	if (msg.senderType === 2) {
		return msg.senderAvatar || STAFF_AVATAR_URL
	}
	return resolveAvatarUrl(msg.senderAvatar)
}

function normalizeCreateTime(value) {
	if (Array.isArray(value) && value.length >= 3) {
		const [year, month, day, hour = 0, minute = 0, second = 0] = value
		const pad = (n) => String(n).padStart(2, '0')
		return `${year}-${pad(month)}-${pad(day)} ${pad(hour)}:${pad(minute)}:${pad(second)}`
	}
	return value
}

function appendMessage(payload) {
	if (!payload?.messageId || Number(payload.ticketId) !== Number(ticketId.value)) {
		return
	}
	if (!detail.value) {
		pendingMessages.push(payload)
		return
	}
	const exists = detail.value.messages?.some((item) => item.messageId === payload.messageId)
	if (exists) {
		return
	}
	detail.value.messages = [
		...(detail.value.messages || []),
		{
			messageId: payload.messageId,
			senderType: payload.senderType,
			senderLabel: payload.senderLabel,
			senderId: payload.senderId,
			senderAvatar: payload.senderAvatar,
			content: payload.content,
			createTime: normalizeCreateTime(payload.createTime)
		}
	]
}

function handleNotifyPayload(payload) {
	if (Number(payload?.ticketId) !== Number(ticketId.value)) {
		return
	}
	if (payload?.type === 'ticket_message') {
		appendMessage(payload)
		return
	}
	if (payload?.type === 'ticket_status_changed') {
		applyStatusChanged(payload)
	}
}

function applyStatusChanged(payload) {
	if (!detail.value) {
		return
	}
	if (payload.status != null) {
		detail.value.status = payload.status
	}
	if (payload.statusLabel) {
		detail.value.statusLabel = payload.statusLabel
	}
}

function flushPendingMessages() {
	if (!detail.value || !pendingMessages.length) {
		return
	}
	const queue = pendingMessages.splice(0, pendingMessages.length)
	queue.forEach((payload) => appendMessage(payload))
}

async function loadDetail() {
	loading.value = true
	const res = await fetchTicketDetail(ticketId.value)
	loading.value = false
	if (!res.ok || !res.data) {
		uni.showToast({ title: res.msg || '加载失败', icon: 'none' })
		return
	}
	detail.value = res.data
	flushPendingMessages()
}

async function sendReply() {
	const content = replyContent.value.trim()
	if (!content) {
		uni.showToast({ title: '请输入内容', icon: 'none' })
		return
	}
	if (sending.value) return
	sending.value = true
	const res = await replyTicket(ticketId.value, content)
	sending.value = false
	if (!res.ok) {
		uni.showToast({ title: res.msg || '发送失败', icon: 'none' })
		return
	}
	replyContent.value = ''
	const profile = getUserProfile()
	const messageId = Number(res.data)
	if (messageId) {
		appendMessage({
			ticketId: ticketId.value,
			messageId,
			senderType: 1,
			senderLabel: profile?.nickname || '我',
			senderId: profile?.userId,
			senderAvatar: profile?.avatar,
			content,
			createTime: new Date().toISOString().slice(0, 19).replace('T', ' ')
		})
		scrollWithAnimation.value = true
		await scrollToBottom(true, pageContext.value)
		return
	}
	await loadDetail()
}
</script>

<style scoped>
.page {
	height: 100vh;
	background: #f5f5f5;
	position: relative;
	overflow: hidden;
}

.header-float {
	position: fixed;
	top: 8px;
	left: 12px;
	right: 12px;
	z-index: 20;
	pointer-events: none;
}

.header-float-card {
	pointer-events: auto;
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 12px;
	padding: 10px 14px;
	background: rgba(255, 255, 255, 0.96);
	border-radius: 12px;
	box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
	backdrop-filter: blur(8px);
}

.header-title {
	flex: 1;
	min-width: 0;
	font-size: 16px;
	font-weight: 600;
	color: #222;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.header-status {
	flex-shrink: 0;
	font-size: 12px;
	padding: 3px 10px;
	border-radius: 999px;
	line-height: 1.2;
}

.status--pending {
	color: #e6a23c;
	background: #fdf6ec;
}

.status--processing {
	color: #409eff;
	background: #ecf5ff;
}

.status--done {
	color: #909399;
	background: #f4f4f5;
}

.message-scroll {
	position: fixed;
	top: 0;
	left: 0;
	right: 0;
	bottom: calc(60px + env(safe-area-inset-bottom));
	height: auto;
	padding: 72px 12px 12px;
	box-sizing: border-box;
	background: #fafafa;
}

.message-list {
	display: flex;
	flex-direction: column;
	gap: 20px;
	min-height: 100%;
	padding-bottom: 8px;
}

.message-bottom-anchor {
	height: 1px;
	width: 100%;
	flex-shrink: 0;
}

.message-row {
	display: flex;
	align-items: flex-start;
	gap: 10px;
	max-width: 82%;
}

.message-row--other {
	align-self: flex-start;
}

.message-row--self {
	align-self: flex-end;
	margin-left: auto;
}

.message-side {
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: 6px;
	flex-shrink: 0;
	width: 52px;
}

.message-name {
	font-size: 11px;
	color: #909399;
	text-align: center;
	line-height: 1.3;
	max-width: 52px;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
}

.avatar {
	width: 36px;
	height: 36px;
	border-radius: 50%;
	flex-shrink: 0;
	background: #eee;
}

.message-main {
	min-width: 0;
	max-width: calc(100% - 62px);
	display: flex;
	flex-direction: column;
}

.message-row--self .message-main {
	align-items: flex-end;
}

.message-bubble {
	min-height: 40px;
	padding: 10px 14px;
	border-radius: 10px;
	background: #f4f4f5;
	box-sizing: border-box;
}

.message-row--self .message-bubble {
	background: #ecf5ff;
}

.message-body {
	font-size: 14px;
	line-height: 1.6;
	color: #303133;
	white-space: pre-wrap;
	word-break: break-word;
}

.message-time {
	margin-top: 4px;
	font-size: 11px;
	color: #c0c4cc;
	text-align: right;
	line-height: 1.4;
}

.composer {
	position: fixed;
	left: 0;
	right: 0;
	bottom: 0;
	z-index: 30;
	display: flex;
	align-items: center;
	gap: 8px;
	padding: 10px 12px calc(10px + env(safe-area-inset-bottom));
	background: rgba(255, 255, 255, 0.98);
	border-top: 1px solid #eee;
	box-sizing: border-box;
}

.composer--disabled {
	justify-content: center;
}

.composer-input {
	flex: 1;
	height: 40px;
	background: #f5f5f5;
	border-radius: 20px;
	padding: 0 14px;
	font-size: 15px;
}

.send-btn {
	flex-shrink: 0;
	height: 40px;
	line-height: 40px;
	padding: 0 16px;
	background: #007aff;
	color: #fff;
	border-radius: 20px;
	font-size: 14px;
	border: none;
}

.send-btn::after {
	border: none;
}

.closed-text {
	font-size: 14px;
	color: #999;
}

.placeholder {
	color: #bbb;
}

.state-wrap {
	padding: 48px 16px;
	text-align: center;
}

.state-text {
	font-size: 14px;
	color: #999;
}
</style>
