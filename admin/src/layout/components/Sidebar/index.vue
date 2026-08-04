<template>
  <div class="sidebar-container">
    <div class="sidebar-logo">
      <div class="logo-brand">
        <span class="logo-mark" aria-hidden="true">
          <i></i><i></i><i></i><i></i>
        </span>
        <h1 class="logo-title">后台管理系统</h1>
      </div>
    </div>

    <div class="sidebar-menu">
      <el-menu
        :key="permissionStore.routeRenderKey"
        :default-active="activeMenu"
        :default-openeds="defaultOpeneds"
        background-color="transparent"
        text-color="var(--sidebar-text)"
        active-text-color="var(--sidebar-active-text)"
        :collapse="false"
        router
      >
        <SidebarItem
          v-for="route in permissionRoutes"
          :key="route.path"
          :item="route"
          :base-path="route.path"
        />
      </el-menu>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { usePermissionStore } from '@/store/modules/permission'
import SidebarItem from './SidebarItem.vue'
import { isRouteHidden } from '@/utils/routeHidden'

const route = useRoute()
const permissionStore = usePermissionStore()

/** 当前高亮菜单路径 */
const activeMenu = computed(() => {
  const { meta, path } = route
  if (meta.activeMenu) {
    return meta.activeMenu
  }
  return path
})

const permissionRoutes = computed(() => {
  return permissionStore.routes.filter((route) => !isRouteHidden(route))
})

/** 默认展开含子菜单的一级路由 */
const defaultOpeneds = computed(() => {
  const openeds = []
  permissionStore.routes.forEach(route => {
    if (route.children && route.children.length > 0) {
      openeds.push(route.path)
    }
  })
  return openeds
})

</script>

<style scoped>
.sidebar-container {
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: var(--sidebar-bg);
  background-image: var(--sidebar-bg-pattern);
  background-size: var(--sidebar-bg-size);
  transition:
    background-color 0.35s ease,
    border-color 0.35s ease;
}

.sidebar-logo {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 16px;
  background: var(--sidebar-logo-gradient);
  backdrop-filter: blur(12px);
  border-bottom: 1px solid var(--sidebar-border);
  flex-shrink: 0;
  transition:
    background 0.35s ease,
    border-color 0.35s ease;
}

.logo-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  min-width: 0;
}

.logo-mark {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  padding: 7px;
  border-radius: var(--radius-sm);
  background: var(--sidebar-logo-accent);
  box-shadow: 0 4px 12px rgba(59, 108, 255, 0.28);
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 3px;
  transition: box-shadow 0.35s ease;
}

:global(html.dark) .logo-mark {
  box-shadow: 0 4px 14px rgba(96, 165, 250, 0.25);
}

.logo-mark i {
  display: block;
  border-radius: 1px;
  background: rgba(255, 255, 255, 0.92);
}

.logo-title {
  color: var(--sidebar-logo-text);
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.02em;
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sidebar-menu {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  padding-top: 4px;
}

.el-menu {
  border-right: none;
}

/* 滚动条 */
.sidebar-menu::-webkit-scrollbar {
  width: 5px;
}

.sidebar-menu::-webkit-scrollbar-thumb {
  background-color: var(--scrollbar-thumb);
  border-radius: 3px;
}

.sidebar-menu::-webkit-scrollbar-thumb:hover {
  background-color: var(--scrollbar-thumb-hover);
}

.sidebar-menu::-webkit-scrollbar-track {
  background-color: transparent;
}

@media screen and (max-width: 1024px) {
  .logo-title {
    font-size: 14px;
  }

  .logo-mark {
    width: 28px;
    height: 28px;
    padding: 6px;
  }
}

@media screen and (max-width: 768px) {
  .sidebar-logo {
    height: 48px;
    padding: 0 12px;
  }

  .logo-title {
    font-size: 13px;
  }
}
</style>
