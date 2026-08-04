<template>
  <div class="app-container" v-loading="loading">
    <el-page-header @back="router.back()" content="店铺详情" class="mb16" />
    <template v-if="shop">
      <el-descriptions title="基本信息" :column="2" border>
        <el-descriptions-item label="店铺ID">{{ shop.id }}</el-descriptions-item>
        <el-descriptions-item label="父店铺ID">{{ shop.parentId ?? 0 }}</el-descriptions-item>
        <el-descriptions-item label="店铺名称">{{ shop.shopName }}</el-descriptions-item>
        <el-descriptions-item label="店铺编码">{{ shop.shopCode }}</el-descriptions-item>
        <el-descriptions-item label="充值积分比率">
          {{ shop.ratio != null ? `1 元 = ${shop.ratio} 积分` : '—' }}
        </el-descriptions-item>
        <el-descriptions-item label="行业类目">{{ shop.categoryId ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="省市区">{{ [shop.province, shop.city, shop.district].filter(Boolean).join(' / ') || '—' }}</el-descriptions-item>
        <el-descriptions-item label="详细地址" :span="2">{{ shop.address || '—' }}</el-descriptions-item>
        <el-descriptions-item label="法人">{{ shop.legalPerson || '—' }}</el-descriptions-item>
        <el-descriptions-item label="身份证号">{{ shop.idCardNo || '—' }}</el-descriptions-item>
        <el-descriptions-item label="统一社会信用代码">{{ shop.businessLicenseNo || '—' }}</el-descriptions-item>
        <el-descriptions-item label="审核状态">{{ auditLabel(shop.auditStatus) }}</el-descriptions-item>
        <el-descriptions-item label="启用">{{ shop.isEnabled === 1 ? '正常' : '禁用' }}</el-descriptions-item>
        <el-descriptions-item label="驳回原因" :span="2">{{ shop.auditReason || '—' }}</el-descriptions-item>
        <el-descriptions-item label="审核时间">{{ formatDateTime(shop.auditTime) }}</el-descriptions-item>
        <el-descriptions-item label="审核人ID">{{ shop.auditId ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ formatDateTime(shop.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatDateTime(shop.updateTime) }}</el-descriptions-item>
      </el-descriptions>

      <h3 class="mt24">图片</h3>
      <el-row :gutter="16" class="img-row">
        <el-col v-for="item in imageFields" :key="item.key" :span="8">
          <div class="img-card">
            <div class="img-title">{{ item.label }}</div>
            <el-image
              v-if="imgUrl(item.key)"
              :src="imgUrl(item.key)"
              fit="contain"
              style="width: 100%; height: 180px"
              :preview-src-list="[imgUrl(item.key)]"
            />
            <div v-else class="img-empty">无</div>
          </div>
        </el-col>
      </el-row>
    </template>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getShop } from '@/api/shop/shop'
import { formatDateTime } from '@/utils/dateTime'
import { parseOptionalPositiveInt } from '@/utils/shopRoute'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const shop = ref(null)

const shopId = computed(() => parseOptionalPositiveInt(route.params.id))

const imageFields = [
  { key: 'idCardFront', label: '身份证正面' },
  { key: 'idCardBack', label: '身份证背面' },
  { key: 'businessLicensePic', label: '营业执照' },
  { key: 'shopExterior', label: '店铺外景' },
  { key: 'shopInterior', label: '店铺内景' }
]

function imgUrl(key) {
  const u = shop.value?.[key]
  if (!u || typeof u !== 'string') return ''
  const t = u.trim()
  return t.startsWith('http://') || t.startsWith('https://') ? t : ''
}

function auditLabel(s) {
  if (s === 0) return '待审核'
  if (s === 1) return '已通过'
  if (s === 2) return '已驳回'
  return '—'
}

/** 加载店铺详情 */
async function load() {
  loading.value = true
  try {
    const res = await getShop(shopId.value)
    shop.value = res.data || null
    if (!shop.value) ElMessage.error('店铺不存在')
  } finally {
    loading.value = false
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
  height: 180px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--el-fill-color-light);
  color: var(--el-text-color-placeholder);
}
</style>
