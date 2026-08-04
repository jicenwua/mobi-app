/**
 * AES 加密解密工具类
 * 与后端 CryptoUtils 保持一致的加密方式：AES/CBC/PKCS5Padding
 */
import CryptoJS from 'crypto-js'

/**
 * 获取加密配置
 */
export function getEncryptionConfig() {
  return {
    // 是否开启加密（从环境变量读取，默认为 false）
    enabled: import.meta.env.VITE_APP_ENCRYPTION_ENABLED === 'true',
    // 密钥（Base64 编码，32 字节）
    secretKey: import.meta.env.VITE_APP_ENCRYPTION_SECRET_KEY || '',
    // 初始化向量 IV（Base64 编码，16 字节）
    iv: import.meta.env.VITE_APP_ENCRYPTION_IV || ''
  }
}

/**
 * 将 Base64 编码的密钥转换为 WordArray
 */
function parseBase64Key(base64Key) {
  return CryptoJS.enc.Base64.parse(base64Key)
}

/**
 * AES 加密
 * @param {string} data - 待加密的 JSON 字符串
 * @param {string} key - Base64 编码的密钥
 * @param {string} iv - Base64 编码的 IV
 * @returns {string} Base64 编码的加密结果
 */
export function aesEncrypt(data, key, iv) {
  const keyWordArray = parseBase64Key(key)
  const ivWordArray = parseBase64Key(iv)

  const encrypted = CryptoJS.AES.encrypt(data, keyWordArray, {
    iv: ivWordArray,
    mode: CryptoJS.mode.CBC,
    padding: CryptoJS.pad.Pkcs7
  })

  // 与后端 CryptoUtils 一致：仅输出原始密文的 Base64，而非 OpenSSL 格式
  return encrypted.ciphertext.toString(CryptoJS.enc.Base64)
}

/**
 * AES 解密
 * @param {string} encryptedData - Base64 编码的加密数据
 * @param {string} key - Base64 编码的密钥
 * @param {string} iv - Base64 编码的 IV
 * @returns {string} 解密后的 JSON 字符串
 */
export function aesDecrypt(encryptedData, key, iv) {
  const keyWordArray = parseBase64Key(key)
  const ivWordArray = parseBase64Key(iv)

  const decrypted = CryptoJS.AES.decrypt(
    { ciphertext: CryptoJS.enc.Base64.parse(encryptedData) },
    keyWordArray,
    {
      iv: ivWordArray,
      mode: CryptoJS.mode.CBC,
      padding: CryptoJS.pad.Pkcs7
    }
  )

  return decrypted.toString(CryptoJS.enc.Utf8)
}

/**
 * 生成随机 IV（16 字节）
 * @returns {string} Base64 编码的 IV
 */
export function generateIv() {
  const iv = CryptoJS.lib.WordArray.random(16)
  return CryptoJS.enc.Base64.stringify(iv)
}

/**
 * 加密请求体（匹配后端 CryptoGlobalFilter 的实现）
 * 后端期望格式：{ payload: "加密数据", iv: "IV" }
 * @param {object} data - 请求数据对象
 * @returns {string} JSON 字符串，包含 payload 和 iv
 */
export function encryptRequestData(data) {
  const config = getEncryptionConfig()

  if (!config.enabled || !data) {
    return data
  }

  // 将数据转为 JSON 字符串
  const jsonData = JSON.stringify(data)

  // 生成随机 IV（匹配后端的动态 IV 机制）
  const iv = generateIv()

  // 加密数据
  const encryptedPayload = aesEncrypt(jsonData, config.secretKey, iv)

  // 返回后端期望的 JSON 格式
  return JSON.stringify({
    payload: encryptedPayload,
    iv: iv
  })
}

/**
 * 解密响应体（匹配后端 CryptoGlobalFilter 的实现）
 * 后端返回格式：{ payload: "加密数据", iv: "IV" }
 * @param {string} responseData - 响应体字符串
 * @returns {object} 解密后的数据对象
 */
export function decryptResponseData(responseData) {
  const config = getEncryptionConfig()

  if (!config.enabled || !responseData) {
    return responseData
  }

  try {
    // 解析响应 JSON
    const responseJson = typeof responseData === 'string' ? JSON.parse(responseData) : responseData

    // 提取 payload 和 iv
    const encryptedPayload = responseJson.payload
    const iv = responseJson.iv

    if (!encryptedPayload || !iv) {
      return responseData
    }

    // 解密数据
    const decryptedJson = aesDecrypt(encryptedPayload, config.secretKey, iv)
    return JSON.parse(decryptedJson)
  } catch {
    throw new Error('响应数据解密失败')
  }
}

export default {
  aesEncrypt,
  aesDecrypt,
  generateIv,
  encryptRequestData,
  decryptResponseData,
  getEncryptionConfig
}
