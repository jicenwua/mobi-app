<template>
	<view class="page">
		<scroll-view class="scroll" scroll-y>
			<view class="form-header">
				<view class="form-badge">
					<text class="form-badge-text">活</text>
				</view>
				<text class="form-title">{{ pageTitle }}</text>
				<text class="form-desc">{{ isAnnouncement ? '配置公告内容与展示周期' : '配置活动信息与满赠规则' }}</text>
			</view>

			<view class="form-block">
				<text class="form-block-title">基本信息</text>
				<view class="form-field">
					<text class="form-label">活动名称</text>
					<input
						v-model="form.activityName"
						class="form-input"
						placeholder="如：五一充值狂欢"
						placeholder-class="form-placeholder"
					/>
				</view>
				<view class="form-field">
					<text class="form-label">活动类型</text>
					<picker :range="kindLabels" :value="kindIndex" @change="onKindChange">
						<view class="form-picker">
							<text class="form-picker-text">{{ kindLabels[kindIndex] }}</text>
							<text class="form-picker-arrow">›</text>
						</view>
					</picker>
				</view>
				<view class="form-field form-field--last">
					<text class="form-label">{{ isAnnouncement ? '公告正文' : '活动描述' }}</text>
					<text class="form-hint">{{ isAnnouncement ? '必填，将展示给店铺成员' : '选填，补充活动说明' }}</text>
					<textarea
						v-model="form.description"
						class="form-textarea"
						:placeholder="isAnnouncement ? '请输入公告内容' : '请输入活动描述（选填）'"
						placeholder-class="form-placeholder"
						maxlength="500"
					/>
				</view>
			</view>

			<view class="form-block">
				<text class="form-block-title">活动时间</text>
				<view class="form-field form-field--last">
					<text class="form-label">活动周期</text>
					<text class="form-hint">开始不填则立即开始；结束不填则永久有效</text>
					<view class="form-date-range">
						<picker mode="date" :value="form.startDate" @change="form.startDate = $event.detail.value">
							<view class="form-date-cell">
								<text class="form-date-tag">开始</text>
								<text
									class="form-date-val"
									:class="{ 'form-date-val--empty': !form.startDate }"
								>{{ form.startDate || '立即开始' }}</text>
							</view>
						</picker>
						<text class="form-date-sep">至</text>
						<picker mode="date" :value="form.endDate" @change="form.endDate = $event.detail.value">
							<view class="form-date-cell">
								<text class="form-date-tag">结束</text>
								<text
									class="form-date-val"
									:class="{ 'form-date-val--empty': !form.endDate }"
								>{{ form.endDate || '永久有效' }}</text>
							</view>
						</picker>
					</view>
					<view v-if="form.startDate || form.endDate" class="form-date-actions">
						<text v-if="form.startDate" class="form-date-clear" @click="form.startDate = ''">清空开始</text>
						<text v-if="form.endDate" class="form-date-clear" @click="form.endDate = ''">清空结束</text>
					</view>
				</view>
			</view>

			<view v-if="!isAnnouncement" class="form-block">
				<view class="form-block-head">
					<text class="form-block-title form-block-title--inline">满赠规则</text>
					<view class="form-block-add" @click="addRule">
						<text class="form-block-add-text">+ 添加规则</text>
					</view>
				</view>
				<view v-if="!form.rules.length" class="rule-empty">
					<text class="rule-empty-icon">🎁</text>
					<text class="rule-empty-text">暂无规则，点击右上角添加</text>
				</view>
				<view v-else class="rule-list">
					<view v-for="(rule, idx) in form.rules" :key="idx" class="rule-card">
						<view class="rule-card-head">
							<text class="rule-card-index">规则 {{ idx + 1 }}</text>
							<text
								v-if="form.rules.length > 1"
								class="rule-card-del"
								@click="removeRule(idx)"
							>删除</text>
						</view>
						<view class="rule-card-body">
							<view class="rule-input-group">
								<text class="rule-input-label">满</text>
								<view class="rule-input-wrap">
									<input
										v-model="rule.thresholdPoints"
										class="rule-input"
										type="number"
										placeholder="100"
										placeholder-class="form-placeholder"
									/>
									<text class="rule-input-suffix">积分</text>
								</view>
							</view>
							<text class="rule-arrow">→</text>
							<view class="rule-input-group">
								<text class="rule-input-label">赠</text>
								<view class="rule-input-wrap">
									<input
										v-model="rule.giftPoints"
										class="rule-input"
										type="number"
										placeholder="500"
										placeholder-class="form-placeholder"
									/>
									<text class="rule-input-suffix">积分</text>
								</view>
							</view>
						</view>
					</view>
				</view>
			</view>

			<view class="form-footer">
				<view class="form-submit" @click="onSubmit">
					<text class="form-submit-text">{{ submitLabel }}</text>
				</view>
			</view>
		</scroll-view>
	</view>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import {
	fetchManageActivityDetail,
	saveManageActivity,
	parseProductPoints
} from '@/api/modules/shop-manage.js'
import { fetchShopFromUserList } from '@/api/modules/shop.js'
import { isShopManager } from '@/utils/shop-role.js'
import { navigateBackDelayed } from '@/utils/navigation.js'

