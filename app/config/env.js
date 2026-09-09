/**
 * 运行环境与网关配置
 * C 端仅走 customer（/mobi）；禁止小程序请求 /mobi/dashboard
 * 上线时在微信公众平台配置 GATEWAY_BASE_URL 的 HTTPS 合法域名
 */

/** 开发环境网关（.env.development 未注入时的兜底） */
const DEV_GATEWAY_BASE_URL = 'http://127.0.0.1:30000'
/** 生产环境网关（.env.production 未注入时的兜底） */
const PROD_GATEWAY_BASE_URL = 'https://www.xcenz.xyz/prod-api'

function normalizeBaseUrl(url) {
	return String(url || '').replace(/\/$/, '')
}

/**
 * 网关根地址，不含末尾斜杠。
 * 由 Vite 在编译时按模式注入 VITE_GATEWAY_BASE_URL：
 * - 运行 → .env.development
 * - 发行 → .env.production
 */
function resolveGatewayBaseUrl() {
	const fromVite = import.meta.env.VITE_GATEWAY_BASE_URL
	if (fromVite) {
		return normalizeBaseUrl(fromVite)
	}
	if (import.meta.env.PROD) {
		return PROD_GATEWAY_BASE_URL
	}
	return DEV_GATEWAY_BASE_URL
}

export const GATEWAY_BASE_URL = resolveGatewayBaseUrl()

/** 管理端路径前缀，小程序禁止访问 */
export const GW_FORBIDDEN_ADMIN_PATH_PREFIX = '/mobi/dashboard'

/** 业务服务前缀，须与网关 Path=/mobi/** 一致 */
export const GW_SERVICE_PREFIX = {
	customer: '/mobi'
}

/** 是否对 POST/PUT JSON 加密（与网关 CryptoGlobalFilter 对齐） */
export const REQUEST_CRYPTO_ENABLED = true

/** 加解密模式：rsa（混合）或 aes（纯对称） */
export const GATEWAY_CRYPTO_MODE = 'rsa'

/**
 * RSA 公钥 Base64（不含 PEM 头尾）
 * REQUEST_CRYPTO_ENABLED 为 true 且 GATEWAY_CRYPTO_MODE 为 rsa 时必填
 */
export const GATEWAY_RSA_PUBLIC_KEY_BASE64 =
	'MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAhuU8p9vr+joQAa4/FQomdcxj53nKrGlV4gbkTFuKdFOop90K49kCWkIwNZEYLReCuAlmFlf3VRT9eYy/lnGWkloJ1Ewy+gh1umc6JWnZuff2W/FOC4JlIGSrrrQq9dcWxW/LnZk4Mrzhmno3EkCBe46Ky09D8aqYQzTib/NsOC1a1JzpVXyoTQlk+k1AQPdOaVO8bJx8QgRJ8OiIf1WI5wzucUQEjCMtrbBcpQoLzdxWn8og1PEvo3P6skYTrU6sJy9bZYMAru2I73ZXPVbK5KGiQe1Ecm/fhbjx+UCe8+EWpZqzEX/IwqYWYml5GeNeLHrenvVSUFmLaGOnyQ9OHwIDAQAB'

/** AES 密钥 Base64，AES 模式时必填 */
export const GATEWAY_AES_SECRET_BASE64 = ''

/** 本地默认头像（服务端 avatar 为空时使用） */
export const DEFAULT_AVATAR_URL = '/static/default-avatar.svg'

/** 与 member Constants.AVATAR_DEFAULT 一致的服务端占位图 */
export const SERVER_DEFAULT_AVATAR_URL =
	'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
