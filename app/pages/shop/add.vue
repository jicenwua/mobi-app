<template>
	<view class="page">
		<view class="hero">
			<view class="hero-badge">
				<view class="hero-icon-ring">
					<uni-icons
						:type="submitted ? 'checkbox-filled' : 'shop-filled'"
						:size="26"
						:color="submitted ? '#34c759' : '#007aff'"
					/>
				</view>
			</view>
			<text class="hero-title">{{ submitted ? '提交成功' : '开店申请' }}</text>
			<text class="hero-desc">{{ heroDesc }}</text>
			<view class="hero-steps">
				<template v-for="(step, index) in steps" :key="step.key">
					<view v-if="index > 0" class="hero-step-line" :class="{ 'hero-step-line--done': submitted || currentStep > index }" />
					<view
						class="hero-step"
						:class="{
							'hero-step--active': !submitted && currentStep === index + 1,
							'hero-step--done': submitted || currentStep > index + 1
						}"
					>
						<text class="hero-step-num">{{ submitted || currentStep > index + 1 ? '✓' : index + 1 }}</text>
						<text class="hero-step-text">{{ step.label }}</text>
					</view>
				</template>
			</view>
		</view>

		<view class="form-body">
			<!-- 步骤 1：填写信息 -->
			<template v-if="currentStep === 1 && !submitted">
				<view class="card">
					<view class="card-head">
						<view class="card-head-dot" />
						<text class="card-title">基本信息</text>
					</view>
					<view class="field">
						<view class="label-row">
							<text class="label">店铺名称</text>
							<text class="req">*</text>
						</view>
						<input
							v-model="form.shopName"
							class="field-input"
							placeholder="请输入店铺名称"
							placeholder-class="field-placeholder"
							maxlength="100"
						/>
					</view>
					<view class="field">
						<view class="label-row">
							<text class="label">行业类目</text>
							<text class="req">*</text>
						</view>
						<picker :range="categoryNames" :value="categoryIndex" @change="onCategoryChange">
							<view class="field-picker">
								<text :class="categoryIndex >= 0 ? 'field-picker-text' : 'field-picker-placeholder'">
									{{ categoryNames[categoryIndex] || '请选择行业类目' }}
								</text>
								<uni-icons type="right" :size="14" color="#c0c4cc" />
							</view>
						</picker>
					</view>
					<view class="field">
						<view class="label-row">
							<text class="label">上级店铺代码</text>
							<text class="label-opt">选填</text>
						</view>
						<input
							v-model="form.parentShopCode"
							class="field-input"
							placeholder="已审核通过的总店代码，留空为独立店"
							placeholder-class="field-placeholder"
							maxlength="64"
						/>
					</view>
					<view class="field">
						<view class="label-row">
							<text class="label">联系电话</text>
							<text class="req">*</text>
						</view>
						<input
							v-model="form.phone"
							class="field-input"
							type="number"
							placeholder="11 位手机号，便于审核联系"
							placeholder-class="field-placeholder"
							maxlength="11"
						/>
					</view>
					<view class="field field--last">
						<view class="label-row">
							<text class="label">充值积分比率</text>
							<text class="req">*</text>
						</view>
						<input
							v-model="form.ratio"
							class="field-input"
							type="number"
							placeholder="如 10 表示充值 1 元得 10 积分"
							placeholder-class="field-placeholder"
						/>
						<text class="field-hint">充值金额（元）× 比率 = 所得积分，创建后不可修改</text>
					</view>
				</view>

				<view class="card">
					<view class="card-head">
						<view class="card-head-dot" />
						<text class="card-title">地址</text>
					</view>
					<view class="field-row">
						<view class="field field--half">
							<view class="label-row">
								<text class="label">省</text>
								<text class="req">*</text>
							</view>
							<input
								v-model="form.province"
								class="field-input"
								placeholder="省份"
								placeholder-class="field-placeholder"
							/>
						</view>
						<view class="field field--half">
							<view class="label-row">
								<text class="label">市</text>
								<text class="req">*</text>
							</view>
							<input
								v-model="form.city"
								class="field-input"
								placeholder="城市"
								placeholder-class="field-placeholder"
							/>
						</view>
					</view>
					<view class="field">
						<view class="label-row">
							<text class="label">区/县</text>
							<text class="req">*</text>
						</view>
						<input
							v-model="form.district"
							class="field-input"
							placeholder="区或县"
							placeholder-class="field-placeholder"
						/>
					</view>
					<view class="field field--last">
						<view class="label-row">
							<text class="label">详细地址</text>
							<text class="req">*</text>
						</view>
						<textarea
							v-model="form.address"
							class="field-textarea"
							placeholder="街道、门牌号等详细地址"
							placeholder-class="field-placeholder"
							maxlength="255"
							:auto-height="false"
						/>
					</view>
				</view>
			</template>

			<!-- 步骤 2：上传资质 -->
			<template v-else-if="currentStep === 2 && !submitted">
				<view class="card">
					<view class="card-head">
						<view class="card-head-dot" />
						<text class="card-title">资质信息</text>
					</view>
					<view class="field">
						<view class="label-row">
							<text class="label">法人姓名</text>
							<text class="req">*</text>
						</view>
						<input
							v-model="form.legalPerson"
							class="field-input"
							placeholder="须与身份证姓名一致"
							placeholder-class="field-placeholder"
						/>
					</view>
					<view class="field">
						<view class="label-row">
							<text class="label">身份证号</text>
							<text class="req">*</text>
						</view>
						<input
							v-model="form.idCardNo"
							class="field-input"
							placeholder="18 位身份证号码"
							placeholder-class="field-placeholder"
							maxlength="18"
						/>
					</view>
					<view class="field field--last">
						<view class="label-row">
							<text class="label">统一社会信用代码</text>
							<text class="req">*</text>
						</view>
						<input
							v-model="form.businessLicenseNo"
							class="field-input"
							placeholder="营业执照上的统一社会信用代码"
							placeholder-class="field-placeholder"
							maxlength="64"
						/>
					</view>
				</view>

				<view class="card">
					<view class="card-head">
						<view class="card-head-dot" />
						<text class="card-title">证照与门店图片</text>
						<text class="req">*</text>
					</view>
					<text class="card-hint">以下图片均为必填，请确保清晰可辨</text>
					<view v-for="item in imageFields" :key="item.key" class="upload-block">
						<view class="label-row">
							<text class="label">{{ item.label }}</text>
							<text class="req">*</text>
						</view>
						<view class="upload-grid">
							<view
								v-if="item.key !== 'carouselImages' && filePaths[item.key]"
								class="upload-thumb"
								@click="previewImage(filePaths[item.key])"
							>
								<image :src="filePaths[item.key]" mode="aspectFill" class="upload-img" />
								<view class="upload-del" @click.stop="clearImage(item.key)">×</view>
							</view>
							<template v-else-if="item.key === 'carouselImages'">
								<view
									v-if="filePaths.carouselImages"
									class="upload-thumb"
									@click="previewImage(filePaths.carouselImages)"
								>
									<image :src="filePaths.carouselImages" mode="aspectFill" class="upload-img" />
									<view class="upload-del" @click.stop="clearImage('carouselImages')">×</view>
								</view>
								<view
									v-for="(extra, ei) in filePaths.carouselImages_extra || []"
									:key="'c-' + ei"
									class="upload-thumb"
									@click="previewImage(extra)"
								>
									<image :src="extra" mode="aspectFill" class="upload-img" />
									<view class="upload-del" @click.stop="removeCarouselExtra(ei)">×</view>
								</view>
							</template>
							<view
								v-if="showUploadAdd(item)"
								class="upload-add"
								hover-class="tap-hover-upload"
								:hover-stay-time="70"
								@click="pickImage(item.key, item.multiple, item.max)"
							>
								<uni-icons type="plusempty" :size="22" color="#8fa3bf" />
								<text class="upload-add-tip">上传</text>
							</view>
						</view>
					</view>
				</view>
			</template>

			<!-- 步骤 3：确认提交 / 等待审核 -->
			<template v-else-if="currentStep === 3">
				<view v-if="!submitted" class="card">
					<view class="card-head">
						<view class="card-head-dot" />
						<text class="card-title">确认信息</text>
					</view>
					<text class="card-hint">请核对以下信息，确认无误后提交审核</text>
					<view class="summary-list">
						<view class="summary-row">
							<text class="summary-label">店铺名称</text>
							<text class="summary-value">{{ form.shopName.trim() }}</text>
						</view>
						<view class="summary-row">
							<text class="summary-label">行业类目</text>
							<text class="summary-value">{{ categoryNames[categoryIndex] }}</text>
						</view>
						<view v-if="(form.parentShopCode || '').trim()" class="summary-row">
							<text class="summary-label">上级店铺</text>
							<text class="summary-value">{{ form.parentShopCode.trim() }}</text>
						</view>
						<view class="summary-row">
							<text class="summary-label">联系电话</text>
							<text class="summary-value">{{ form.phone.trim() }}</text>
						</view>
						<view class="summary-row">
							<text class="summary-label">积分比率</text>
							<text class="summary-value">1 元 = {{ form.ratio }} 积分</text>
						</view>
						<view class="summary-row summary-row--block">
							<text class="summary-label">店铺地址</text>
							<text class="summary-value">{{ fullAddress }}</text>
						</view>
						<view class="summary-row">
							<text class="summary-label">法人姓名</text>
							<text class="summary-value">{{ form.legalPerson.trim() }}</text>
						</view>
						<view class="summary-row">
							<text class="summary-label">证照图片</text>
							<text class="summary-value">已上传 {{ uploadedImageCount }} 张</text>
						</view>
					</view>
				</view>

				<view v-else class="card card--success">
					<view class="success-icon-wrap">
						<uni-icons type="checkbox-filled" :size="48" color="#34c759" />
					</view>
					<text class="success-title">申请已提交</text>
					<text class="success-desc">您的开店申请已进入审核流程，请耐心等待。审核结果将通过小程序通知您。</text>
					<view class="success-tips">
						<text class="success-tip-item">· 审核通常需要 1～3 个工作日</text>
						<text class="success-tip-item">· 请保持联系电话畅通</text>
						<text class="success-tip-item">· 可在会员页查看审核进度</text>
					</view>
				</view>
			</template>

			<view class="bottom-spacer" />
		</view>

		<view class="footer-bar" :style="footerSafeStyle">
			<template v-if="submitted">
				<button class="submit-btn" @click="goBackHome">返回会员页</button>
			</template>
			<template v-else-if="currentStep === 1">
				<button class="submit-btn" @click="goNext">下一步</button>
			</template>
			<template v-else-if="currentStep === 2">
				<view class="footer-actions">
					<button class="footer-btn footer-btn--ghost" @click="goPrev">上一步</button>
					<button class="footer-btn footer-btn--primary" @click="goNext">下一步</button>
				</view>
			</template>
			<template v-else>
				<view class="footer-actions">
					<button class="footer-btn footer-btn--ghost" :disabled="submitting" @click="goPrev">上一步</button>
					<button
						class="footer-btn footer-btn--primary"
						:loading="submitting"
						:disabled="submitting"
						@click="onSubmit"
					>
						{{ submitting ? '提交中…' : '提交申请' }}
					</button>
				</view>
			</template>
		</view>
	</view>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { WX_PERM } from '@/api/constants/customer.js'
