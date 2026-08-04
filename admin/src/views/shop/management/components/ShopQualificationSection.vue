<template>
  <div>
    <el-divider content-position="left">资质</el-divider>
    <el-row :gutter="24">
      <el-col :xs="24" :md="12">
        <el-form-item label="法人姓名" prop="legalPerson">
          <el-input v-model="form.legalPerson" clearable />
        </el-form-item>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-form-item label="身份证号" prop="idCardNo">
          <el-input v-model="form.idCardNo" clearable />
        </el-form-item>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-form-item prop="businessLicenseNo" class="nowrap-label-item">
          <template #label>
            <span class="form-label-text">统一社会信用代码</span>
          </template>
          <el-input v-model="form.businessLicenseNo" maxlength="64" clearable />
        </el-form-item>
      </el-col>
    </el-row>

    <el-alert
      type="info"
      :closable="false"
      show-icon
      class="hint-alert"
      title="图片仅缓存在浏览器本地；点击「提交保存」后一次性提交到服务端，由服务端识别身份证并与法人信息比对，通过后上传到 OSS 再入库（待审核）。"
    />
  </div>
</template>

<script setup>
defineProps({
  form: { type: Object, required: true }
})
</script>

<style scoped>
.nowrap-label-item :deep(.form-label-text) {
  white-space: nowrap;
  display: inline-block;
}

.hint-alert {
  margin: 8px 0 20px;
}
</style>
