<template>
  <div class="login-container">
    <div class="theme-toggle" @click="toggleTheme">
      <el-icon v-if="themeStore.themeMode === 'light'"><Moon /></el-icon>
      <el-icon v-else><Sunny /></el-icon>
    </div>

    <el-form
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        class="login-form"
    >
      <div class="title-container">
        <h3 class="title">后台管理系统</h3>
        <p class="subtitle">欢迎登录</p>
      </div>

      <el-form-item prop="username" class="input-item">
        <span class="svg-container">
          <el-icon><User /></el-icon>
        </span>
        <el-input
            v-model="loginForm.username"
            placeholder="请输入用户名"
            type="text"
        />
      </el-form-item>

      <el-form-item prop="password" class="input-item">
        <span class="svg-container">
          <el-icon><Lock /></el-icon>
        </span>
        <el-input
            v-model="loginForm.password"
            :type="passwordType"
            placeholder="请输入密码"
        />
        <span class="show-pwd" @click="showPwd">
          <el-icon>
            <View v-if="passwordType === 'password'" />
            <Hide v-else />
          </el-icon>
        </span>
      </el-form-item>

      <div class="captcha-row">
        <el-form-item prop="code" class="captcha-input-item">
          <span class="svg-container">
            <el-icon><Key /></el-icon>
          </span>
          <el-input
              v-model="loginForm.code"
              placeholder="验证码"
              @keyup.enter="handleLogin"
          />
        </el-form-item>
        <div class="captcha-img-container" @click="getCode">
          <img :src="codeUrl" alt="验证码" v-if="codeUrl" />
          <div v-else class="captcha-loading">加载中</div>
        </div>
      </div>

      <el-button
          :loading="loading"
          type="primary"
          class="login-btn"
          @click.prevent="handleLogin"
      >
        登录
      </el-button>
    </el-form>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/store/modules/user'
import { useThemeStore } from '@/store/modules/theme'
import { ElMessage } from 'element-plus'
import { User, Lock, View, Hide, Moon, Sunny, Key } from '@element-plus/icons-vue'
import { getCodeImg } from '@/api/system/user'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const themeStore = useThemeStore()

const loginFormRef = ref(null)
const loading = ref(false)
const passwordType = ref('password')
const codeUrl = ref('')

const loginForm = reactive({
  username: '',
  password: '',
  code: '',
  uuid: ''
})

const loginRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' }
  ]
}

function toggleTheme() {
  themeStore.toggleTheme()
}

function showPwd() {
  if (passwordType.value === 'password') {
    passwordType.value = ''
  } else {
    passwordType.value = 'password'
  }
}

/** 拉取验证码（img 为二次 base64，需 atob 一次） */
async function getCode() {
  try {
    const res = await getCodeImg()
    const base64Img = atob(res.img)
    codeUrl.value = 'data:image/jpeg;base64,' + base64Img
    loginForm.uuid = res.uuid
  } catch {
    // 验证码加载失败时保持占位
  }
}

async function handleLogin() {
  if (loading.value || !loginFormRef.value) return

  await loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const loginData = {
          username: loginForm.username,
          password: loginForm.password,
          code: loginForm.code,
          uuid: loginForm.uuid
        }
        await userStore.userLogin(loginData)

        const redirect = route.query.redirect || '/'
        router.push(redirect)

        ElMessage.success('登录成功')
      } catch {
        getCode()
        loginForm.code = ''
      } finally {
        loading.value = false
      }
    }
  })
}

onMounted(() => {
  themeStore.initTheme()
  // 进入登录页时清掉过期 token，避免带着旧 token 请求触发 401 弹窗
  userStore.prepareLoginPage()
  getCode()
})
</script>

<style scoped>
.login-container {
  min-height: 100vh;
  width: 100%;
  background: var(--login-bg);
  overflow: hidden;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  transition: all 0.3s ease;
}

.theme-toggle {
  position: absolute;
  top: 20px;
  right: 20px;
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: var(--login-card-bg);
  box-shadow: var(--box-shadow-base);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.3s ease;
  z-index: 10;
}

.theme-toggle:hover {
  transform: scale(1.1);
  box-shadow: var(--box-shadow-light);
}

.theme-toggle .el-icon {
  font-size: 24px;
  color: var(--primary-color);
}

.title-container {
  text-align: center;
  margin-bottom: 40px;
}

.title {
  font-size: 28px;
  color: var(--login-title-color);
  margin: 0 0 10px 0;
  font-weight: bold;
  transition: color 0.3s ease;
}

.subtitle {
  font-size: 14px;
  color: var(--text-color-secondary);
  margin: 0;
  transition: color 0.3s ease;
}

.login-form {
  position: relative;
  width: 420px;
  padding: 40px;
  background: var(--login-card-bg);
  border-radius: 12px;
  box-shadow: var(--box-shadow-light);
}

:deep(.el-form-item) {
  border: 1px solid var(--border-color-light);
  background: var(--bg-color-secondary);
  border-radius: 8px;
  margin-bottom: 25px;
  display: flex;
  align-items: center;
  position: relative;
}

:deep(.el-input) {
  flex: 1;
  width: 100%;
}

:deep(.el-input__wrapper) {
  box-shadow: none !important;
  background: transparent !important;
  padding-left: 45px;
}

.svg-container {
  position: absolute;
  left: 15px;
  z-index: 2;
  color: var(--text-color-secondary);
  display: flex;
  align-items: center;
}

.captcha-row {
  display: flex;
  gap: 15px;
  align-items: flex-start;
  margin-bottom: 25px;
}

.captcha-input-item {
  flex: 1;
  margin-bottom: 0;
}

.captcha-img-container {
  width: 120px;
  height: 48px;
  cursor: pointer;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid var(--border-color-light);
  background: var(--bg-color);
  flex-shrink: 0;
  transition: background-color 0.35s ease, border-color 0.35s ease;
}

.captcha-img-container img {
  width: 100%;
  height: 100%;
  object-fit: fill;
}

.captcha-loading {
  font-size: 12px;
  color: var(--text-color-placeholder);
  text-align: center;
  line-height: 48px;
}

.show-pwd {
  padding-right: 15px;
  cursor: pointer;
  color: var(--text-color-secondary);
}

.login-btn {
  width: 100%;
  height: 48px;
  font-size: 16px;
  border-radius: 8px;
}

@media screen and (max-width: 480px) {
  .login-form {
    width: 90%;
    padding: 25px;
  }
  .captcha-img-container {
    width: 100px;
  }
}
</style>