import { hasPermission } from '@/utils/permissions.js'
import { navigateBackDelayed } from '@/utils/navigation.js'

const steps = [
	{ key: 'info', label: '填写信息' },
	{ key: 'upload', label: '上传资质' },
	{ key: 'review', label: '等待审核' }
]

onLoad(() => {
	if (!hasPermission(WX_PERM.SHOP_ADD)) {
		uni.showToast({ title: '无添加店铺权限', icon: 'none' })
		navigateBackDelayed(800, 1)
	}
})

const categories = [
	{ id: 1, name: '餐饮' },
	{ id: 2, name: '零售' },
	{ id: 3, name: '服务' },
	{ id: 4, name: '其他' }
]

const categoryNames = categories.map((c) => c.name)
const categoryIndex = ref(0)
const currentStep = ref(1)
const submitting = ref(false)
const submitted = ref(false)

const footerSafeStyle = computed(() => {
	const bottom = uni.getSystemInfoSync().safeAreaInsets?.bottom || 0
	return { paddingBottom: `${Math.max(bottom, 12)}px` }
})

const heroDesc = computed(() => {
	if (submitted.value) return '申请已提交，请耐心等待审核结果'
	if (currentStep.value === 1) return '填写店铺基本信息与经营地址'
	if (currentStep.value === 2) return '填写资质信息并上传证照、门店实景照片'
	return '核对信息无误后提交，进入审核流程'
})

