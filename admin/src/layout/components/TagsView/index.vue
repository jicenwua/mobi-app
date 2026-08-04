<template>
  <div class="tags-view-container">
    <el-scrollbar class="tags-view-wrapper">
      <router-link
        v-for="tag in visitedViews"
        :key="tag.path"
        :to="{ path: tag.path, query: tag.query }"
        class="tags-view-item"
        :class="{ active: isActive(tag) }"
      >
        {{ tag.title }}
        <el-icon v-if="!isAffix(tag)" class="close-icon" @click.prevent.stop="closeSelectedTag(tag)">
          <Close />
        </el-icon>
      </router-link>
    </el-scrollbar>
  </div>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Close } from '@element-plus/icons-vue'

const TAGS_STORAGE_KEY = 'admin-tags-view'

const route = useRoute()
const router = useRouter()

function loadVisitedViews() {
  try {
    const raw = sessionStorage.getItem(TAGS_STORAGE_KEY)
    const parsed = raw ? JSON.parse(raw) : []
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

const visitedViews = ref(loadVisitedViews())

function persistVisitedViews() {
  sessionStorage.setItem(TAGS_STORAGE_KEY, JSON.stringify(visitedViews.value))
}

/** 将当前路由加入已访问标签 */
function addTags() {
  const { name } = route
  if (name) {
    const exist = visitedViews.value.some(item => item.path === route.path)
    if (!exist) {
      visitedViews.value.push({
        name: route.name,
        title: route.meta.title || 'no-name',
        path: route.path,
        query: route.query,
        affix: route.meta.affix || false
      })
      persistVisitedViews()
    }
  }
}

function isActive(tag) {
  return tag.path === route.path
}

function isAffix(tag) {
  return tag.affix
}

/** 关闭标签；若关闭当前页则跳转至最后一个标签 */
function closeSelectedTag(view) {
  const index = visitedViews.value.indexOf(view)
  if (index > -1) {
    visitedViews.value.splice(index, 1)
    persistVisitedViews()
  }

  if (isActive(view)) {
    const latestView = visitedViews.value.slice(-1)[0]
    if (latestView) {
      router.push(latestView.path)
    } else {
      router.push('/')
    }
  }
}

watch(
  () => route.path,
  () => {
    addTags()
  },
  { immediate: true }
)
</script>

<style scoped>
.tags-view-container {
  height: 36px;
  width: 100%;
  background: var(--tags-bg);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--tags-border);
  transition:
    background-color 0.35s ease,
    border-color 0.35s ease,
    box-shadow 0.35s ease;
}

.tags-view-wrapper {
  white-space: nowrap;
}

.tags-view-item {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  position: relative;
  cursor: pointer;
  height: 26px;
  line-height: 1;
  border: 1px solid var(--panel-border);
  color: var(--text-color-regular);
  background: var(--tags-item-bg);
  padding: 0 10px;
  font-size: 12px;
  margin-left: 6px;
  margin-top: 5px;
  border-radius: var(--radius-sm);
  transition:
    color 0.35s ease,
    background-color 0.35s ease,
    border-color 0.35s ease;
}

.tags-view-item:first-of-type {
  margin-left: 15px;
}

.tags-view-item.active {
  background-color: var(--primary-color);
  color: #fff;
  border-color: var(--primary-color);
  box-shadow: 0 2px 8px rgba(59, 108, 255, 0.35);
}

:root.dark .tags-view-item.active {
  box-shadow: 0 2px 8px rgba(96, 165, 250, 0.25);
}

.close-icon {
  width: 16px;
  height: 16px;
  vertical-align: 2px;
  border-radius: 50%;
  transition: all 0.3s cubic-bezier(0.645, 0.045, 0.355, 1);
  transform-origin: 100% 50%;
}

.close-icon:hover {
  background-color: var(--text-color-secondary);
  color: var(--bg-color);
}

/* 响应式：平板 */
@media screen and (max-width: 1024px) {
  .tags-view-container {
    height: 30px;
  }

  .tags-view-item {
    height: 24px;
    line-height: 24px;
    font-size: 11px;
    padding: 0 6px;
  }
}

/* 响应式：手机 */
@media screen and (max-width: 768px) {
  .tags-view-container {
    height: 28px;
  }

  .tags-view-item {
    height: 22px;
    line-height: 22px;
    font-size: 11px;
    padding: 0 5px;
    margin-left: 3px;
  }

  .tags-view-item:first-of-type {
    margin-left: 10px;
  }
}

@media screen and (max-width: 480px) {
  .tags-view-container {
    height: 26px;
  }

  .tags-view-item {
    height: 20px;
    line-height: 20px;
    font-size: 10px;
    padding: 0 4px;
    margin-left: 2px;
  }

  .tags-view-item:first-of-type {
    margin-left: 8px;
  }

  .close-icon {
    width: 14px;
    height: 14px;
  }
}
</style>
