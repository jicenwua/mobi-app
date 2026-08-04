/** 后台 sys 模块通用字典（与后端枚举对齐） */

export const SYS_NORMAL_DISABLE = {
  '0': { label: '正常', type: 'success' },
  '1': { label: '停用', type: 'danger' }
}

export const SYS_SUCCESS_FAIL = {
  0: { label: '成功', type: 'success' },
  1: { label: '失败', type: 'danger' },
  '0': { label: '成功', type: 'success' },
  '1': { label: '失败', type: 'danger' }
}

export const SYS_USER_SEX = {
  '0': { label: '男', type: '' },
  '1': { label: '女', type: '' }
}

const DICT_MAP = {
  sys_normal_disable: SYS_NORMAL_DISABLE,
  sys_success_fail: SYS_SUCCESS_FAIL,
  sys_user_sex: SYS_USER_SEX
}

/**
 * @param {string} dictType
 * @param {string|number|null|undefined} value
 * @returns {{ label: string, type: string }}
 */
export function resolveDict(dictType, value) {
  const dict = DICT_MAP[dictType]
  if (!dict) {
    return { label: value == null || value === '' ? '—' : String(value), type: 'info' }
  }
  const hit = dict[value] ?? dict[String(value)]
  if (hit) {
    return hit
  }
  if (dictType === 'sys_user_sex') {
    return { label: '未知', type: '' }
  }
  return { label: value == null || value === '' ? '—' : String(value), type: 'info' }
}
