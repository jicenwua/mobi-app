/** 拼接侧栏/路由菜单 path，避免 path-browserify 在 URL 上出现重复段 */
export function joinRoutePath(basePath, routePath) {
  if (!routePath) {
    return (basePath || '/').replace(/\/+/g, '/') || '/'
  }
  if (/^https?:\/\//.test(routePath)) {
    return routePath
  }
  if (routePath.startsWith('/')) {
    return routePath.replace(/\/+/g, '/')
  }
  const base = (basePath || '').replace(/\/$/, '')
  return `${base}/${routePath}`.replace(/\/+/g, '/') || '/'
}
