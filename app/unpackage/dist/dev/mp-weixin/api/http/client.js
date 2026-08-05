"use strict";
const common_vendor = require("../../common/vendor.js");
const config_env = require("../../config/env.js");
const utils_cryptoGateway = require("../../utils/crypto-gateway.js");
const api_modules_authToken = require("../modules/auth-token.js");
const utils_apiResponse = require("../../utils/api-response.js");
const services_authRelogin = require("../../services/auth-relogin.js");
const utils_requestDedupe = require("../../utils/request-dedupe.js");
function normalizeBase(base) {
  return String(base || "").replace(/\/+$/, "");
}
function normalizeSegment(seg) {
  if (!seg)
    return "";
  const s = seg.startsWith("/") ? seg : `/${seg}`;
  return s.replace(/\/+$/, "");
}
function normalizePath(path) {
  if (!path)
    return "/";
  return path.startsWith("/") ? path : `/${path}`;
}
function isForbiddenAdminGatewayPath(pathname) {
  const block = normalizePath(config_env.GW_FORBIDDEN_ADMIN_PATH_PREFIX);
  const p = pathname.replace(/\/+$/, "") || "/";
  return p === block || p.startsWith(`${block}/`);
}
function buildGatewayUrl(service, path) {
  const base = normalizeBase(config_env.GATEWAY_BASE_URL);
  const prefix = normalizeSegment(config_env.GW_SERVICE_PREFIX[service]);
  const p = normalizePath(path);
  if (prefix === "") {
    throw new Error(
      `[buildGatewayUrl] 未知 service: ${String(service)}，小程序仅允许 customer（/mobi）`
    );
  }
  return `${base}${prefix}${p}`;
}
function assertNotAdminGatewayUrl(urlStr) {
  let pathname = "";
  if (/^https?:\/\//i.test(urlStr)) {
    try {
      pathname = new URL(urlStr).pathname;
    } catch {
      return;
    }
  } else {
    pathname = normalizePath(urlStr);
  }
  if (isForbiddenAdminGatewayPath(pathname)) {
    throw new Error("[request] 禁止请求管理端路径 /mobi/dashboard");
  }
}
function applyRequestCrypto(merged) {
  const cryptoOn = utils_cryptoGateway.getRequestCryptoEnabled(config_env.REQUEST_CRYPTO_ENABLED);
  if (!cryptoOn)
    return;
  const method = (merged.method || "GET").toUpperCase();
  merged.header = merged.header || {};
  if (method === "GET") {
    const getCrypto = utils_cryptoGateway.buildEncryptedGetHeaders(merged.url, true);
    if (!getCrypto)
      return;
    Object.assign(merged.header, getCrypto.headers);
    merged.__cryptoSessionKey = getCrypto.sessionKey;
    return;
  }
  if (method !== "POST" && method !== "PUT")
    return;
  if (merged.data === void 0 || merged.data === null)
    return;
  const enc = utils_cryptoGateway.buildEncryptedRequestBody(method, merged.data, merged.url, true);
  if (!enc)
    return;
  merged.data = enc.body;
  merged.__cryptoSessionKey = enc.sessionKey;
  merged.header["content-type"] = "application/json";
  merged.header[utils_cryptoGateway.HDR.ENCRYPTED_BODY] = "true";
  Object.assign(merged.header, enc.headers);
  if (String(config_env.GATEWAY_CRYPTO_MODE).toLowerCase() === "aes") {
    Object.assign(merged.header, utils_cryptoGateway.buildAesModeSecureHeaders(merged.url));
  }
}
function isLoginRequestUrl(url) {
  return String(url || "").includes("/app/login");
}
function request(options) {
  return executeRequest(options, false);
}
function executeRequest(options, retried401) {
  const { service, path, url: rawUrl, success, fail, complete, ...rest } = options;
  let url;
  if (rawUrl != null && rawUrl !== "") {
    assertNotAdminGatewayUrl(rawUrl);
    if (/^https?:\/\//i.test(rawUrl)) {
      url = rawUrl;
    } else {
      url = `${normalizeBase(config_env.GATEWAY_BASE_URL)}${normalizePath(rawUrl)}`;
    }
  } else if (service && path) {
    url = buildGatewayUrl(service, path);
  } else {
    throw new Error("[request] 请传入 service+path，或传入相对网关的 url");
  }
  const merged = { ...rest, url };
  const token = api_modules_authToken.getToken();
  if (token) {
    merged.header = { ...merged.header || {} };
    merged.header.Authorization = `Bearer ${token}`;
  }
  try {
    applyRequestCrypto(merged);
  } catch (e) {
    const errMsg = (e == null ? void 0 : e.message) || String(e);
    fail == null ? void 0 : fail({ errMsg });
    complete == null ? void 0 : complete();
    return { errMsg, abort() {
    } };
  }
  const dedupeConfig = {
    method: merged.method || "GET",
    url,
    data: merged.data,
    params: merged.params
  };
  return utils_requestDedupe.runWithDedupe(
    () => new Promise((resolve, reject) => {
      common_vendor.index.request({
        ...merged,
        success(res) {
          utils_cryptoGateway.maybeDecryptResponse(res, merged.__cryptoSessionKey);
          resolve(res);
        },
        fail: reject
      });
    }),
    dedupeConfig,
    { silent: true }
  ).then(async (res) => {
    const { tokenChanged, permChanged } = api_modules_authToken.applyResponseSession(res);
    if (tokenChanged && !permChanged) {
      await services_authRelogin.syncPermissionsAfterTokenRefresh();
    }
    const body = utils_apiResponse.unwrapResponseBody(res.data);
    if (!retried401 && !isLoginRequestUrl(url) && utils_apiResponse.isUnauthorizedResponse(res, body)) {
      try {
        const loginResult = await services_authRelogin.handleSessionUnauthorized();
        if (loginResult.ok) {
          return executeRequest(options, true);
        }
        services_authRelogin.redirectToLoginIfNeeded(loginResult);
      } catch {
        services_authRelogin.redirectToLoginIfNeeded({ ok: false });
      }
    }
    success == null ? void 0 : success(res);
    return res;
  }).catch((err) => {
    if (err == null ? void 0 : err.duplicateRequest) {
      common_vendor.index.showToast({ title: err.message || "请勿重复提交", icon: "none" });
    }
    fail == null ? void 0 : fail(err);
    throw err;
  }).finally(() => {
    complete == null ? void 0 : complete();
  });
}
exports.buildGatewayUrl = buildGatewayUrl;
exports.request = request;
