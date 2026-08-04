import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { SHOP_IMAGE_FIELDS } from '../constants'

function createEmptyLists() {
  return Object.fromEntries(SHOP_IMAGE_FIELDS.map((field) => [field, []]))
}

function createEmptyRawFiles() {
  return Object.fromEntries(SHOP_IMAGE_FIELDS.map((field) => [field, null]))
}

export function useShopImageUpload(form) {
  const fileLists = reactive(createEmptyLists())
  const rawFiles = reactive(createEmptyRawFiles())

  const previewVisible = ref(false)
  const previewUrl = ref('')
  let previewRevokeUrl = ''

  function onFileChange(field, _uploadFile, uploadFiles) {
    const last = uploadFiles[uploadFiles.length - 1]
    rawFiles[field] = last?.raw || null
    form[field] = rawFiles[field] ? 'pending' : ''
  }

  function onFileRemove(field) {
    rawFiles[field] = null
    form[field] = ''
  }

  function onUploadExceed() {
    ElMessage.warning('每项仅可上传一张图片，请先删除当前图片再更换')
  }

  function handleUploadPreview(file) {
    if (previewRevokeUrl) {
      URL.revokeObjectURL(previewRevokeUrl)
      previewRevokeUrl = ''
    }
    if (file.url) {
      previewUrl.value = file.url
    } else if (file.raw) {
      previewRevokeUrl = URL.createObjectURL(file.raw)
      previewUrl.value = previewRevokeUrl
    } else {
      ElMessage.warning('无法预览该文件')
      return
    }
    previewVisible.value = true
  }

  function onPreviewDialogClosed() {
    if (previewRevokeUrl) {
      URL.revokeObjectURL(previewRevokeUrl)
      previewRevokeUrl = ''
    }
    previewUrl.value = ''
  }

  function hydrateFromShop(shop) {
    SHOP_IMAGE_FIELDS.forEach((field) => {
      const url = shop[field]
      if (url) {
        fileLists[field] = [{ name: field, url }]
        form[field] = url
      } else {
        fileLists[field] = []
        form[field] = ''
      }
      rawFiles[field] = null
    })
  }

  function validateRequiredFiles(isEdit) {
    if (isEdit) return true
    for (const field of SHOP_IMAGE_FIELDS) {
      if (!rawFiles[field]) {
        return false
      }
    }
    return true
  }

  function getSubmitFiles() {
    return { ...rawFiles }
  }

  return {
    fileLists,
    rawFiles,
    previewVisible,
    previewUrl,
    onFileChange,
    onFileRemove,
    onUploadExceed,
    handleUploadPreview,
    onPreviewDialogClosed,
    hydrateFromShop,
    validateRequiredFiles,
    getSubmitFiles
  }
}
