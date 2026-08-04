/** localStorage 中用户信息 key（仅持久化展示字段 + 菜单树，不含权限标识等） */
export const USER_INFO_STORAGE_KEY = 'userInfo'

/**
 * 从接口完整 VO 中挑出允许落盘的字段（用于刷新后展示头像昵称、以及可选的菜单恢复）
 * 不包含：permissions、roles、邮箱手机等敏感或非必要字段
 */
export function pickPersistedUserProfile(full) {
  if (!full || typeof full !== 'object') {
    return {}
  }
  const userName = full.userName ?? full.username ?? ''
  let menus = full.menus
  if (!Array.isArray(menus)) {
    menus = menus == null ? [] : []
  }
  return {
    userId: full.userId ?? null,
    userName,
    username: userName,
    nickName: full.nickName ?? '',
    avatar: full.avatar ?? '',
    menus
  }
}

export function loadPersistedUserInfo() {
  try {
    const raw = localStorage.getItem(USER_INFO_STORAGE_KEY)
    if (!raw) return {}
    const parsed = JSON.parse(raw)
    const clean = pickPersistedUserProfile(parsed)
    // 迁移：若曾为整包 VO，去掉 permissions / roles 等并写回瘦身结果
    if (parsed && typeof parsed === 'object' && ('permissions' in parsed || 'roles' in parsed)) {
      savePersistedUserInfo(clean)
    }
    return clean
  } catch {
    return {}
  }
}

export function savePersistedUserInfo(slice) {
  const clean = pickPersistedUserProfile(slice)
  localStorage.setItem(USER_INFO_STORAGE_KEY, JSON.stringify(clean))
}