const shopId = ref('')
const activityId = ref('')
const kindLabels = ['充值满赠', '消费满赠', '公告']
const kindIndex = ref(0)

const isAnnouncement = computed(() => kindIndex.value === 2)

const pageTitle = computed(() => (activityId.value ? '修改活动' : '新建活动'))
const submitLabel = computed(() => (activityId.value ? '保存修改' : '创建活动'))

const form = reactive({
	activityName: '',
	description: '',
	startDate: '',
	endDate: '',
	rules: [{ thresholdPoints: '', giftPoints: '' }]
})

function denyAndBack(message) {
	uni.showToast({ title: message, icon: 'none' })
	navigateBackDelayed(800, 1)
}

onLoad(async (options) => {
	shopId.value = options?.shopId || ''
	activityId.value = options?.activityId || ''
	if (!shopId.value) {
		denyAndBack('缺少店铺 ID')
		return
	}
	const shopRes = await fetchShopFromUserList(shopId.value)
	if (!shopRes.ok || !isShopManager(shopRes.data)) {
		denyAndBack('仅店长可编辑活动')
		return
	}
	if (!activityId.value) {
		uni.setNavigationBarTitle({ title: '新建活动' })
	}
	if (activityId.value) {
		uni.setNavigationBarTitle({ title: '修改活动' })
		const res = await fetchManageActivityDetail(Number(activityId.value))
		if (!res.ok || !res.data) {
			uni.showToast({ title: res.msg || '加载失败', icon: 'none' })
			return
		}
		const d = res.data
		form.activityName = d.activityName || ''
		form.description = d.description || ''
		if (d.kind === 2) {
			kindIndex.value = 2
		} else {
			kindIndex.value = d.activityType === 2 ? 1 : 0
		}
		form.startDate = toDateStr(d.startTime)
		form.endDate = toDateStr(d.endTime)
		form.rules = (d.rules || []).map((r) => ({
			thresholdPoints: r.thresholdAmount != null ? String(r.thresholdAmount) : '',
			giftPoints: r.giftPoints != null ? String(r.giftPoints) : ''
		}))
		if (!form.rules.length) {
			form.rules = [{ thresholdPoints: '', giftPoints: '' }]
		}
	}
})

function onKindChange(e) {
	kindIndex.value = Number(e.detail.value)
}

function addRule() {
	form.rules.push({ thresholdPoints: '', giftPoints: '' })
}

function removeRule(idx) {
	form.rules.splice(idx, 1)
}

function toDateStr(iso) {
	if (!iso) return ''
	return String(iso).slice(0, 10)
}

function toDateTime(dateStr, endOfDay) {
	if (!dateStr) return null
	return endOfDay ? `${dateStr}T23:59:59` : `${dateStr}T00:00:00`
}

