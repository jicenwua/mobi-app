"use strict";
const common_vendor = require("../common/vendor.js");
const config_env = require("../config/env.js");
function patchCryptoJsRandom() {
  const WordArray = common_vendor.CryptoJS.lib.WordArray;
  if (WordArray.__mobiRandomPatched)
    return;
  WordArray.random = function randomWordArray(nBytes) {
    const words = [];
    const nWords = Math.ceil(nBytes / 4);
    for (let i = 0; i < nWords; i++) {
      words.push(Math.random() * 4294967296 >>> 0);
    }
    return WordArray.create(words, nBytes);
  };
  WordArray.__mobiRandomPatched = true;
}
patchCryptoJsRandom();
const HDR = {
  ENCRYPTED_BODY: "X-Encrypted-Body",
  ENCRYPTED: "X-Encrypted",
  ENCRYPTED_KEY: "X-Encrypted-Key",
  IV: "X-IV",
  TIMESTAMP: "X-Timestamp",
  NONCE: "X-Nonce",
  SIGNATURE: "X-Signature"
};
function getRequestCryptoEnabled(defaultValue) {
  return Boolean(defaultValue);
}
function toPemPublicKey(b64) {
  const s = String(b64 || "").trim();
  if (!s)
    return "";
  if (s.includes("BEGIN PUBLIC KEY"))
    return s;
  const body = s.replace(/\s/g, "");
  const lines = body.match(/.{1,64}/g) || [];
  return `-----BEGIN PUBLIC KEY-----
${lines.join("\n")}
-----END PUBLIC KEY-----`;
}
function aesEncryptBase64(plainUtf8, aesKeyBase64, ivBase64) {
  const key = common_vendor.CryptoJS.enc.Base64.parse(aesKeyBase64);
  const iv = common_vendor.CryptoJS.enc.Base64.parse(ivBase64);
  const enc = common_vendor.CryptoJS.AES.encrypt(plainUtf8, key, {
    iv,
    mode: common_vendor.CryptoJS.mode.CBC,
    padding: common_vendor.CryptoJS.pad.Pkcs7
  });
  return enc.ciphertext.toString(common_vendor.CryptoJS.enc.Base64);
}
function aesDecryptBase64(payloadBase64, aesKeyBase64, ivBase64) {
  const key = common_vendor.CryptoJS.enc.Base64.parse(aesKeyBase64);
  const iv = common_vendor.CryptoJS.enc.Base64.parse(ivBase64);
  const decrypted = common_vendor.CryptoJS.AES.decrypt(
    { ciphertext: common_vendor.CryptoJS.enc.Base64.parse(payloadBase64) },
    key,
    { iv, mode: common_vendor.CryptoJS.mode.CBC, padding: common_vendor.CryptoJS.pad.Pkcs7 }
  );
  return common_vendor.CryptoJS.enc.Utf8.stringify(decrypted);
}
function rsaEncryptUtf8(plainText, publicKeyPem) {
  const jse = new common_vendor.JSEncrypt();
  jse.setPublicKey(publicKeyPem);
  const out = jse.encrypt(plainText);
  if (!out)
    throw new Error("RSA 加密失败，请检查 GATEWAY_RSA_PUBLIC_KEY_BASE64");
  return out;
}
function buildSignature(queryParams, timestamp, nonce, secretStr) {
  const sortedKeys = Object.keys(queryParams || {}).sort();
  let paramString = "";
  for (const k of sortedKeys) {
    paramString += `${k}=${queryParams[k]}&`;
  }
  const signSource = `${paramString}timestamp=${timestamp}&nonce=${nonce}&secret=${secretStr}`;
  return common_vendor.CryptoJS.SHA256(signSource).toString();
}
function createRsaSessionKey() {
  const pem = toPemPublicKey(config_env.GATEWAY_RSA_PUBLIC_KEY_BASE64);
  if (!pem) {
    throw new Error("[crypto] 请在 config/env.js 配置 GATEWAY_RSA_PUBLIC_KEY_BASE64");
  }
  const sessionKey = common_vendor.CryptoJS.lib.WordArray.random(32).toString(common_vendor.CryptoJS.enc.Base64);
  const encryptedKey = rsaEncryptUtf8(sessionKey, pem);
  return { sessionKey, encryptedKey };
}
function parseUrlQueryParams(fullUrl) {
  try {
    const u = fullUrl.split("?");
    if (u.length < 2)
      return {};
    const sp = new URLSearchParams(u[1]);
    const out = {};
    sp.forEach((value, key) => {
      out[key] = value;
    });
    return out;
  } catch {
    return {};
  }
}
function buildEncryptedGetHeaders(fullUrl, cryptoEnabled) {
  if (!cryptoEnabled)
    return null;
  const mode = config_env.GATEWAY_CRYPTO_MODE.toLowerCase();
  const headers = { [HDR.ENCRYPTED_BODY]: "true" };
  if (mode === "rsa") {
    const { sessionKey, encryptedKey } = createRsaSessionKey();
    headers[HDR.ENCRYPTED_KEY] = encryptedKey;
    return { sessionKey, headers };
  }
  if (mode === "aes") {
    const secret = String("").trim();
    if (!secret) {
      throw new Error("[crypto] AES 模式请配置 GATEWAY_AES_SECRET_BASE64");
    }
    Object.assign(headers, buildAesModeSecureHeaders(fullUrl));
    return { sessionKey: secret, headers };
  }
  throw new Error(`[crypto] 不支持的 GATEWAY_CRYPTO_MODE: ${config_env.GATEWAY_CRYPTO_MODE}`);
}
function buildEncryptedRequestBody(method, data, fullUrl, cryptoEnabled) {
  const m = (method || "GET").toUpperCase();
  if (!cryptoEnabled || m !== "POST" && m !== "PUT")
    return null;
  if (data === void 0 || data === null)
    return null;
  const plain = typeof data === "string" ? data : JSON.stringify(data === "" ? {} : data);
  const mode = config_env.GATEWAY_CRYPTO_MODE.toLowerCase();
  const ivB64 = common_vendor.CryptoJS.lib.WordArray.random(16).toString(common_vendor.CryptoJS.enc.Base64);
  if (mode === "rsa") {
    const { sessionKey, encryptedKey } = createRsaSessionKey();
    const payload = aesEncryptBase64(plain, sessionKey, ivB64);
    return {
      sessionKey,
      body: payload,
      headers: {
        [HDR.ENCRYPTED_KEY]: encryptedKey,
        [HDR.IV]: ivB64
      }
    };
  }
  if (mode === "aes") {
    const secret = String("").trim();
    if (!secret) {
      throw new Error("[crypto] AES 模式请配置 GATEWAY_AES_SECRET_BASE64");
    }
    const payload = aesEncryptBase64(plain, secret, ivB64);
    return {
      sessionKey: secret,
      body: payload,
      headers: {
        [HDR.IV]: ivB64
      }
    };
  }
  throw new Error(`[crypto] 不支持的 GATEWAY_CRYPTO_MODE: ${config_env.GATEWAY_CRYPTO_MODE}`);
}
function buildAesModeSecureHeaders(fullUrl) {
  const secret = String("").trim();
  const queryParams = parseUrlQueryParams(fullUrl);
  const timestamp = String(Date.now());
  const nonce = String(Date.now() % 1e6);
  const signature = buildSignature(queryParams, timestamp, nonce, secret);
  return {
    [HDR.TIMESTAMP]: timestamp,
    [HDR.NONCE]: nonce,
    [HDR.SIGNATURE]: signature
  };
}
function responseBodyLooksEncrypted(data, headersLower) {
  const ivInHeader = headersLower[HDR.IV.toLowerCase()];
  if (ivInHeader)
    return true;
  return data && typeof data === "object" && data.payload != null && data.iv != null;
}
function extractResponsePayload(data) {
  if (data == null)
    return null;
  if (typeof data === "string") {
    const trimmed = data.trim();
    if (!trimmed)
      return null;
    try {
      const parsed = JSON.parse(trimmed);
      if (typeof parsed === "string")
        return parsed;
      if (parsed && parsed.payload != null)
        return String(parsed.payload);
    } catch {
      return trimmed;
    }
  }
  if (typeof data === "object" && data.payload != null) {
    return String(data.payload);
  }
  return null;
}
function maybeDecryptResponse(res, sessionKey) {
  var _a;
  const headers = res.header || res.headers || {};
  const lower = {};
  for (const k of Object.keys(headers)) {
    lower[String(k).toLowerCase()] = headers[k];
  }
  const encHeader = lower[HDR.ENCRYPTED.toLowerCase()];
  const isEncryptedHeader = encHeader != null && String(encHeader).toLowerCase() === "true";
  if (!isEncryptedHeader && !responseBodyLooksEncrypted(res.data, lower)) {
    return res;
  }
  const key = String(sessionKey || "").trim();
  if (!key)
    return res;
  const iv = lower[HDR.IV.toLowerCase()] || (typeof res.data === "object" && ((_a = res.data) == null ? void 0 : _a.iv) != null ? String(res.data.iv) : null);
  const payload = extractResponsePayload(res.data);
  if (!iv || !payload)
    return res;
  try {
    const jsonText = aesDecryptBase64(payload, key, String(iv));
    res.data = JSON.parse(jsonText);
  } catch {
  }
  return res;
}
exports.HDR = HDR;
exports.buildAesModeSecureHeaders = buildAesModeSecureHeaders;
exports.buildEncryptedGetHeaders = buildEncryptedGetHeaders;
exports.buildEncryptedRequestBody = buildEncryptedRequestBody;
exports.getRequestCryptoEnabled = getRequestCryptoEnabled;
exports.maybeDecryptResponse = maybeDecryptResponse;
