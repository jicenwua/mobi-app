<template>
  <div class="screenfull" @click="toggle">
    <el-icon>
      <FullScreen v-if="!isFullscreen" />
      <Aim v-else />
    </el-icon>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { FullScreen, Aim } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import screenfull from 'screenfull'

const isFullscreen = ref(false)

/** 切换浏览器全屏 */
function toggle() {
  if (!screenfull.isEnabled) {
    ElMessage({
      message: '您的浏览器不支持全屏',
      type: 'warning'
    })
    return false
  }
  
  screenfull.toggle()
}

/** 同步全屏状态到图标 */
function syncFullscreen() {
  isFullscreen.value = screenfull.isFullscreen
}

onMounted(() => {
  if (screenfull.isEnabled) {
    screenfull.on('change', syncFullscreen)
  }
})

onUnmounted(() => {
  if (screenfull.isEnabled) {
    screenfull.off('change', syncFullscreen)
  }
})
</script>

<style scoped>
.screenfull {
  padding: 0 10px;
  cursor: pointer;
  display: flex;
  align-items: center;
}

.screenfull .el-icon {
  font-size: 20px;
}
</style>
