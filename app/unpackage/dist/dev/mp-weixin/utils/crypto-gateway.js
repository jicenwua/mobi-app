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
function buildEncryptedRequestBody(method, data, fullUrl, cryptoEnabled) {
  const m = (method || "GET").toUpperCase();
  if (!cryptoEnabled || m !== "POST" && m !== "PUT")
    return null;
  if (data === void 0 || data === null)
    return null;
  const plain = typeof data === "string" ? data : JSON.stringify(data === "" ? {} : data);
  const mode = config_env.GATEWAY_CRYPTO_MODE.toLowerCase();
  if (mode === "rsa") {
    const pem = toPemPublicKey(config_env.GATEWAY_RSA_PUBLIC_KEY_BASE64);
    if (!pem) {
      throw new Error("[crypto] 请在 config/env.js 配置 GATEWAY_RSA_PUBLIC_KEY_BASE64");
    }
    const aesKeyB64 = common_vendor.CryptoJS.lib.WordArray.random(32).toString(common_vendor.CryptoJS.enc.Base64);
    const ivB64 = common_vendor.CryptoJS.lib.WordArray.random(16).toString(common_vendor.CryptoJS.enc.Base64);
    const payload = aesEncryptBase64(plain, aesKeyB64, ivB64);
    const encryptedKey = rsaEncryptUtf8(aesKeyB64, pem);
    return JSON.stringify({ encryptedKey, payload, iv: ivB64 });
  }
  if (mode === "aes") {
    const secret = String("").trim();
    if (!secret) {
      throw new Error("[crypto] AES 模式请配置 GATEWAY_AES_SECRET_BASE64");
    }
    const ivB64 = common_vendor.CryptoJS.lib.WordArray.random(16).toString(common_vendor.CryptoJS.enc.Base64);
    const payload = aesEncryptBase64(plain, secret, ivB64);
    return JSON.stringify({ payload, iv: ivB64 });
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
function maybeDecryptResponse(res) {
  const headers = res.header || res.headers || {};
  const lower = {};
  for (const k of Object.keys(headers)) {
    lower[String(k).toLowerCase()] = headers[k];
  }
  const enc = lower[HDR.ENCRYPTED.toLowerCase()];
  if (!enc || String(enc).toLowerCase() !== "true") {
    return res;
  }
  let data = res.data;
  if (typeof data === "string") {
    try {
      data = JSON.parse(data);
    } catch {
      return res;
    }
  }
  if (!data || typeof data !== "object")
    return res;
  try {
    if (data.encryptedKey != null && data.payload != null && data.iv != null) {
      const jsonText = aesDecryptBase64(data.payload, String(data.encryptedKey), String(data.iv));
      res.data = JSON.parse(jsonText);
      return res;
    }
    if (data.payload != null && data.iv != null) {
      const secret = String(config_env.GATEWAY_AES_SECRET_BASE64 || "").trim();
      if (!secret)
        return res;
      const jsonText = aesDecryptBase64(data.payload, secret, String(data.iv));
      res.data = JSON.parse(jsonText);
      return res;
    }
  } catch {
  }
  return res;
}
exports.HDR = HDR;
exports.buildAesModeSecureHeaders = buildAesModeSecureHeaders;
exports.buildEncryptedRequestBody = buildEncryptedRequestBody;
exports.getRequestCryptoEnabled = getRequestCryptoEnabled;
exports.maybeDecryptResponse = maybeDecryptResponse;
