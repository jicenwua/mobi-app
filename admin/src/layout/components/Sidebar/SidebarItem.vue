<template>
  <div v-if="shouldShowSidebarItem(item)">
    <template v-if="hasOneShowingChild(item.children, item) && onlyOneChild">
      <el-menu-item
        :index="resolvePath(onlyOneChild.path)"
      >
        <el-icon v-if="onlyOneChild.meta && onlyOneChild.meta.icon">
          <component :is="onlyOneChild.meta.icon" />
        </el-icon>
        <template #title>
          <MenuTitle :path="resolvePath(onlyOneChild.path)" :title="onlyOneChild.meta?.title" />
        </template>
      </el-menu-item>
    </template>

    <el-sub-menu
      v-else-if="item.children && item.children.length > 0"
      :index="resolvePath(item.path)"
    >
      <template #title>
        <el-icon v-if="item.meta && item.meta.icon">
          <component :is="item.meta.icon" />
        </el-icon>
        <MenuTitle :path="resolvePath(item.path)" :title="item.meta?.title" />
      </template>
      
      <SidebarItem
        v-for="child in item.children"
        :key="child.path"
        :item="child"
        :base-path="resolvePath(item.path)"
      />
    </el-sub-menu>

    <el-menu-item
      v-else
      :index="resolvePath(item.path)"
    >
      <el-icon v-if="item.meta && item.meta.icon">
        <component :is="item.meta.icon" />
      </el-icon>
      <template #title>
        <MenuTitle :path="resolvePath(item.path)" :title="item.meta?.title" />
      </template>
    </el-menu-item>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import SidebarItem from './SidebarItem.vue'
import MenuTitle from './MenuTitle.vue'
import { isRouteHidden } from '@/utils/routeHidden'
import { joinRoutePath } from '@/utils/routePath'

const props = defineProps({
  item: {
    type: Object,
    required: true
  },
  basePath: {
    type: String,
    default: ''
  }
})

const onlyOneChild = ref(null)

/** 是否仅展示一个子菜单（或无子菜单时用父级） */
function hasOneShowingChild(children = [], parent) {
  const showingChildren = children.filter(child => {
    if (isRouteHidden(child)) {
      return false
    } else {
      onlyOneChild.value = child
      return true
    }
  })

  if (showingChildren.length === 1) {
    return true
  }

  if (showingChildren.length === 0) {
    return false
  }

  return false
}

function resolvePath(routePath) {
  return joinRoutePath(props.basePath, routePath)
}

/** 无标题且无可见子项时不渲染（避免侧栏空白菜单） */
function shouldShowSidebarItem(item) {
  if (isRouteHidden(item)) return false
  const title = (item.meta?.title || '').trim()
  if (title) return true
  const children = item.children || []
  return children.some((child) => !isRouteHidden(child) && (child.meta?.title || '').trim())
}
</script>

<style scoped>
.el-menu-item,
.el-sub-menu__title {
  font-size: 14px;
}
</style>
