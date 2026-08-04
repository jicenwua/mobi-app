/**
 * 将 Date 格式化为 YYYY-MM-DD HH:mm:ss（浏览器本地时区）
 */
function pad2(n) {
  return String(n).padStart(2, '0')
}

function formatFromDate(d) {
  if (!(d instanceof Date) || Number.isNaN(d.getTime())) return '-'
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-${pad2(d.getDate())} ${pad2(d.getHours())}:${pad2(d.getMinutes())}:${pad2(d.getSeconds())}`
}

/** 是否带 Z 或 ±HH:mm 等显式时区（需按瞬时换算到本地显示） */
function hasExplicitTimeZone(s) {
  const t = s.trim()
  return /[zZ]$|[+-]\d{2}:\d{2}$/.test(t)
}

/**
 * 表格/详情展示用时间字符串。
 * - 带 Z / +00:00 的 ISO8601：解析为绝对时间后按浏览器本地时区输出（东八区用户可见与库中本地时刻一致）。
 * - 无时区后缀的「yyyy-MM-dd HH:mm:ss」或「yyyy-MM-ddTHH:mm:ss」：按原样规范为「yyyy-MM-dd HH:mm:ss」，不做偏移换算。
 * - Java 反序列化数组 [y,M,d,h,m,s]：按分量拼接。
 */
export function formatDateTime(val) {
  if (val == null || val === '') return '-'

  if (Array.isArray(val)) {
    const [y, M = 1, d = 1, h = 0, m = 0, s = 0] = val
    return `${y}-${pad2(M)}-${pad2(d)} ${pad2(h)}:${pad2(m)}:${pad2(s)}`
  }

  if (typeof val === 'number' && Number.isFinite(val)) {
    return formatFromDate(new Date(val))
  }

  if (val instanceof Date) {
    return formatFromDate(val)
  }

  if (typeof val === 'string') {
    const s = val.trim()
    if (!s) return '-'

    const looksLikeDatetime =
      /^\d{4}-\d{2}-\d{2}[ T]\d{2}:\d{2}:\d{2}/.test(s)

    // 无时区：视为「墙上钟」与数据库一致，直接排版
    if (looksLikeDatetime && !hasExplicitTimeZone(s)) {
      return s
        .replace('T', ' ')
        .replace(/\.\d+/, '')
        .trim()
        .slice(0, 19)
    }

    const d = new Date(s)
    if (!Number.isNaN(d.getTime())) {
      return formatFromDate(d)
    }
  }

  return String(val)
}
