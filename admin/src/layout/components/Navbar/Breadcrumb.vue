<template>
  <el-breadcrumb class="breadcrumb" separator="/">
    <el-breadcrumb-item
      v-for="(item, index) in breadcrumbs"
      :key="item.path"
    >
      <span
        v-if="index === breadcrumbs.length - 1"
        class="no-redirect"
      >
        {{ item.meta?.title }}
      </span>
      <a
        v-else
        @click.prevent="handleLink(item)"
      >
        {{ item.meta?.title }}
      </a>
    </el-breadcrumb-item>
  </el-breadcrumb>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()

const breadcrumbs = ref([])

/** 根据 matched 生成面包屑，首页路由前补「首页」 */
function getBreadcrumb() {
  let matched = route.matched.filter(item => item.meta && item.meta.title)
  
  const first = matched[0]
  if (!isDashboard(first)) {
    matched = [{ path: '/dashboard', meta: { title: '首页' } }].concat(matched)
  }
  
  breadcrumbs.value = matched
}

function isDashboard(route) {
  const name = route && route.name
  if (!name) {
    return false
  }
  return name.toString().trim().toLowerCase() === 'dashboard'
}

function handleLink(item) {
  const { redirect, path } = item
  if (redirect) {
    router.push(redirect)
    return
  }
  router.push(path)
}

watch(
  () => route.path,
  () => getBreadcrumb(),
  { immediate: true }
)
</script>

<style scoped>
.breadcrumb {
  display: inline-block;
  font-size: 14px;
  line-height: 52px;
  margin-left: 8px;
}

.no-redirect {
  color: var(--breadcrumb-current);
  cursor: text;
}

a {
  color: var(--breadcrumb-link);
  cursor: pointer;
}

a:hover {
  color: var(--breadcrumb-hover);
}
</style>
