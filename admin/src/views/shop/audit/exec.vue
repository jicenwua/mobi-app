<template>
  <div class="app-container" v-loading="loading">
    <el-page-header @back="router.back()" content="店铺审核" class="mb16" />
    <template v-if="shop">
      <el-descriptions title="店铺资料" :column="2" border>
        <el-descriptions-item label="店铺ID">{{ shop.id }}</el-descriptions-item>
        <el-descriptions-item label="店铺名称">{{ shop.shopName }}</el-descriptions-item>
        <el-descriptions-item label="店铺编码">{{ shop.shopCode }}</el-descriptions-item>
        <el-descriptions-item label="类目ID">{{ shop.categoryId ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="省市区">{{ [shop.province, shop.city, shop.district].filter(Boolean).join(' / ') || '—' }}</el-descriptions-item>
        <el-descriptions-item label="详细地址">{{ shop.address || '—' }}</el-descriptions-item>
        <el-descriptions-item label="法人">{{ shop.legalPerson || '—' }}</el-descriptions-item>
        <el-descriptions-item label="身份证号">{{ shop.idCardNo || '—' }}</el-descriptions-item>
        <el-descriptions-item label="统一社会信用代码">{{ shop.businessLicenseNo || '—' }}</el-descriptions-item>
        <el-descriptions-item label="当前审核状态">{{ auditLabel(shop.auditStatus) }}</el-descriptions-item>
        <el-descriptions-item label="驳回原因" :span="2">{{ shop.auditReason || '—' }}</el-descriptions-item>
      </el-descriptions>

      <h3 class="mt24">证件与门店照片</h3>
      <el-row :gutter="16" class="img-row">
        <el-col v-for="item in imageFields" :key="item.key" :span="8">
          <div class="img-card">
            <div class="img-title">{{ item.label }}</div>
            <el-image
              v-if="imgUrl(item.key)"
              :src="imgUrl(item.key)"
              fit="contain"
              style="width: 100%; height: 160px"
            />
            <div v-else class="img-empty">无</div>
          </div>
        </el-col>
      </el-row>

      <el-divider />
      <h3>审核处理</h3>
      <el-form :model="auditForm" label-width="100px" style="max-width: 520px">
        <el-form-item label="审核结果" required>
          <el-radio-group v-model="auditForm.auditStatus">
            <el-radio :label="1">通过</el-radio>
            <el-radio :label="2">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="驳回原因" v-if="auditForm.auditStatus === 2" required>
          <el-input v-model="auditForm.auditReason" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="请填写驳回原因" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="submitting" @click="submitAudit" v-hasPermi="['system:shop:audit']">提交审核</el-button>
        </el-form-item>
      </el-form>
    </template>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getShop, auditShop } from '@/api/shop/shop'
import { parseOptionalPositiveInt } from '@/utils/shopRoute'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const shop = ref(null)

const shopId = computed(() => parseOptionalPositiveInt(route.params.id))

const imageFields = [
  { key: 'idCardFront', label: '身份证正面' },
  { key: 'idCardBack', label: '身份证背面' },
  { key: 'businessLicensePic', label: '营业执照' },
  { key: 'shopExterior', label: '店铺外景' },
  { key: 'shopInterior', label: '店铺内景' }
]

const auditForm = reactive({
  auditStatus: 1,
  auditReason: ''
})

function auditLabel(s) {
  if (s === 0) return '待审核'
  if (s === 1) return '已通过'
  if (s === 2) return '已驳回'
  return '—'
}

function imgUrl(key) {
  const u = shop.value?.[key]
  if (!u || typeof u !== 'string') return ''
  const t = u.trim()
  return t.startsWith('http://') || t.startsWith('https://') ? t : ''
}

/** 加载待审店铺资料 */
async function load() {
  loading.value = true
  try {
    const res = await getShop(shopId.value)
    shop.value = res.data || null
    if (!shop.value) {
      ElMessage.error('店铺不存在')
      return
    }
    auditForm.auditStatus = 1
    auditForm.auditReason = ''
  } finally {
    loading.value = false
  }
}

/** 提交审核结果 */
async function submitAudit() {
  if (auditForm.auditStatus === 2 && !auditForm.auditReason?.trim()) {
    ElMessage.warning('驳回时请填写原因')
    return
  }
  submitting.value = true
  try {
    await auditShop({
      id: shopId.value,
      auditStatus: auditForm.auditStatus,
      auditReason: auditForm.auditStatus === 2 ? auditForm.auditReason.trim() : undefined
    })
    ElMessage.success('已保存')
    router.push('/shop/audit')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  load()
})
</script>

<style scoped>
.app-container {
  padding: 20px;
}
.mb16 {
  margin-bottom: 16px;
}
.mt24 {
  margin-top: 24px;
}
.img-row {
  margin-top: 12px;
}
.img-card {
  border: 1px solid var(--el-border-color);
  border-radius: 6px;
  padding: 8px;
  margin-bottom: 12px;
}
.img-title {
  font-size: 13px;
  margin-bottom: 6px;
  color: var(--el-text-color-secondary);
}
.img-empty {
  height: 160px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--el-fill-color-light);
  color: var(--el-text-color-placeholder);
}
</style>
