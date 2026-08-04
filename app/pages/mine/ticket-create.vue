<template>
	<view class="page">
		<view class="page-body">
			<view class="tip-card">
				<text class="tip-title">提交工单</text>
				<text class="tip-desc">请尽量详细描述您遇到的问题，客服会在工单内回复您。</text>
			</view>

			<view class="form-card">
				<view class="field">
					<view class="field-head">
						<text class="label">工单标题</text>
						<text class="counter">{{ title.length }}/100</text>
					</view>
					<view class="input-wrap">
						<input
							v-model="title"
							class="input"
							maxlength="100"
							placeholder="请简要描述问题，如：积分未到账"
							placeholder-class="placeholder"
							:disabled="submitting"
						/>
					</view>
				</view>

				<view class="field">
					<view class="field-head">
						<text class="label">问题描述</text>
						<text class="counter">{{ description.length }}/2000</text>
					</view>
					<view class="textarea-wrap">
						<textarea
							v-model="description"
							class="textarea"
							maxlength="2000"
							placeholder="请详细描述您遇到的问题，包括发生时间、操作步骤等"
							placeholder-class="placeholder"
							:disabled="submitting"
							:show-confirm-bar="false"
							:fixed="true"
						/>
					</view>
				</view>
			</view>
		</view>

		<view class="footer">
			<button
				class="submit-btn"
				:class="{ 'submit-btn--disabled': !canSubmit }"
				:loading="submitting"
				:disabled="submitting || !canSubmit"
				hover-class="tap-hover-opacity-light"
				:hover-stay-time="70"
				@click="submit"
			>
				提交工单
			</button>
		</view>
	</view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { createTicket } from '@/api/modules/ticket.js'

const title = ref('')
const description = ref('')
const submitting = ref(false)

const canSubmit = computed(() => title.value.trim().length > 0 && description.value.trim().length > 0)

async function submit() {
	const trimmedTitle = title.value.trim()
	const trimmedDesc = description.value.trim()
	if (!trimmedTitle) {
		uni.showToast({ title: '请输入工单标题', icon: 'none' })
		return
	}
	if (!trimmedDesc) {
		uni.showToast({ title: '请输入问题描述', icon: 'none' })
		return
	}
	if (submitting.value) return
	submitting.value = true
	const res = await createTicket({ title: trimmedTitle, description: trimmedDesc })
	submitting.value = false
	if (!res.ok) {
		uni.showToast({ title: res.msg || '提交失败', icon: 'none' })
		return
	}
	uni.showToast({ title: '提交成功', icon: 'success' })
	setTimeout(() => {
		uni.redirectTo({
			url: `/pages/mine/ticket-detail?id=${res.data}`,
			animationType: 'slide-in-right',
			animationDuration: 200
		})
	}, 400)
}
</script>

<style scoped>
.page {
	min-height: 100vh;
	background: #f5f5f5;
	display: flex;
	flex-direction: column;
	box-sizing: border-box;
}

.page-body {
	flex: 1;
	padding: 16px 16px 0;
	box-sizing: border-box;
}

.tip-card {
	background: linear-gradient(135deg, #eef5ff 0%, #f8fbff 100%);
	border-radius: 12px;
	padding: 16px;
	margin-bottom: 12px;
	border: 1px solid #dbeafe;
}

.tip-title {
	display: block;
	font-size: 16px;
	font-weight: 600;
	color: #1f2937;
	margin-bottom: 6px;
}

.tip-desc {
	display: block;
	font-size: 13px;
	line-height: 1.6;
	color: #6b7280;
}

.form-card {
	background: #fff;
	border-radius: 12px;
	padding: 4px 0;
	box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
	overflow: hidden;
}

.field {
	padding: 16px;
}

.field + .field {
	border-top: 1px solid #f0f0f0;
}

.field-head {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-bottom: 10px;
}

.label {
	font-size: 15px;
	color: #111827;
	font-weight: 600;
}

.counter {
	font-size: 12px;
	color: #9ca3af;
}

.input-wrap,
.textarea-wrap {
	background: #f9fafb;
	border: 1px solid #e5e7eb;
	border-radius: 10px;
	overflow: hidden;
}

.input {
	display: block;
	width: 100%;
	height: 48px;
	line-height: 48px;
	padding: 0 14px;
	box-sizing: border-box;
	font-size: 15px;
	color: #111827;
	background: transparent;
}

.textarea-wrap {
	min-height: 180px;
}

.textarea {
	display: block;
	width: 100%;
	min-height: 180px;
	padding: 14px;
	box-sizing: border-box;
	font-size: 15px;
	line-height: 1.6;
	color: #111827;
	background: transparent;
}

.placeholder {
	color: #9ca3af;
	font-size: 15px;
}

.footer {
	padding: 16px 16px calc(16px + env(safe-area-inset-bottom));
	background: #f5f5f5;
	box-sizing: border-box;
}

.submit-btn {
	width: 100%;
	height: 48px;
	line-height: 48px;
	background: #007aff;
	color: #fff;
	border-radius: 12px;
	font-size: 16px;
	font-weight: 600;
	border: none;
}

.submit-btn--disabled {
	opacity: 0.45;
}

.submit-btn::after {
	border: none;
}
</style>
