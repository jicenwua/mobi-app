<template>
	<view v-if="visible" class="coupon-mask" @click="$emit('close')">
		<scroll-view class="coupon-sheet-scroll" scroll-y @click.stop>
			<view class="coupon-sheet" @click.stop>
				<view class="coupon-sheet-header">
					<view class="coupon-sheet-badge">
						<text class="coupon-sheet-badge-text">券</text>
					</view>
					<text class="coupon-sheet-title">{{ editingCouponId ? '修改折扣券' : '添加折扣券' }}</text>
					<text class="coupon-sheet-desc">{{ editingCouponId ? '创建后不可修改优惠规则，仅可调整发放时间' : '设置优惠规则与发放计划' }}</text>
				</view>

				<view v-if="editingCouponId" class="coupon-block">
					<text class="coupon-block-title">优惠规则（不可修改）</text>
					<view class="coupon-readonly-card">
						<text class="coupon-readonly-name">{{ form.couponName }}</text>
						<text class="coupon-readonly-tag">{{ couponTypeLabels[couponTypeIndex] }}</text>
						<text class="coupon-readonly-rule">{{ formatCouponRuleFromForm }}</text>
						<text class="coupon-readonly-meta">有效期 {{ formatValidDaysFromForm }}</text>
					</view>
				</view>

				<view v-else class="coupon-block">
					<text class="coupon-block-title">基本信息</text>
					<view class="coupon-field">
						<text class="coupon-label">名称</text>
						<input
							v-model="form.couponName"
							class="coupon-input"
							placeholder="如：新客满减券"
							placeholder-class="coupon-placeholder"
							maxlength="50"
						/>
					</view>
					<view class="coupon-field">
						<text class="coupon-label">折扣券类型</text>
						<picker :range="couponTypeLabels" :value="couponTypeIndex" @change="$emit('coupon-type-change', $event)">
							<view class="coupon-picker">
								<text class="coupon-picker-text">{{ couponTypeLabels[couponTypeIndex] }}</text>
								<text class="coupon-picker-arrow">›</text>
							</view>
						</picker>
					</view>
				</view>

				<view v-if="!editingCouponId" class="coupon-block">
					<text class="coupon-block-title">优惠规则</text>
					<view class="coupon-field">
						<text class="coupon-label">使用门槛</text>
						<text class="coupon-hint">满多少积分可用，不填表示无门槛</text>
						<view class="coupon-input-wrap coupon-input-wrap--suffix">
							<input
								v-model="form.thresholdAmount"
								class="coupon-input coupon-input--inset"
								type="digit"
								placeholder="不限"
								placeholder-class="coupon-placeholder"
							/>
							<text class="coupon-input-suffix">积分</text>
						</view>
					</view>
					<view class="coupon-field">
						<text class="coupon-label">{{ form.type === 1 ? '折扣力度' : '减免积分' }}</text>
						<text class="coupon-hint">{{ form.type === 1 ? '如 85 表示 8.5 折' : '固定减免积分数' }}</text>
						<view class="coupon-input-wrap coupon-input-wrap--suffix">
							<input
								v-model="form.discountValue"
								class="coupon-input coupon-input--inset"
								type="digit"
								:placeholder="form.type === 1 ? '85' : '20'"
								placeholder-class="coupon-placeholder"
							/>
							<text class="coupon-input-suffix">{{ form.type === 1 ? '折' : '积分' }}</text>
						</view>
					</view>
				</view>

				<view class="coupon-block">
					<text class="coupon-block-title">{{ editingCouponId ? '发放设置' : '时间与发放' }}</text>
					<view v-if="!editingCouponId" class="coupon-field">
						<view class="coupon-label-row">
							<view>
								<text class="coupon-label coupon-label--inline">使用有效期</text>
								<text class="coupon-hint coupon-hint--tight">自用户领取时起算</text>
							</view>
							<view class="coupon-never" :class="{ 'coupon-never--on': form.neverExpire }">
								<switch
									:checked="form.neverExpire"
									color="#007aff"
									@change="$emit('never-expire-change', $event)"
								/>
								<text class="coupon-never-text">领取后永久</text>
							</view>
						</view>
						<view v-if="!form.neverExpire" class="coupon-input-wrap coupon-input-wrap--suffix">
							<text class="coupon-input-prefix">领取后</text>
							<input
								v-model="form.validDays"
								class="coupon-input coupon-input--inset"
								type="number"
								placeholder="30"
								placeholder-class="coupon-placeholder"
							/>
							<text class="coupon-input-suffix">天内有效</text>
						</view>
						<view v-else class="coupon-picker coupon-picker--disabled">
							<text class="coupon-picker-text coupon-picker-text--muted">领取后永久有效，到期不会自动失效</text>
						</view>
					</view>
					<view v-if="!editingCouponId" class="coupon-field">
						<text class="coupon-label">发放数量</text>
						<text class="coupon-hint">不填表示无限发放</text>
						<view class="coupon-input-wrap coupon-input-wrap--suffix">
							<input
								v-model="form.totalQuantity"
								class="coupon-input coupon-input--inset"
								type="number"
								placeholder="不限"
								placeholder-class="coupon-placeholder"
							/>
							<text class="coupon-input-suffix">张</text>
						</view>
					</view>
					<view class="coupon-field">
						<text class="coupon-label">发放时间范围</text>
						<text class="coupon-hint">在此时间段内用户可领取；开始不填则立即开始，结束不填则永久有效</text>
						<view class="coupon-range-stack">
							<picker
								mode="date"
								:value="form.distributionStartDate"
								@change="form.distributionStartDate = $event.detail.value"
							>
								<view class="coupon-range-row">
									<text class="coupon-range-label">开始发放</text>
									<text
										class="coupon-range-value"
										:class="{ 'coupon-range-value--empty': !form.distributionStartDate }"
									>{{ form.distributionStartDate || '立即开始' }}</text>
									<text class="coupon-range-arrow">›</text>
								</view>
							</picker>
							<view class="coupon-range-line" />
							<picker
								mode="date"
								:value="form.distributionEndDate"
								@change="form.distributionEndDate = $event.detail.value"
							>
								<view class="coupon-range-row">
									<text class="coupon-range-label">结束发放</text>
									<text
										class="coupon-range-value"
										:class="{ 'coupon-range-value--empty': !form.distributionEndDate }"
									>{{ form.distributionEndDate || '永久有效' }}</text>
									<text class="coupon-range-arrow">›</text>
								</view>
							</picker>
						</view>
						<view v-if="form.distributionStartDate || form.distributionEndDate" class="coupon-date-actions">
							<text v-if="form.distributionStartDate" class="coupon-date-clear" @click="form.distributionStartDate = ''">清空开始</text>
							<text v-if="form.distributionEndDate" class="coupon-date-clear" @click="form.distributionEndDate = ''">清空结束</text>
						</view>
					</view>
				</view>

				<view class="coupon-sheet-footer">
					<view class="coupon-footer-btn coupon-footer-btn--ghost" @click="$emit('close')">取消</view>
					<view
						v-if="editingCouponId && canStopCouponDistribution(editingCouponItem)"
						class="coupon-footer-btn coupon-footer-btn--warn"
						@click="$emit('stop-distribution')"
					>
						停止发放
					</view>
					<view
						v-if="editingCouponId && canResumeCouponDistribution(editingCouponItem)"
						class="coupon-footer-btn coupon-footer-btn--resume"
						@click="$emit('resume-distribution')"
					>
						恢复发放
					</view>
					<view class="coupon-footer-btn coupon-footer-btn--primary" @click="$emit('submit')">
						{{ editingCouponId ? '保存修改' : '创建折扣券' }}
					</view>
				</view>
			</view>
		</scroll-view>
	</view>
