<template>
  <div class="app-container shop-add-page">
    <el-page-header @back="router.back()" :content="isEdit ? '编辑店铺' : '添加店铺'" class="page-head" />
    <el-card shadow="never" class="form-card" v-loading="pageLoading">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="156px" class="shop-form">
        <ShopBasicSection :form="form" :is-edit="isEdit" :categories="SHOP_CATEGORIES" />
        <ShopAddressSection :form="form" />
        <ShopQualificationSection :form="form" />
        <ShopImageUploadSection
          v-model:preview-visible="previewVisible"
          :file-lists="fileLists"
          :preview-url="previewUrl"
          :on-file-change="onFileChange"
          :on-file-remove="onFileRemove"
          :on-upload-exceed="onUploadExceed"
          :handle-upload-preview="handleUploadPreview"
          :on-preview-dialog-closed="onPreviewDialogClosed"
        />

        <el-form-item class="actions">
          <el-button type="primary" :loading="submitting" @click="submit">提交保存</el-button>
          <el-button @click="router.back()">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { parseOptionalPositiveInt } from '@/utils/shopRoute'
import { addShopFull, getShop } from '@/api/shop/shop'
import { SHOP_CATEGORIES, SHOP_UPLOAD_LAYOUT, createEmptyShopForm } from './constants'
import { useShopImageUpload } from './composables/useShopImageUpload'
import ShopBasicSection from './components/ShopBasicSection.vue'
import ShopAddressSection from './components/ShopAddressSection.vue'
import ShopQualificationSection from './components/ShopQualificationSection.vue'
import ShopImageUploadSection from './components/ShopImageUploadSection.vue'

const router = useRouter()
const route = useRoute()
const formRef = ref()
const submitting = ref(false)
const pageLoading = ref(false)
const isEdit = ref(false)
const shopId = ref(null)

const form = reactive(createEmptyShopForm())

const {
  fileLists,
  previewVisible,
  previewUrl,
  onFileChange,
  onFileRemove,
  onUploadExceed,
  handleUploadPreview,
  onPreviewDialogClosed,
  hydrateFromShop,
  getSubmitFiles
} = useShopImageUpload(form)

const rules = {
  shopName: [{ required: true, message: '请输入店铺名称', trigger: 'blur' }],
  ratio: [
    { required: true, message: '请填写充值积分比率', trigger: 'change' },
    {
      validator: (_rule, value, callback) => {
        if (value == null || value < 1 || value > 100000) {
          callback(new Error('比率须为 1～100000 的正整数'))
        } else {
          callback()
        }
      },
      trigger: 'change'
    }
  ],
  categoryId: [{ required: true, message: '请选择类目', trigger: 'change' }],
  managerUserId: [{ required: true, message: '请填写店长用户ID', trigger: 'change' }],
  legalPerson: [{ required: true, message: '请输入法人姓名', trigger: 'blur' }],
  idCardNo: [{ required: true, message: '请输入身份证号', trigger: 'blur' }],
  businessLicenseNo: [{ required: true, message: '请输入统一社会信用代码', trigger: 'blur' }],
  address: [{ required: true, message: '请输入地址', trigger: 'blur' }],
  idCardFront: [{ required: true, message: '请上传身份证正面', trigger: 'change' }],
  idCardBack: [{ required: true, message: '请上传身份证背面', trigger: 'change' }],
  businessLicensePic: [{ required: true, message: '请上传营业执照', trigger: 'change' }],
  shopExterior: [{ required: true, message: '请上传店铺外景', trigger: 'change' }],
  shopInterior: [{ required: true, message: '请上传店铺内景', trigger: 'change' }]
}

onMounted(() => {
  const id = parseOptionalPositiveInt(route.params.id)
  if (id) {
    isEdit.value = true
    shopId.value = id
    void loadShopDetail()
  }
})

async function loadShopDetail() {
  pageLoading.value = true
  try {
    const res = await getShop(shopId.value)
    const shop = res.data
    if (!shop) return

    Object.assign(form, {
      parentShopCode: shop.parentShopCode || '',
      shopName: shop.shopName || '',
      ratio: shop.ratio ?? 10,
      categoryId: shop.categoryId,
      province: shop.province || '',
      city: shop.city || '',
      district: shop.district || '',
      address: shop.address || '',
      legalPerson: shop.legalPerson || '',
      idCardNo: shop.idCardNo || '',
      businessLicenseNo: shop.businessLicenseNo || ''
    })
    hydrateFromShop(shop)
  } catch {
    ElMessage.error('加载店铺信息失败')
  } finally {
    pageLoading.value = false
  }
}

function buildMetaPayload() {
  return {
    parentShopCode: form.parentShopCode?.trim() || undefined,
    shopName: form.shopName,
    ratio: form.ratio,
    managerUserId: form.managerUserId,
    categoryId: form.categoryId,
    province: form.province,
    city: form.city,
    district: form.district,
    address: form.address,
    legalPerson: form.legalPerson,
    idCardNo: form.idCardNo,
    businessLicenseNo: form.businessLicenseNo
  }
}

async function submit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  if (!isEdit.value) {
    for (const row of SHOP_UPLOAD_LAYOUT) {
      for (const u of row.cols) {
        if (!getSubmitFiles()[u.field]) {
          ElMessage.warning(`请上传：${u.label}`)
          return
        }
      }
    }
  }

  submitting.value = true
  try {
    if (isEdit.value) {
      ElMessage.warning('编辑功能尚未完全实现')
      ElMessage.success('保存成功（模拟）')
    } else {
      await addShopFull(buildMetaPayload(), getSubmitFiles())
      ElMessage.success('保存成功')
    }
    router.push('/shop/manage')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.shop-add-page {
  padding: 20px;
  background: var(--el-bg-color-page, #f5f7fa);
  min-height: 100%;
  box-sizing: border-box;
}

.page-head {
  margin-bottom: 16px;
}

.form-card {
  max-width: 1040px;
  border-radius: 8px;
}

.shop-form {
  padding: 4px 8px 8px;
}

.shop-form :deep(.el-divider__text) {
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.shop-form :deep(.el-form-item__label) {
  white-space: nowrap;
  line-height: 1.4;
  align-items: flex-start;
  padding-top: 2px;
}

.actions {
  margin-top: 8px;
  padding-top: 8px;
}
</style>