const fullAddress = computed(() => {
	return [form.province, form.city, form.district, form.address]
		.map((s) => (s || '').trim())
		.filter(Boolean)
		.join('')
})

const form = reactive({
	parentShopCode: '',
	shopName: '',
	phone: '',
	province: '',
	city: '',
	district: '',
	address: '',
	legalPerson: '',
	idCardNo: '',
	businessLicenseNo: '',
	ratio: ''
})

const filePaths = reactive({
	idCardFront: '',
	idCardBack: '',
	businessLicensePic: '',
	shopExterior: '',
	shopInterior: '',
	carouselImages: '',
	carouselImages_extra: []
})

const imageFields = [
	{ key: 'idCardFront', label: '身份证正面', required: true },
	{ key: 'idCardBack', label: '身份证背面', required: true },
	{ key: 'businessLicensePic', label: '营业执照', required: true },
	{ key: 'shopExterior', label: '店铺外景', required: true },
	{ key: 'shopInterior', label: '店铺内景', required: true },
	{ key: 'carouselImages', label: '轮播图', required: true, multiple: true, max: 3 }
]

const carouselCount = computed(() => {
	let n = filePaths.carouselImages ? 1 : 0
	n += (filePaths.carouselImages_extra || []).length
	return n
})

const uploadedImageCount = computed(() => {
	let n = carouselCount.value
	for (const item of imageFields) {
		if (item.key !== 'carouselImages' && filePaths[item.key]) n += 1
	}
	return n
})

