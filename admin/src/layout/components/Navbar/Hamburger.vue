<template>
  <div class="hamburger" @click="toggleClick">
    <el-icon :class="{ 'is-active': isActive }">
      <Fold v-if="isActive" />
      <Expand v-else />
    </el-icon>
  </div>
</template>

<script setup>
import { Fold, Expand } from '@element-plus/icons-vue'

defineProps({
  isActive: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['toggleClick'])

function toggleClick() {
  emit('toggleClick')
}
</script>

<style scoped>
.hamburger {
  padding: 0 10px;
  cursor: pointer;
  display: flex;
  align-items: center;
}

.hamburger .el-icon {
  font-size: 20px;
}

.is-active {
  transform: rotate(180deg);
}
</style>