async function onSubmit() {
	if (!form.activityName.trim()) {
		uni.showToast({ title: '请输入活动名称', icon: 'none' })
		return
	}
	if (isAnnouncement.value && !form.description.trim()) {
		uni.showToast({ title: '请输入公告正文', icon: 'none' })
		return
	}
	if (form.startDate && form.endDate && form.startDate > form.endDate) {
		uni.showToast({ title: '结束时间不能早于开始时间', icon: 'none' })
		return
	}
	const rules = []
	if (!isAnnouncement.value) {
		for (const r of form.rules) {
			const threshold = parseProductPoints(r.thresholdPoints)
			const pts = parseProductPoints(r.giftPoints)
			if (!threshold || !pts) {
				uni.showToast({ title: '请填写有效的规则（积分为正整数）', icon: 'none' })
				return
			}
			rules.push({ thresholdAmount: threshold, giftPoints: pts })
		}
	}
	const payload = {
		activityId: activityId.value ? Number(activityId.value) : undefined,
		shopId: Number(shopId.value),
		activityName: form.activityName.trim(),
		description: form.description.trim() || null,
		kind: isAnnouncement.value ? 2 : 1,
		activityType: isAnnouncement.value ? undefined : (kindIndex.value === 1 ? 2 : 1),
		startTime: toDateTime(form.startDate, false),
		endTime: toDateTime(form.endDate, true),
		rules: isAnnouncement.value ? [] : rules
	}
	uni.showLoading({ title: '保存中…', mask: true })
	const res = await saveManageActivity(payload)
	uni.hideLoading()
	if (!res.ok) {
		uni.showToast({ title: res.msg || '保存失败', icon: 'none' })
		return
	}
	uni.showToast({ title: '保存成功', icon: 'success' })
	navigateBackDelayed(500)
}
</script>

<style scoped>
.page {
	min-height: 100vh;
	background: #f4f6f9;
}

.scroll {
	height: 100vh;
	box-sizing: border-box;
	padding-bottom: calc(24px + env(safe-area-inset-bottom));
}

