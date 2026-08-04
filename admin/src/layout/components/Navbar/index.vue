<template>
  <div class="navbar">
    <NavbarSkyDecoration />

    <div class="left-menu">
      <Hamburger />
      <Breadcrumb />
    </div>
    
    <div class="right-menu">
      <div class="theme-toggle" @click="toggleTheme" title="切换主题">
        <el-icon v-if="themeStore.themeMode === 'light'"><Moon /></el-icon>
        <el-icon v-else><Sunny /></el-icon>
      </div>

      <Screenfull />

      <el-dropdown trigger="click">
        <div class="avatar-wrapper">
          <el-avatar :size="40" :src="userInfo.avatar || defaultAvatar" />
          <span class="username">{{ userInfo.username || '管理员' }}</span>
          <el-icon><CaretBottom /></el-icon>
        </div>
        
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item @click="handleProfile">
              <el-icon><User /></el-icon>
              个人中心
            </el-dropdown-item>
            <el-dropdown-item divided @click="handleLogout">
              <el-icon><SwitchButton /></el-icon>
              退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/modules/user'
import { useThemeStore } from '@/store/modules/theme'
import { ElMessageBox } from 'element-plus'
import { CaretBottom, User, SwitchButton, Moon, Sunny } from '@element-plus/icons-vue'
import Hamburger from './Hamburger.vue'
import Breadcrumb from './Breadcrumb.vue'
import Screenfull from './Screenfull.vue'
import NavbarSkyDecoration from './NavbarSkyDecoration.vue'

const router = useRouter()
const userStore = useUserStore()
const themeStore = useThemeStore()

const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const userInfo = computed(() => userStore.userInfo)

function toggleTheme() {
  themeStore.toggleTheme()
}

function handleProfile() {
  router.push('/system/profile')
}

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    
    await userStore.userLogout()
    router.push('/login')
  } catch {
    // 用户取消确认
  }
}

onMounted(() => {
  themeStore.initTheme()
})
</script>

<style scoped>
.navbar {
  height: 52px;
  overflow: hidden;
  position: relative;
  background: var(--navbar-bg);
  backdrop-filter: blur(14px);
  -webkit-backdrop-filter: blur(14px);
  box-shadow: var(--box-shadow-light);
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
  border-bottom: 1px solid var(--navbar-border);
  transition:
    background-color 0.35s ease,
    border-color 0.35s ease,
    box-shadow 0.35s ease;
}

.left-menu {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
}

.right-menu {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  gap: 15px;
}

.theme-toggle {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  border: 1px solid var(--border-color-light);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition:
    background-color 0.25s ease,
    color 0.25s ease,
    border-color 0.25s ease,
    transform 0.35s ease;
  color: var(--text-color-regular);
}

.theme-toggle:hover {
  background: var(--bg-color-secondary);
  border-color: var(--primary-color);
  color: var(--primary-color);
  transform: rotate(20deg);
}

.theme-toggle .el-icon {
  font-size: 20px;
}

.avatar-wrapper {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 0 10px;
}

.username {
  margin: 0 8px;
  font-size: 14px;
  color: var(--text-color-primary);
  transition: color 0.3s ease;
}

.el-dropdown-menu__item {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 响应式：平板 */
@media screen and (max-width: 1024px) {
  .navbar {
    padding: 0 15px;
  }

  .username {
    display: none;
  }
}

/* 响应式：手机 */
@media screen and (max-width: 768px) {
  .navbar {
    height: 45px;
    padding: 0 10px;
  }

  .right-menu {
    gap: 10px;
  }

  .theme-toggle {
    width: 32px;
    height: 32px;
  }

  .theme-toggle .el-icon {
    font-size: 18px;
  }

  .avatar-wrapper {
    padding: 0 5px;
  }

  .el-avatar {
    width: 32px !important;
    height: 32px !important;
  }
}

@media screen and (max-width: 480px) {
  .navbar {
    height: 40px;
    padding: 0 8px;
  }

  .right-menu {
    gap: 8px;
  }

  .theme-toggle {
    width: 28px;
    height: 28px;
  }

  .theme-toggle .el-icon {
    font-size: 16px;
  }

  .el-avatar {
    width: 28px !important;
    height: 28px !important;
  }
}
</style>
