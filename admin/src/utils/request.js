import axios from 'axios'
import { ElMessage } from 'element-plus'
import { getEncryptionConfig, aesEncrypt, aesDecrypt, generateIv } from './crypto'
import { attachRequestDedupe } from './requestDedupe'
import CryptoJS from 'crypto-js'
import { extractTokenFromResponse, getStoredToken, setStoredToken } from '@/utils/authToken'
import { refreshSessionAfterTokenChange } from '@/utils/sessionRefresh'
import { useUserStore } from '@/store/modules/user'
import {
  handleSessionExpired,
  isPublicApiRequest,
  isUnauthorizedResponse
} from '@/utils/authSession'

// 获取加密配置
const encryptionConfig = getEncryptionConfig()

/** 本次请求协商的 AES 会话密钥（响应解密复用） */
const CRYPTO_SESSION_KEY = '__cryptoSessionKey'

/**
 * 生成请求签名（纯 AES 模式需要）
 */
function generateSignature(params, timestamp, nonce, secret) {
  // 按参数名排序
  const sortedParams = Object.keys(params).sort()

  // 拼接参数字符串
  let paramString = ''
  sortedParams.forEach(key => {
    paramString += `${key}=${params[key]}&`
  })

  // 拼接完整待签名字符串
  const signSource = paramString + `timestamp=${timestamp}&nonce=${nonce}&secret=${secret}`

  // 生成 SHA-256 签名
  return CryptoJS.SHA256(signSource).toString()
}

async function applyRsaSessionKey(config) {
  const { JSEncrypt } = await import('jsencrypt')
  const publicKey = import.meta.env.VITE_APP_ENCRYPTION_RSA_PUBLIC_KEY
  const aesKey = CryptoJS.lib.WordArray.random(32).toString(CryptoJS.enc.Base64)
  const encrypt = new JSEncrypt()
  encrypt.setPublicKey(publicKey)
  const encryptedKey = encrypt.encrypt(aesKey)
  config[CRYPTO_SESSION_KEY] = aesKey
  config.headers['X-Encrypted-Key'] = encryptedKey
  return aesKey
}

function applyAesSecureHeaders(config) {
  const secretKey = encryptionConfig.secretKey
  const timestamp = Date.now().toString()
  const nonce = CryptoJS.lib.WordArray.random(16).toString(CryptoJS.enc.Hex)
  const params = {}
  if (config.params) {
    Object.assign(params, config.params)
  }
  const signature = generateSignature(params, timestamp, nonce, secretKey)
  config.headers['X-Timestamp'] = timestamp
  config.headers['X-Nonce'] = nonce
  config.headers['X-Signature'] = signature
  config[CRYPTO_SESSION_KEY] = secretKey
}

// 创建 axios 实例
const service = axios.create({
  baseURL: import.meta.env.VITE_APP_BASE_API + '/mobi/dashboard',
  timeout: 15000
})

// 相同请求进行中合并；POST/PUT 等短冷却防重复提交
attachRequestDedupe(service, { cooldownMs: 800 })
const axiosRequest = service.request.bind(service)
service.request = (config) =>
  axiosRequest(config).catch((err) => {
    if (err?.duplicateRequest) {
      ElMessage.warning(err.message || '请勿重复提交')
    }
    return Promise.reject(err)
  })

// 请求拦截器
service.interceptors.request.use(
  async config => {
    if (!isPublicApiRequest(config)) {
      const token = getStoredToken()
      if (token) {
        config.headers['Authorization'] = `Bearer ${token}`
      }
    }

    const method = config.method?.toUpperCase() || 'GET'
    const isRsaMode = import.meta.env.VITE_APP_ENCRYPTION_RSA_MODE === 'true'
    const canEncryptBody =
      encryptionConfig.enabled &&
      config.data &&
      !(config.data instanceof FormData) &&
      ['POST', 'PUT'].includes(method)

    if (encryptionConfig.enabled && method === 'GET') {
      config.headers['X-Encrypted-Body'] = 'true'
      if (isRsaMode) {
        await applyRsaSessionKey(config)
      } else {
        applyAesSecureHeaders(config)
      }
    }

    if (canEncryptBody) {
      if (isRsaMode) {
        const aesKey = await applyRsaSessionKey(config)
        const iv = generateIv()
        const jsonData = JSON.stringify(config.data)
        const encryptedPayload = aesEncrypt(jsonData, aesKey, iv)
        config.headers['X-IV'] = iv
        config.data = encryptedPayload
      } else {
        applyAesSecureHeaders(config)
        const secretKey = encryptionConfig.secretKey
        const jsonData = JSON.stringify(config.data)
        const iv = encryptionConfig.iv || generateIv()
        const encryptedPayload = aesEncrypt(jsonData, secretKey, iv)
        config.headers['X-IV'] = iv
        config.data = encryptedPayload
      }

      config.headers['X-Encrypted-Body'] = 'true'
    }

    return config
  },
  error => Promise.reject(error)
)