function showUploadAdd(item) {
	if (item.key === 'carouselImages') {
		return carouselCount.value < item.max
	}
	return !filePaths[item.key]
}

function onCategoryChange(e) {
	categoryIndex.value = Number(e.detail.value) || 0
}

function scrollToTop() {
	uni.pageScrollTo({ scrollTop: 0, duration: 0 })
}

function goNext() {
	if (currentStep.value === 1) {
		const err = validateStep1()
		if (err) {
			uni.showToast({ title: err, icon: 'none' })
			return
		}
		currentStep.value = 2
	} else if (currentStep.value === 2) {
		const err = validateStep2()
		if (err) {
			uni.showToast({ title: err, icon: 'none' })
			return
		}
		currentStep.value = 3
	}
	scrollToTop()
}

function goPrev() {
	if (currentStep.value <= 1 || submitting.value) return
	currentStep.value -= 1
	scrollToTop()
}

function goBackHome() {
	uni.navigateBack()
}

function pickImage(key, multiple, max) {
	const remain = multiple ? max - carouselCount.value : 1
	if (remain <= 0) return
	uni.chooseImage({
		count: multiple ? remain : 1,
		sizeType: ['compressed'],
		sourceType: ['album', 'camera'],
		success: (res) => {
			const paths = res.tempFilePaths || []
			if (!paths.length) return
			if (key === 'carouselImages') {
				if (!filePaths.carouselImages) {
					filePaths.carouselImages = paths[0]
					paths.slice(1).forEach((p) => filePaths.carouselImages_extra.push(p))
				} else {
					paths.forEach((p) => {
						if (carouselCount.value < max) filePaths.carouselImages_extra.push(p)
					})
				}
				return
			}
			filePaths[key] = paths[0]
		}
	})
}

function clearImage(key) {
	if (key === 'carouselImages') {
		const extras = filePaths.carouselImages_extra || []
		if (extras.length) {
			filePaths.carouselImages = extras.shift()
			filePaths.carouselImages_extra = extras
		} else {
			filePaths.carouselImages = ''
			filePaths.carouselImages_extra = []
		}
		return
	}
	filePaths[key] = ''
}

function removeCarouselExtra(index) {
	filePaths.carouselImages_extra.splice(index, 1)
}