</template>

<script setup>
import { computed } from 'vue'
import {
	canStopCouponDistribution,
	canResumeCouponDistribution,
	formatCouponRule,
	formatValidDays
} from '@/utils/coupon-manage-display.js'

const props = defineProps({
	visible: { type: Boolean, default: false },
	editingCouponId: { type: [String, Number], default: null },
	editingCouponItem: { type: Object, default: null },
	form: { type: Object, required: true },
	couponTypeLabels: { type: Array, default: () => [] },
	couponTypeIndex: { type: Number, default: 0 }
})

defineEmits([
	'close',
	'submit',
	'coupon-type-change',
	'never-expire-change',
	'stop-distribution',
	'resume-distribution'
])

const formatCouponRuleFromForm = computed(() =>
	formatCouponRule({
		type: props.form.type,
		thresholdAmount: props.form.thresholdAmount,
		discountValue: props.form.discountValue
	})
)

const formatValidDaysFromForm = computed(() => {
	if (props.form.neverExpire) return '领取后永久'
	return formatValidDays(props.form)
})
</script>

<style scoped>
.coupon-mask {
	position: fixed;
	inset: 0;
	z-index: 999;
	background: rgba(15, 23, 42, 0.52);
	display: flex;
	align-items: flex-end;
	justify-content: center;
	box-sizing: border-box;
}