.form-header {
	display: flex;
	flex-direction: column;
	align-items: center;
	padding: 20px 16px 8px;
	background: linear-gradient(180deg, #fff 0%, #f4f6f9 100%);
}

.form-badge {
	width: 48px;
	height: 48px;
	border-radius: 14px;
	background: linear-gradient(135deg, #c4b5fd 0%, #7c3aed 100%);
	display: flex;
	align-items: center;
	justify-content: center;
	margin-bottom: 10px;
	box-shadow: 0 6px 16px rgba(124, 58, 237, 0.28);
}

.form-badge-text {
	font-size: 22px;
	font-weight: 700;
	color: #fff;
	line-height: 1;
}

.form-title {
	font-size: 20px;
	font-weight: 700;
	color: #1a1a1a;
}

.form-desc {
	margin-top: 4px;
	font-size: 13px;
	color: #8a94a6;
}

.form-block {
	margin: 12px 16px 0;
	background: #fff;
	border-radius: 14px;
	padding: 14px 14px 4px;
	box-shadow: 0 1px 4px rgba(15, 23, 42, 0.04);
}

.form-block-head {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-bottom: 12px;
}

.form-block-title {
	display: block;
	font-size: 13px;
	font-weight: 600;
	color: #7c3aed;
	margin-bottom: 12px;
	padding-left: 10px;
	position: relative;
}

.form-block-title--inline {
	margin-bottom: 0;
}

.form-block-title::before {
	content: '';
	position: absolute;
	left: 0;
	top: 50%;
	transform: translateY(-50%);
	width: 3px;
	height: 14px;
	border-radius: 2px;
	background: linear-gradient(180deg, #c4b5fd, #7c3aed);
}

.form-block-add {
	padding: 4px 12px;
	background: rgba(124, 58, 237, 0.1);
	border-radius: 16px;
}

.form-block-add-text {
	font-size: 12px;
	font-weight: 600;
	color: #7c3aed;
}

.form-field {
	margin-bottom: 14px;
}

.form-field--last {
	margin-bottom: 10px;
}

.form-label {
	display: block;
	font-size: 14px;
	font-weight: 600;
	color: #2c3e50;
	margin-bottom: 8px;
}

.form-hint {
	display: block;
	font-size: 12px;
	color: #8a94a6;
	margin: -4px 0 8px;
	line-height: 1.5;
}

.form-textarea {
	width: 100%;
	min-height: 96px;
	padding: 12px 14px;
	box-sizing: border-box;
	border-radius: 10px;
	background: #f8fafc;
	font-size: 14px;
	color: #2c3e50;
	line-height: 1.5;
}

.form-date-actions {
	display: flex;
	gap: 16px;
	margin-top: 8px;
}

.form-date-clear {
	font-size: 12px;
	color: #7c3aed;
}

.form-input {
	width: 100%;
	height: 44px;
	padding: 0 14px;
	box-sizing: border-box;
	background: #f7f9fc;
	border: 1px solid #e4e9f0;
	border-radius: 10px;
	font-size: 15px;
	color: #1a1a1a;
}

.form-placeholder {
	color: #b8c0cc;
	font-size: 15px;
}

.form-picker {
	display: flex;
	align-items: center;
	justify-content: space-between;
	height: 44px;
	padding: 0 14px;
	box-sizing: border-box;
	background: #f7f9fc;
	border: 1px solid #e4e9f0;
	border-radius: 10px;
}

.form-picker-text {
	flex: 1;
	font-size: 15px;
	color: #1a1a1a;
}

.form-picker-arrow {
	font-size: 20px;
	color: #c5cdd8;
	font-weight: 300;
	margin-left: 8px;
}

.form-date-range {
	display: flex;
	align-items: stretch;
	gap: 8px;
}

.form-date-cell {
	flex: 1;
	min-width: 0;
	display: flex;
	flex-direction: column;
	justify-content: center;
	padding: 10px 12px;
	background: #f7f9fc;
	border: 1px solid #e4e9f0;
	border-radius: 10px;
	box-sizing: border-box;
}

.form-date-tag {
	font-size: 11px;
	color: #9aa3b2;
	margin-bottom: 4px;
}

.form-date-val {
	font-size: 14px;
	font-weight: 500;
	color: #1a1a1a;
}

.form-date-val--empty {
	color: #b8c0cc;
	font-weight: 400;
}

.form-date-sep {
	flex-shrink: 0;
	align-self: center;
	font-size: 13px;
	color: #9aa3b2;
}

.rule-empty {
	display: flex;
	flex-direction: column;
	align-items: center;
	padding: 24px 0 20px;
}

.rule-empty-icon {
	font-size: 28px;
	margin-bottom: 8px;
	opacity: 0.6;
}

.rule-empty-text {
	font-size: 13px;
	color: #9aa3b2;
}

.rule-list {
	padding-bottom: 6px;
}

.rule-card {
	background: #f7f9fc;
	border: 1px solid #e4e9f0;
	border-radius: 12px;
	padding: 12px;
	margin-bottom: 10px;
}

.rule-card:last-child {
	margin-bottom: 4px;
}

.rule-card-head {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-bottom: 10px;
}

.rule-card-index {
	font-size: 12px;
	font-weight: 600;
	color: #7c3aed;
}

.rule-card-del {
	font-size: 13px;
	color: #e64340;
	padding: 2px 4px;
}

.rule-card-body {
	display: flex;
	align-items: center;
	gap: 8px;
}

.rule-input-group {
	flex: 1;
	min-width: 0;
}

.rule-input-label {
	display: block;
	font-size: 12px;
	color: #9aa3b2;
	margin-bottom: 6px;
}

.rule-input-wrap {
	display: flex;
	align-items: center;
	background: #fff;
	border: 1px solid #e4e9f0;
	border-radius: 10px;
	overflow: hidden;
}

.rule-input {
	flex: 1;
	min-width: 0;
	height: 40px;
	padding: 0 10px;
	font-size: 15px;
	color: #1a1a1a;
	box-sizing: border-box;
}

.rule-input-suffix {
	padding-right: 10px;
	font-size: 12px;
	font-weight: 500;
	color: #7c3aed;
	flex-shrink: 0;
}

.rule-arrow {
	flex-shrink: 0;
	font-size: 16px;
	color: #c5cdd8;
	padding-top: 18px;
}

.form-footer {
	padding: 20px 16px 8px;
}

.form-submit {
	height: 48px;
	line-height: 48px;
	text-align: center;
	border-radius: 12px;
	background: linear-gradient(135deg, #c4b5fd 0%, #7c3aed 100%);
	box-shadow: 0 4px 14px rgba(124, 58, 237, 0.32);
}

.form-submit-text {
	font-size: 16px;
	font-weight: 600;
	color: #fff;
}
</style>