function previewImage(src) {
	if (!src) return
	uni.previewImage({ urls: [src], current: src })
}

function validateStep1() {
	if (!(form.shopName || '').trim()) return '请输入店铺名称'
	const phone = (form.phone || '').trim()
	if (!phone) return '请输入联系电话'
	if (!/^1[3-9]\d{9}$/.test(phone)) return '请输入正确的 11 位手机号'
	if (!(form.province || '').trim()) return '请输入省份'
	if (!(form.city || '').trim()) return '请输入城市'
	if (!(form.district || '').trim()) return '请输入区/县'
	if (!(form.address || '').trim()) return '请输入详细地址'
	const ratio = parseInt(String(form.ratio || '').trim(), 10)
	if (!Number.isFinite(ratio) || ratio < 1 || ratio > 100000) {
		return '充值积分比率须为 1～100000 的正整数'
	}
	return ''
}

function validateStep2() {
	if (!(form.legalPerson || '').trim()) return '请输入法人姓名'
	if (!(form.idCardNo || '').trim()) return '请输入身份证号'
	if (!(form.businessLicenseNo || '').trim()) return '请输入统一社会信用代码'
	for (const item of imageFields) {
		if (item.key === 'carouselImages') {
			if (carouselCount.value < 1) return '请至少上传 1 张轮播图'
			continue
		}
		if (item.required && !filePaths[item.key]) return `请上传${item.label}`
	}
	return ''
}

async function onSubmit() {
	const err = validateStep1() || validateStep2()
	if (err) {
		uni.showToast({ title: err, icon: 'none' })
		return
	}

	const carouselImages = [filePaths.carouselImages, ...(filePaths.carouselImages_extra || [])].filter(
		Boolean
	)

	const ratioVal = parseInt(String(form.ratio || '').trim(), 10)

	const meta = {
		parentShopCode: (form.parentShopCode || '').trim() || undefined,
		shopName: form.shopName.trim(),
		phone: (form.phone || '').trim(),
		ratio: ratioVal,
		categoryId: categories[categoryIndex.value]?.id,
		province: form.province.trim(),
		city: form.city.trim(),
		district: form.district.trim(),
		address: form.address.trim(),
		legalPerson: form.legalPerson.trim(),
		idCardNo: form.idCardNo.trim(),
		businessLicenseNo: form.businessLicenseNo.trim()
	}

	submitting.value = true
	try {
		const { submitShopAddFull } = await import('@/api/modules/shop.js')
		const result = await submitShopAddFull(meta, {
			idCardFront: filePaths.idCardFront,
			idCardBack: filePaths.idCardBack,
			businessLicensePic: filePaths.businessLicensePic,
			shopExterior: filePaths.shopExterior,
			shopInterior: filePaths.shopInterior,
			carouselImages
		})

		if (!result.ok) {
			uni.showToast({ title: result.msg || '提交失败', icon: 'none', duration: 2800 })
			return
		}

		submitted.value = true
		scrollToTop()
	} finally {
		submitting.value = false
	}
}
</script>

<style scoped>
.page {
	min-height: 100vh;
	background-color: #f5f7fa;
	box-sizing: border-box;
}

