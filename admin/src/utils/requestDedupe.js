/** 进行中请求 */
const pending = new Map()
/** 写操作最近一次完成时间 */
const lastDoneAt = new Map()

const MUTATING_METHODS = ['POST', 'PUT', 'PATCH', 'DELETE']
const DEFAULT_COOLDOWN_MS = 800

export class DuplicateRequestError extends Error {
  constructor(message = '请勿重复提交') {
    super(message)
    this.name = 'DuplicateRequestError'
    this.duplicateRequest = true
  }
}

function serializePart(value) {
  if (value == null) return ''
  if (typeof FormData !== 'undefined' && value instanceof FormData) {
    return '[FormData]'
  }
  if (typeof value === 'string') return value
  try {
    return JSON.stringify(value)
  } catch {
    return String(value)
  }
}

/**
 * 生成请求指纹（method + url + params + body）
 */
export function buildRequestKey(config = {}) {
  const method = String(config.method || 'GET').toUpperCase()
  const baseURL = config.baseURL || ''
  const url = config.url || ''
  const fullUrl = /^https?:\/\//i.test(url) ? url : `${baseURL}${url}`
  return [method, fullUrl, serializePart(config.params), serializePart(config.data)].join('|')
}

function isMutating(method) {
  return MUTATING_METHODS.includes(String(method || 'GET').toUpperCase())
}

/**
 * 包装单次请求：进行中合并为同一 Promise；写操作冷却期内拒绝重复
 */
export function runWithDedupe(executor, config, options = {}) {
  const { cooldownMs = DEFAULT_COOLDOWN_MS } = options
  const method = String(config.method || 'GET').toUpperCase()
  const key = buildRequestKey(config)
  const mutate = isMutating(method)

  if (mutate) {
    const last = lastDoneAt.get(key)
    if (last != null && Date.now() - last < cooldownMs) {
      return Promise.reject(new DuplicateRequestError('操作过于频繁，请稍后再试'))
    }
  }

  if (pending.has(key)) {
    return pending.get(key)
  }

  const promise = Promise.resolve()
    .then(() => executor())
    .finally(() => {
      pending.delete(key)
      if (mutate) {
        lastDoneAt.set(key, Date.now())
      }
    })

  pending.set(key, promise)
  return promise
}

function resolveAxiosConfig(instance, config) {
  const defaults = instance.defaults || {}
  const cfg = typeof config === 'string' ? { url: config } : { ...(config || {}) }
  return {
    method: (cfg.method || defaults.method || 'get').toUpperCase(),
    url: cfg.url || '',
    baseURL: cfg.baseURL ?? defaults.baseURL ?? '',
    params: cfg.params ?? defaults.params,
    data: cfg.data
  }
}

/**
 * 为 axios 实例挂载请求去重（合并进行中、写操作冷却）
 */
export function attachRequestDedupe(axiosInstance, options = {}) {
  const rawRequest = axiosInstance.request.bind(axiosInstance)
  axiosInstance.request = function dedupedRequest(config) {
    const merged = resolveAxiosConfig(axiosInstance, config)
    return runWithDedupe(() => rawRequest(config), merged, options)
  }
}
