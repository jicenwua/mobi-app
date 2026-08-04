"use strict";
const api_http_client = require("../http/client.js");
const api_constants_customer = require("../constants/customer.js");
const utils_apiResponse = require("../../utils/api-response.js");
function generatePayQrcode() {
  return new Promise((resolve) => {
    api_http_client.request({
      service: "customer",
      path: api_constants_customer.CUSTOMER_API.PAY_QRCODE_GENERATE,
      method: "GET",
      success: (res) => {
        const body = utils_apiResponse.unwrapResponseBody(res.data);
        const ok = res.statusCode === 200 && utils_apiResponse.isApiSuccess(body);
        resolve({
          ok,
          data: body.data || null,
          msg: body.msg || (ok ? "生成成功" : "生成失败")
        });
      },
      fail: (err) => resolve({ ok: false, data: null, msg: err.errMsg || "网络错误" })
    });
  });
}
function verifyPayQrcode({ shopId, token }) {
  return new Promise((resolve) => {
    api_http_client.request({
      service: "customer",
      path: api_constants_customer.CUSTOMER_API.PAY_QRCODE_VERIFY,
      method: "GET",
      data: { shopId, token },
      success: (res) => {
        const body = utils_apiResponse.unwrapResponseBody(res.data);
        const ok = res.statusCode === 200 && utils_apiResponse.isApiSuccess(body);
        resolve({
          ok,
          data: body.data || null,
          msg: body.msg || (ok ? "校验成功" : "校验失败")
        });
      },
      fail: (err) => resolve({ ok: false, data: null, msg: err.errMsg || "网络错误" })
    });
  });
}
function resolveShopInvite(token) {
  return new Promise((resolve) => {
    api_http_client.request({
      service: "customer",
      path: api_constants_customer.CUSTOMER_API.SHOP_INVITE_RESOLVE,
      method: "GET",
      data: { token },
      success: (res) => {
        const body = utils_apiResponse.unwrapResponseBody(res.data);
        const ok = res.statusCode === 200 && utils_apiResponse.isApiSuccess(body);
        const data = body.data || null;
        if (data && Array.isArray(data.picture)) {
          data.carouselImages = data.picture;
        }
        resolve({
          ok,
          data,
          msg: body.msg || (ok ? "查询成功" : "查询失败")
        });
      },
      fail: (err) => resolve({ ok: false, data: null, msg: err.errMsg || "网络错误" })
    });
  });
}
function fetchShopQrcode(shopId) {
  return new Promise((resolve) => {
    api_http_client.request({
      service: "customer",
      path: `${api_constants_customer.CUSTOMER_API.SHOP_QRCODE}/${shopId}`,
      method: "GET",
      success: (res) => {
        const body = utils_apiResponse.unwrapResponseBody(res.data);
        const ok = res.statusCode === 200 && utils_apiResponse.isApiSuccess(body);
        resolve({
          ok,
          data: body.data || null,
          msg: body.msg || (ok ? "生成成功" : "生成失败")
        });
      },
      fail: (err) => resolve({ ok: false, data: null, msg: err.errMsg || "网络错误" })
    });
  });
}
function generateOrderQrcode({ logId, shopId }) {
  return new Promise((resolve) => {
    api_http_client.request({
      service: "customer",
      path: api_constants_customer.CUSTOMER_API.ORDER_QRCODE_GENERATE,
      method: "GET",
      data: { logId, shopId },
      success: (res) => {
        const body = utils_apiResponse.unwrapResponseBody(res.data);
        const ok = res.statusCode === 200 && utils_apiResponse.isApiSuccess(body);
        resolve({
          ok,
          data: body.data || null,
          msg: body.msg || (ok ? "生成成功" : "生成失败")
        });
      },
      fail: (err) => resolve({ ok: false, data: null, msg: err.errMsg || "网络错误" })
    });
  });
}
async function generateStaffInviteQrcode() {
  try {
    const res = await api_http_client.request({
      service: "customer",
      path: api_constants_customer.CUSTOMER_API.STAFF_INVITE_QRCODE_GENERATE,
      method: "GET"
    });
    const body = utils_apiResponse.unwrapResponseBody(res.data);
    const ok = res.statusCode === 200 && utils_apiResponse.isApiSuccess(body);
    return {
      ok,
      data: body.data || null,
      msg: body.msg || (ok ? "生成成功" : "生成失败")
    };
  } catch (err) {
    return {
      ok: false,
      data: null,
      msg: (err == null ? void 0 : err.errMsg) || (err == null ? void 0 : err.message) || "网络错误"
    };
  }
}
exports.fetchShopQrcode = fetchShopQrcode;
exports.generateOrderQrcode = generateOrderQrcode;
exports.generatePayQrcode = generatePayQrcode;
exports.generateStaffInviteQrcode = generateStaffInviteQrcode;
exports.resolveShopInvite = resolveShopInvite;
exports.verifyPayQrcode = verifyPayQrcode;