.hero {
	margin: 0 0 4px;
	padding: 20px 20px 18px;
	background: linear-gradient(160deg, #e8f3ff 0%, #f5f9ff 48%, #f5f7fa 100%);
}

.hero-badge {
	display: flex;
	justify-content: center;
	margin-bottom: 12px;
}

.hero-icon-ring {
	width: 56px;
	height: 56px;
	border-radius: 18px;
	background: #ffffff;
	display: flex;
	align-items: center;
	justify-content: center;
	box-shadow: 0 6px 20px rgba(0, 122, 255, 0.12);
}

.hero-title {
	display: block;
	text-align: center;
	font-size: 20px;
	font-weight: 600;
	color: #1a1a1a;
	line-height: 1.35;
}

.hero-desc {
	display: block;
	margin-top: 6px;
	text-align: center;
	font-size: 13px;
	color: #6b7a8f;
	line-height: 1.55;
	padding: 0 8px;
}

.hero-steps {
	display: flex;
	align-items: center;
	justify-content: center;
	margin-top: 16px;
	padding: 0 4px;
}

.hero-step {
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: 4px;
	min-width: 56px;
	flex-shrink: 0;
}

.hero-step-num {
	width: 22px;
	height: 22px;
	border-radius: 11px;
	font-size: 11px;
	font-weight: 600;
	line-height: 22px;
	text-align: center;
	color: #8fa3bf;
	background: rgba(255, 255, 255, 0.85);
}

.hero-step--active .hero-step-num {
	color: #ffffff;
	background: #007aff;
	font-size: 12px;
}

.hero-step--done .hero-step-num {
	color: #ffffff;
	background: #34c759;
	font-size: 11px;
}

.hero-step-text {
	font-size: 11px;
	color: #8fa3bf;
	line-height: 1.2;
	white-space: nowrap;
}

.hero-step--active .hero-step-text {
	color: #007aff;
	font-weight: 500;
}

.hero-step--done .hero-step-text {
	color: #34c759;
	font-weight: 500;
}

.hero-step-line {
	flex: 1;
	max-width: 36px;
	height: 1px;
	margin: 0 4px 14px;
	background: #dce8f8;
}

.hero-step-line--done {
	background: linear-gradient(90deg, #34c759, #5dd879);
}

.form-body {
	padding: 0 16px;
}

.card {
	background: #ffffff;
	border-radius: 16px;
	padding: 16px 16px 4px;
	margin-bottom: 12px;
	box-shadow: 0 2px 12px rgba(15, 35, 70, 0.04);
}

.card--success {
	padding: 28px 20px 24px;
	text-align: center;
}

.card-head {
	display: flex;
	align-items: center;
	gap: 8px;
	margin-bottom: 14px;
}

.card-head-dot {
	width: 4px;
	height: 14px;
	border-radius: 2px;
	background: linear-gradient(180deg, #007aff, #5ac8fa);
	flex-shrink: 0;
}

.card-title {
	font-size: 15px;
	font-weight: 600;
	color: #1a1a1a;
	line-height: 1.4;
}

.card-hint {
	display: block;
	margin: -6px 0 14px;
	font-size: 12px;
	color: #8fa3bf;
	line-height: 1.5;
}

.field {
	margin-bottom: 14px;
}

.field--last {
	margin-bottom: 12px;
}

.field-row {
	display: flex;
	gap: 10px;
}

.field--half {
	flex: 1;
	min-width: 0;
}

.label-row {
	display: flex;
	align-items: center;
	gap: 3px;
	margin-bottom: 7px;
}

.label {
	font-size: 13px;
	color: #4a5568;
	line-height: 1.4;
	font-weight: 500;
}

.req {
	font-size: 12px;
	color: #ff6b6b;
	line-height: 1.4;
}

.label-opt {
	font-size: 11px;
	color: #a0aec0;
	line-height: 1.4;
	margin-left: 2px;
}

.field-hint {
	display: block;
	margin-top: 6px;
	font-size: 11px;
	color: #8fa3bf;
	line-height: 1.45;
}

.field-input {
	display: block;
	width: 100%;
	height: 44px;
	padding: 0 12px;
	box-sizing: border-box;
	font-size: 14px;
	color: #1a1a1a;
	background-color: #f8fafc;
	border: 1px solid #e8edf3;
	border-radius: 10px;
}

.field-placeholder {
	color: #b8c2d0;
	font-size: 14px;
}

.field-picker {
	display: flex;
	align-items: center;
	justify-content: space-between;
	width: 100%;
	height: 44px;
	padding: 0 12px;
	box-sizing: border-box;
	background-color: #f8fafc;
	border: 1px solid #e8edf3;
	border-radius: 10px;
}

.field-picker-text {
	font-size: 14px;
	color: #1a1a1a;
	line-height: 1.4;
	flex: 1;
}

.field-picker-placeholder {
	font-size: 14px;
	color: #b8c2d0;
	line-height: 1.4;
	flex: 1;
}

.field-textarea {
	display: block;
	width: 100%;
	min-height: 88px;
	padding: 10px 12px;
	box-sizing: border-box;
	font-size: 14px;
	line-height: 1.5;
	color: #1a1a1a;
	background-color: #f8fafc;
	border: 1px solid #e8edf3;
	border-radius: 10px;
}

.summary-list {
	margin-bottom: 12px;
}

.summary-row {
	display: flex;
	align-items: flex-start;
	justify-content: space-between;
	gap: 12px;
	padding: 11px 0;
	border-bottom: 1px solid #f0f3f7;
}

.summary-row:last-child {
	border-bottom: none;
}

.summary-row--block {
	flex-direction: column;
	gap: 6px;
}

.summary-label {
	font-size: 13px;
	color: #8fa3bf;
	flex-shrink: 0;
	line-height: 1.4;
}

.summary-value {
	font-size: 14px;
	color: #1a1a1a;
	text-align: right;
	flex: 1;
	line-height: 1.45;
	word-break: break-all;
}

.summary-row--block .summary-value {
	text-align: left;
}

.success-icon-wrap {
	margin-bottom: 12px;
}

.success-title {
	display: block;
	font-size: 18px;
	font-weight: 600;
	color: #1a1a1a;
	margin-bottom: 8px;
}

.success-desc {
	display: block;
	font-size: 13px;
	color: #6b7a8f;
	line-height: 1.6;
	margin-bottom: 16px;
}

.success-tips {
	display: flex;
	flex-direction: column;
	gap: 6px;
	padding: 12px 14px;
	background: #f8fafc;
	border-radius: 10px;
	text-align: left;
}

.success-tip-item {
	font-size: 12px;
	color: #8fa3bf;
	line-height: 1.5;
}

.upload-block {
	margin-bottom: 16px;
}

.upload-block:last-child {
	margin-bottom: 8px;
}

.upload-grid {
	display: flex;
	flex-wrap: wrap;
	gap: 10px;
}

.upload-add,
.upload-thumb {
	width: 88px;
	height: 88px;
	border-radius: 12px;
	position: relative;
	overflow: hidden;
	flex-shrink: 0;
}

.upload-add {
	background: #f8fafc;
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	border: 1px dashed #c5d4e8;
	gap: 4px;
}

.upload-add-tip {
	font-size: 11px;
	color: #8fa3bf;
	line-height: 1;
}

.upload-img {
	width: 100%;
	height: 100%;
}

.upload-del {
	position: absolute;
	top: 4px;
	right: 4px;
	width: 20px;
	height: 20px;
	background: rgba(0, 0, 0, 0.52);
	color: #fff;
	border-radius: 50%;
	text-align: center;
	line-height: 20px;
	font-size: 14px;
}

.footer-bar {
	position: fixed;
	left: 0;
	right: 0;
	bottom: 0;
	z-index: 10;
	padding: 10px 16px 0;
	background: linear-gradient(180deg, rgba(245, 247, 250, 0) 0%, #f5f7fa 28%);
	box-sizing: border-box;
}

.footer-actions {
	display: flex;
	gap: 10px;
}

.footer-btn {
	flex: 1;
	height: 46px;
	line-height: 46px;
	border-radius: 23px;
	font-size: 15px;
	font-weight: 500;
	border: none;
}

.footer-btn::after {
	border: none;
}

.footer-btn--ghost {
	color: #007aff;
	background: #ffffff;
	border: 1px solid rgba(0, 122, 255, 0.25);
	box-shadow: 0 2px 8px rgba(15, 35, 70, 0.04);
}

.footer-btn--primary {
	color: #ffffff;
	background: linear-gradient(135deg, #007aff 0%, #0a84ff 100%);
	box-shadow: 0 4px 14px rgba(0, 122, 255, 0.28);
}

.footer-btn[disabled] {
	opacity: 0.65;
}

.submit-btn {
	width: 100%;
	height: 46px;
	line-height: 46px;
	border-radius: 23px;
	font-size: 15px;
	font-weight: 500;
	color: #ffffff;
	background: linear-gradient(135deg, #007aff 0%, #0a84ff 100%);
	border: none;
	box-shadow: 0 4px 14px rgba(0, 122, 255, 0.28);
}

.submit-btn::after {
	border: none;
}

.bottom-spacer {
	height: 88px;
}
</style>
