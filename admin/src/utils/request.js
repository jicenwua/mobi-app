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

    // 如果开启了加密（multipart 表单不能加密）
    if (encryptionConfig.enabled && config.data && !(config.data instanceof FormData) && ['POST', 'PUT'].includes(config.method?.toUpperCase())) {
      // 判断是 RSA 模式还是纯 AES 模式
      const isRsaMode = import.meta.env.VITE_APP_ENCRYPTION_RSA_MODE === 'true'

      if (isRsaMode) {
        // RSA + AES 混合加密模式
        const { JSEncrypt } = await import('jsencrypt')
        const publicKey = import.meta.env.VITE_APP_ENCRYPTION_RSA_PUBLIC_KEY

        // 生成随机 AES 密钥
        const aesKey = CryptoJS.lib.WordArray.random(32).toString(CryptoJS.enc.Base64)
        const iv = generateIv()

        // RSA 加密 AES 密钥
        const encrypt = new JSEncrypt()
        encrypt.setPublicKey(publicKey)
        const encryptedKey = encrypt.encrypt(aesKey)

        // AES 加密请求体
        const jsonData = JSON.stringify(config.data)
        const encryptedPayload = aesEncrypt(jsonData, aesKey, iv)

        // 发送格式：{encryptedKey, payload, iv}
        config.data = JSON.stringify({
          encryptedKey: encryptedKey,
          payload: encryptedPayload,
          iv: iv
        })
      } else {
        // 纯 AES 加密模式（需要签名验证）
        const secretKey = encryptionConfig.secretKey
        const timestamp = Date.now().toString()
        const nonce = CryptoJS.lib.WordArray.random(16).toString(CryptoJS.enc.Hex)

        // 生成签名
        const params = {}
        if (config.params) {
          Object.assign(params, config.params)
        }
        const signature = generateSignature(params, timestamp, nonce, secretKey)

        // 添加签名请求头
        config.headers['X-Timestamp'] = timestamp
        config.headers['X-Nonce'] = nonce
        config.headers['X-Signature'] = signature

        // AES 加密请求体
        const jsonData = JSON.stringify(config.data)
        const iv = encryptionConfig.iv || generateIv()
        const encryptedPayload = aesEncrypt(jsonData, secretKey, iv)

        // 发送格式：{payload, iv}
        config.data = JSON.stringify({
          payload: encryptedPayload,
          iv: iv
        })
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

    // 解密响应体（网关响应头为 X-Encrypted，请求头为 X-Encrypted-Body）
    if (encryptionConfig.enabled && response.headers['x-encrypted'] === 'true') {
      try {
        const isRsaMode = import.meta.env.VITE_APP_ENCRYPTION_RSA_MODE === 'true'

        if (isRsaMode) {
          // RSA 模式：响应中包含 encryptedKey
          const { encryptedKey, payload, iv } = res
          const aesKey = encryptedKey // 响应中的 AES 密钥是明文的
          const decryptedJson = aesDecrypt(payload, aesKey, iv)
          res = JSON.parse(decryptedJson)
        } else {
          // 纯 AES 模式：使用配置的密钥解密
          const { payload, iv } = res
          const decryptedJson = aesDecrypt(payload, encryptionConfig.secretKey, iv)
          res = JSON.parse(decryptedJson)
        }
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
