<template>
  <div>
    <el-divider content-position="left">图片</el-divider>
    <div v-for="(row, rowIdx) in uploadLayout" :key="rowIdx" class="upload-row-wrap">
      <el-row :gutter="24" class="upload-row">
        <el-col v-for="item in row.cols" :key="item.field" :xs="24" :span="row.cols.length === 1 ? 24 : 12">
          <el-form-item :label="item.label" :prop="item.field" :required="item.required">
            <el-upload
              v-model:file-list="fileLists[item.field]"
              class="upload-card"
              :class="{ 'upload-filled': fileLists[item.field].length > 0 }"
              :auto-upload="false"
              :limit="1"
              list-type="picture-card"
              accept="image/*"
              :on-change="(f, fl) => onFileChange(item.field, f, fl)"
              :on-remove="() => onFileRemove(item.field)"
              :on-exceed="onUploadExceed"
              :on-preview="handleUploadPreview"
            >
              <el-icon><Plus /></el-icon>
            </el-upload>
          </el-form-item>
        </el-col>
      </el-row>
    </div>

    <el-dialog
      v-model="previewVisible"
      title="图片预览"
      width="min(720px, 92vw)"
      append-to-body
      align-center
      destroy-on-close
      class="upload-preview-dialog"
      @closed="onPreviewDialogClosed"
    >
      <img v-if="previewUrl" :src="previewUrl" alt="" class="upload-preview-img" />
    </el-dialog>
  </div>
</template>

<script setup>
import { Plus } from '@element-plus/icons-vue'
import { SHOP_UPLOAD_LAYOUT } from '../constants'

defineProps({
  fileLists: { type: Object, required: true },
  previewUrl: { type: String, default: '' },
  onFileChange: { type: Function, required: true },
  onFileRemove: { type: Function, required: true },
  onUploadExceed: { type: Function, required: true },
  handleUploadPreview: { type: Function, required: true },
  onPreviewDialogClosed: { type: Function, required: true }
})

const previewVisible = defineModel('previewVisible', { type: Boolean, default: false })

const uploadLayout = SHOP_UPLOAD_LAYOUT
</script>

<style scoped>
.upload-row-wrap + .upload-row-wrap {
  margin-top: 4px;
}

.upload-row :deep(.el-upload--picture-card) {
  --el-upload-picture-card-size: 120px;
}

.upload-card.upload-filled :deep(.el-upload.el-upload--picture-card) {
  display: none;
}

.upload-preview-img {
  display: block;
  max-width: 100%;
  max-height: min(70vh, 640px);
  margin: 0 auto;
  object-fit: contain;
}
</style>
