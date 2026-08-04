<template>
  <div class="error-page">
    <div class="error-content">
      <h1 class="error-code">404</h1>
      <h2 class="error-desc">页面不存在</h2>
      <p class="error-detail">抱歉，您访问的页面不存在或已被删除</p>
      <el-button type="primary" @click="goHome">返回首页</el-button>
    </div>
  </div>
</template>

<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

function goHome() {
  router.push('/')
}
</script>

<style scoped>
.error-page {
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f0f2f5;
}

.error-content {
  text-align: center;
}

.error-code {
  font-size: 120px;
  color: #409eff;
  margin: 0;
}

.error-desc {
  font-size: 32px;
  color: #303133;
  margin: 20px 0;
}

.error-detail {
  font-size: 16px;
  color: #909399;
  margin-bottom: 40px;
}
</style>
