<template>
  <div>
    <el-divider content-position="left">地址</el-divider>
    <el-row :gutter="24">
      <el-col :xs="24" :sm="8">
        <el-form-item label="省" prop="province">
          <el-input v-model="form.province" clearable />
        </el-form-item>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-form-item label="市" prop="city">
          <el-input v-model="form.city" clearable />
        </el-form-item>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-form-item label="区/县" prop="district">
          <el-input v-model="form.district" clearable />
        </el-form-item>
      </el-col>
      <el-col :span="24">
        <el-form-item label="详细地址" prop="address">
          <el-input
            v-model="form.address"
            type="textarea"
            :rows="3"
            maxlength="255"
            show-word-limit
            resize="none"
          />
        </el-form-item>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
defineProps({
  form: { type: Object, required: true }
})
</script>