/** 处理响应头中的新 token（JWT 续签 / 权限版本刷新） */
async function applyResponseToken(response) {
  const newToken = extractTokenFromResponse(response)
  if (!newToken || !setStoredToken(newToken)) {
    return
  }
  try {
    const userStore = useUserStore()
    userStore.syncToken(newToken)
    const { usePermissionStore } = await import('@/store/modules/permission')
    const permissionStore = usePermissionStore()
    // 首次进入时尚未注册动态路由，只更新 token，由路由守卫 generateRoutes
    if (permissionStore.addRoutes.length === 0) {
      return
    }
    await refreshSessionAfterTokenChange()
  } catch {
    /* 权限刷新失败不影响当前响应 */
  }
}

// 响应拦截器
service.interceptors.response.use(
  async response => {
    let res = response.data

    // 解密响应体：IV 在响应头 X-IV，body 仅为密文
    const sessionKey = response.config?.[CRYPTO_SESSION_KEY]
    const iv = response.headers['x-iv'] || response.data?.iv
    const looksEncrypted =
      response.headers['x-encrypted'] === 'true' ||
      (iv && typeof response.data === 'string') ||
      (response.data?.payload && response.data?.iv)
    if (encryptionConfig.enabled && looksEncrypted && sessionKey && iv) {
      try {
        let payload = response.data
        if (typeof payload === 'string') {
          try {
            const parsed = JSON.parse(payload)
            payload = typeof parsed === 'string' ? parsed : parsed?.payload
          } catch {
            /* 保持为纯密文字符串 */
          }
        } else if (payload?.payload) {
          payload = payload.payload
        }
        const decryptedJson = aesDecrypt(payload, sessionKey, iv)
        res = JSON.parse(decryptedJson)
      } catch {
        ElMessage({
          message: '响应数据解密失败',
          type: 'error',
          duration: 5 * 1000
        })
        return Promise.reject(new Error('响应数据解密失败'))
      }
    }

    if (res?.code === 200 && !response.config?.skipSessionRefresh) {
      await applyResponseToken(response)
    }

    if (res.code !== 200) {
      if (isUnauthorizedResponse(res)) {
        await handleSessionExpired(res.msg || '登录已过期，请重新登录')
      } else {
        ElMessage({
          message: res.msg || 'Error',
          type: 'error',
          duration: 5 * 1000
        })
      }

      const err = new Error(res.msg || 'Error')
      err.code = res.code
      return Promise.reject(err)
    }
    return res
  },
  error => {
    if (isUnauthorizedResponse(null, error)) {
      const msg =
        error.response?.data?.msg ||
        error.response?.data?.message ||
        '登录已过期或未授权，请重新登录'
      handleSessionExpired(msg)
      return Promise.reject(error)
    }

      // 优先从响应体中获取后端返回的错误信息
      const responseData = error.response?.data
      let message = responseData?.msg || responseData?.message || error.message

      // 如果响应体中没有错误信息，再根据 HTTP 状态码提示
      if (!message && error.response) {
          switch (error.response.status) {
              case 400:
                  message = '请求参数错误'
                  break
              case 401:
                  message = '未授权，请重新登录'
                  break
              case 403:
                  message = '拒绝访问'
                  break
              case 404:
                  message = '请求地址不存在'
                  break
              case 500:
                  message = '服务器内部错误'
                  break
              case 502:
                  message = '网关错误'
                  break
              case 503:
                  message = '服务不可用'
                  break
              case 504:
                  message = '网关超时'
                  break
              default:
                  message = `连接错误${error.response.status}`
          }
      } else if (!message) {
          message = '连接到服务器失败'
      }

      ElMessage({
          message: message,
          type: 'error',
          duration: 5 * 1000
      })
    return Promise.reject(error)
  }
)

export default service
