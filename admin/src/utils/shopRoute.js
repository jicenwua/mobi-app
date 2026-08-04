/** 从路由 query 读取店铺 ID（店铺子页跳转常用） */
export function shopIdFromRoute(route) {
  const raw = route?.query?.shopId
  return raw != null && raw !== '' ? String(raw) : ''
}

/**
 * 将表单中的店铺 ID 转为 Long 查询参数；非法输入返回 undefined，避免传 NaN。
 */
export function parseOptionalShopId(value) {
  if (value == null || value === '') {
    return undefined
  }
  const raw = String(value).trim()
  if (!raw || !/^\d+$/.test(raw)) {
    return undefined
  }
  const parsed = Number(raw)
  return Number.isSafeInteger(parsed) && parsed > 0 ? parsed : undefined
}

/**
 * 必填店铺 ID；非法时返回 null，由调用方提示用户。
 */
export function parseRequiredShopId(value) {
  const parsed = parseOptionalShopId(value)
  return parsed ?? null
}

/**
 * 将表单中的正整数 ID 转为查询参数；非法输入返回 undefined。
 */
export function parseOptionalPositiveInt(value) {
  return parseOptionalShopId(value)
}
