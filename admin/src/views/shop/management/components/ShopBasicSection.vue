<template>
  <div>
    <el-divider content-position="left">主体信息</el-divider>
    <el-row :gutter="24">
      <el-col :xs="24" :md="12">
        <el-form-item label="店铺名称" prop="shopName">
          <el-input v-model="form.shopName" maxlength="100" show-word-limit clearable />
        </el-form-item>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-form-item label="行业类目" prop="categoryId">
          <el-select v-model="form.categoryId" placeholder="请选择" style="width: 100%" clearable>
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
      </el-col>
      <el-col :xs="24" :md="12">
        <el-form-item label="充值积分比率" prop="ratio">
          <el-input-number
            v-model="form.ratio"
            :min="1"
            :max="100000"
            :precision="0"
            :step="1"
            :disabled="isEdit"
            controls-position="right"
            style="width: 100%"
          />
          <div class="ratio-hint">1 元 × 比率 = 充值 1 元所得积分；创建后不可修改</div>
        </el-form-item>
      </el-col>
      <el-col v-if="!isEdit" :xs="24" :md="12">
        <el-form-item label="店长用户ID" prop="managerUserId">
          <el-input-number
            v-model="form.managerUserId"
            :min="1"
            :precision="0"
            controls-position="right"
            placeholder="mobi_user.user_id"
            style="width: 100%"
          />
          <div class="ratio-hint">店长须先完成小程序注册；提交后将绑定为该店铺店长</div>
        </el-form-item>
      </el-col>
      <el-col :span="24">
        <el-form-item label="上级店铺代码" prop="parentShopCode">
          <el-input
            v-model="form.parentShopCode"
            maxlength="64"
            clearable
            show-word-limit
            placeholder="选填：填写已审核通过的总店「店铺唯一代码」；留空表示独立店（无上级）"
          />
        </el-form-item>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
defineProps({
  form: { type: Object, required: true },
  isEdit: { type: Boolean, default: false },
  categories: { type: Array, default: () => [] }
})
</script>

<style scoped>
.ratio-hint {
  margin-top: 6px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  line-height: 1.4;
}
</style>
