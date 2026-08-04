/** 路由是否在侧栏隐藏（兼容 route.hidden 与 meta.hidden） */
export function isRouteHidden(route) {
  if (!route) return true
  if (route.hidden === true) return true
  return route.meta?.hidden === true
}