.coupon-sheet-scroll {
	width: 100%;
	max-height: 92vh;
	box-sizing: border-box;
}

.coupon-sheet {
	width: 100%;
	background: #f4f6f9;
	border-radius: 20px 20px 0 0;
	padding: 0 16px calc(16px + env(safe-area-inset-bottom));
	box-sizing: border-box;
	overflow: hidden;
}

.coupon-sheet-header {
	display: flex;
	flex-direction: column;
	align-items: center;
	padding: 20px 8px 16px;
	background: linear-gradient(180deg, #fff 0%, #f4f6f9 100%);
}

.coupon-sheet-badge {
	width: 48px;
	height: 48px;
	border-radius: 14px;
	background: linear-gradient(135deg, #6eb5ff 0%, #007aff 100%);
	display: flex;
	align-items: center;
	justify-content: center;
	margin-bottom: 10px;
	box-shadow: 0 6px 16px rgba(0, 122, 255, 0.28);
}

.coupon-sheet-badge-text {
	font-size: 22px;
	font-weight: 700;
	color: #fff;
	line-height: 1;
}

.coupon-sheet-title {
	font-size: 18px;
	font-weight: 700;
	color: #1a1a1a;
	letter-spacing: 0.3px;
}

.coupon-sheet-desc {
	margin-top: 4px;
	font-size: 13px;
	color: #8a94a6;
}

.coupon-block {
	background: #fff;
	border-radius: 14px;
	padding: 14px 14px 4px;
	margin-bottom: 12px;
	box-shadow: 0 1px 4px rgba(15, 23, 42, 0.04);
}

.coupon-block-title {
	display: block;
	font-size: 13px;
	font-weight: 600;
	color: #007aff;
	margin-bottom: 12px;
	padding-left: 10px;
	position: relative;
}

.coupon-block-title::before {
	content: '';
	position: absolute;
	left: 0;
	top: 50%;
	transform: translateY(-50%);
	width: 3px;
	height: 14px;
	border-radius: 2px;
	background: linear-gradient(180deg, #6eb5ff, #007aff);
}

.coupon-field {
	margin-bottom: 14px;
}

.coupon-label {
	display: block;
	font-size: 14px;
	font-weight: 600;
	color: #2c3e50;
	margin-bottom: 2px;
}

.coupon-label--inline {
	margin-bottom: 0;
}

.coupon-hint {
	display: block;
	font-size: 12px;
	color: #9aa3b2;
	margin-bottom: 8px;
}

.coupon-hint--tight {
	margin-bottom: 0;
	margin-top: 2px;
}

.coupon-input-prefix {
	padding-left: 14px;
	font-size: 14px;
	color: #5c6678;
	flex-shrink: 0;
}

.coupon-label-row {
	display: flex;
	align-items: center;
	justify-content: space-between;
	margin-bottom: 8px;
}

.coupon-input {
	width: 100%;
	height: 44px;
	padding: 0 14px;
	box-sizing: border-box;
	background: #f7f9fc;
	border: 1px solid #e4e9f0;
	border-radius: 10px;
	font-size: 15px;
	color: #1a1a1a;
	transition: border-color 0.2s;
}

.coupon-input--inset {
	border: none;
	background: transparent;
	height: 44px;
	padding: 0 12px 0 14px;
	flex: 1;
	min-width: 0;
}

.coupon-placeholder {
	color: #b8c0cc;
	font-size: 15px;
}

.coupon-input-wrap {
	display: flex;
	align-items: center;
	background: #f7f9fc;
	border: 1px solid #e4e9f0;
	border-radius: 10px;
	overflow: hidden;
}

.coupon-input-wrap--suffix .coupon-input-suffix {
	padding-right: 14px;
	font-size: 14px;
	font-weight: 500;
	color: #007aff;
	flex-shrink: 0;
}

.coupon-picker {
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

.coupon-picker--disabled {
	background: #f0f3f7;
	border-color: #e8ecf0;
}

.coupon-picker-text {
	flex: 1;
	min-width: 0;
	font-size: 15px;
	color: #1a1a1a;
}

.coupon-picker-text--muted {
	color: #b8c0cc;
}

.coupon-picker-arrow {
	font-size: 20px;
	color: #c5cdd8;
	font-weight: 300;
	line-height: 1;
	margin-left: 8px;
}

.coupon-never {
	display: flex;
	align-items: center;
	gap: 4px;
	padding: 4px 10px 4px 6px;
	border-radius: 20px;
	background: #f0f3f7;
}

.coupon-never--on {
	background: rgba(0, 122, 255, 0.1);
}

.coupon-never-text {
	font-size: 12px;
	color: #666;
	font-weight: 500;
}

.coupon-never--on .coupon-never-text {
	color: #007aff;
}

.coupon-range-stack {
	background: #f7f9fc;
	border: 1px solid #e4e9f0;
	border-radius: 10px;
	overflow: hidden;
}

.coupon-range-row {
	display: flex;
	align-items: center;
	height: 48px;
	padding: 0 14px;
	box-sizing: border-box;
}

.coupon-range-label {
	font-size: 14px;
	color: #5c6678;
	flex-shrink: 0;
	width: 72px;
}

.coupon-range-value {
	flex: 1;
	min-width: 0;
	text-align: right;
	font-size: 15px;
	color: #1a1a1a;
}

.coupon-range-value--empty {
	color: #b8c0cc;
}

.coupon-range-arrow {
	font-size: 20px;
	color: #c5cdd8;
	font-weight: 300;
	margin-left: 8px;
	flex-shrink: 0;
}

.coupon-range-line {
	height: 1px;
	background: #e4e9f0;
	margin: 0 14px;
}

.coupon-date-actions {
	display: flex;
	gap: 16px;
	margin-top: 8px;
}

.coupon-date-clear {
	font-size: 12px;
	color: #007aff;
}

.coupon-sheet-footer {
	display: flex;
	gap: 12px;
	padding: 4px 0 8px;
}

.coupon-footer-btn {
	flex: 1;
	height: 46px;
	line-height: 46px;
	text-align: center;
	border-radius: 12px;
	font-size: 15px;
	font-weight: 600;
	box-sizing: border-box;
}

.coupon-footer-btn--ghost {
	background: #fff;
	color: #5c6678;
	border: 1px solid #e4e9f0;
}

.coupon-footer-btn--primary {
	background: linear-gradient(135deg, #6eb5ff 0%, #007aff 100%);
	color: #fff;
	box-shadow: 0 4px 14px rgba(0, 122, 255, 0.32);
}

.coupon-footer-btn--warn {
	background: #fff4f0;
	color: #e65c00;
	border: 1px solid #ffd4c2;
}

.coupon-footer-btn--resume {
	background: #f0faf4;
	color: #1a9a4a;
	border: 1px solid #b8e6c8;
}

.coupon-readonly-card {
	background: #f7f9fc;
	border-radius: 10px;
	padding: 12px 14px;
	border: 1px solid #e8edf3;
}

.coupon-readonly-name {
	display: block;
	font-size: 15px;
	font-weight: 600;
	color: #222;
	margin-bottom: 6px;
}

.coupon-readonly-tag {
	display: inline-block;
	font-size: 11px;
	color: #007aff;
	background: rgba(0, 122, 255, 0.1);
	padding: 2px 6px;
	border-radius: 4px;
	margin-bottom: 8px;
}

.coupon-readonly-rule {
	display: block;
	font-size: 14px;
	color: #e65c00;
	margin-bottom: 4px;
}

.coupon-readonly-meta {
	display: block;
	font-size: 12px;
	color: #888;
}
</style>
