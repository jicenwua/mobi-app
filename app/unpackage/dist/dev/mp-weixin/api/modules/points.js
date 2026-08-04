"use strict";
const api_http_client = require("../http/client.js");
const api_constants_customer = require("../constants/customer.js");
const utils_apiResponse = require("../../utils/api-response.js");
function parseListRows(body) {
  return utils_apiResponse.pickApiList(body);
}
function parsePage(res) {
  const body = utils_apiResponse.unwrapResponseBody(res.data);
  const ok = res.statusCode === 200 && utils_apiResponse.isApiSuccess(body);
  return {
    ok,
    rows: parseListRows(body),
    total: body.total ?? 0,
    msg: body.msg || (ok ? "查询成功" : "加载失败")
  };
}
function fetchShopPointsAccountList(params) {
  return new Promise((resolve) => {
    api_http_client.request({
      service: "customer",
      path: api_constants_customer.CUSTOMER_API.POINTS_SHOP_ACCOUNT_LIST,
      method: "GET",
      data: params,
      success: (res) => resolve(parsePage(res)),
      fail: (err) => resolve({ ok: false, rows: [], total: 0, msg: err.errMsg || "网络错误" })
    });
  });
}
function fetchShopConsumeLogList(params) {
  return new Promise((resolve) => {
    api_http_client.request({
      service: "customer",
      path: api_constants_customer.CUSTOMER_API.POINTS_SHOP_LOG_LIST,
      method: "GET",
      data: params,
      success: (res) => resolve(parsePage(res)),
      fail: (err) => resolve({ ok: false, rows: [], total: 0, msg: err.errMsg || "网络错误" })
    });
  });
}
function fetchConsumeLogDetail(logId, shopId) {
  return new Promise((resolve) => {
    api_http_client.request({
      service: "customer",
      path: `${api_constants_customer.CUSTOMER_API.POINTS_LOG_DETAIL}/${logId}`,
      method: "GET",
      data: { shopId },
      success: (res) => {
        const body = utils_apiResponse.unwrapResponseBody(res.data);
        const ok = res.statusCode === 200 && utils_apiResponse.isApiSuccess(body);
        resolve({
          ok,
          data: body.data ?? null,
          msg: body.msg || (ok ? "查询成功" : "加载失败")
        });
      },
      fail: (err) => resolve({ ok: false, data: null, msg: err.errMsg || "网络错误" })
    });
  });
}
function consumePoints(payload) {
  return new Promise((resolve) => {
    api_http_client.request({
      service: "customer",
      path: api_constants_customer.CUSTOMER_API.POINTS_CONSUME,
      method: "POST",
      data: payload,
      success: (res) => {
        const body = utils_apiResponse.unwrapResponseBody(res.data);
        const ok = res.statusCode === 200 && utils_apiResponse.isApiSuccess(body);
        resolve({
          ok,
          data: body.data,
          msg: body.msg || (ok ? "扣款成功" : "扣款失败")
        });
      },
      fail: (err) => resolve({ ok: false, data: null, msg: err.errMsg || "网络错误" })
    });
  });
}
function rechargeMemberPoints(payload) {
  return new Promise((resolve) => {
    api_http_client.request({
      service: "customer",
      path: api_constants_customer.CUSTOMER_API.POINTS_RECHARGE,
      method: "POST",
      data: payload,
      success: (res) => {
        const body = utils_apiResponse.unwrapResponseBody(res.data);
        const ok = res.statusCode === 200 && utils_apiResponse.isApiSuccess(body);
        resolve({
          ok,
          data: body.data ?? null,
          msg: body.msg || (ok ? "充值成功" : "充值失败")
        });
      },
      fail: (err) => resolve({ ok: false, data: null, msg: err.errMsg || "网络错误" })
    });
  });
}
function purchaseProducts(payload) {
  return new Promise((resolve) => {
    api_http_client.request({
      service: "customer",
      path: api_constants_customer.CUSTOMER_API.POINTS_PURCHASE,
      method: "POST",
      data: payload,
      success: (res) => {
        const body = utils_apiResponse.unwrapResponseBody(res.data);
        const ok = res.statusCode === 200 && utils_apiResponse.isApiSuccess(body);
        resolve({
          ok,
          data: body.data ?? null,
          msg: body.msg || (ok ? "购买成功" : "购买失败")
        });
      },
      fail: (err) => resolve({ ok: false, data: null, msg: err.errMsg || "网络错误" })
    });
  });
}
function fetchMyOrderList(params) {
  return new Promise((resolve) => {
    api_http_client.request({
      service: "customer",
      path: api_constants_customer.CUSTOMER_API.POINTS_MY_ORDER_LIST,
      method: "GET",
      data: params,
      success: (res) => resolve(parsePage(res)),
      fail: (err) => resolve({ ok: false, rows: [], total: 0, msg: err.errMsg || "网络错误" })
    });
  });
}
function verifyOrderQrcode(payload) {
  return new Promise((resolve) => {
    api_http_client.request({
      service: "customer",
      path: api_constants_customer.CUSTOMER_API.ORDER_VERIFY,
      method: "POST",
      data: payload,
      success: (res) => {
        const body = utils_apiResponse.unwrapResponseBody(res.data);
        const ok = res.statusCode === 200 && utils_apiResponse.isApiSuccess(body);
        resolve({
          ok,
          data: body.data ?? null,
          msg: body.msg || (ok ? "核销成功" : "核销失败")
        });
      },
      fail: (err) => resolve({ ok: false, data: null, msg: err.errMsg || "网络错误" })
    });
  });
}
function refundOrder(payload) {
  return new Promise((resolve) => {
    api_http_client.request({
      service: "customer",
      path: api_constants_customer.CUSTOMER_API.ORDER_REFUND,
      method: "POST",
      data: payload,
      success: (res) => {
        const body = utils_apiResponse.unwrapResponseBody(res.data);
        const ok = res.statusCode === 200 && utils_apiResponse.isApiSuccess(body);
        resolve({
          ok,
          data: body.data ?? null,
          msg: body.msg || (ok ? "退款成功" : "退款失败")
        });
      },
      fail: (err) => resolve({ ok: false, data: null, msg: err.errMsg || "网络错误" })
    });
  });
}
exports.consumePoints = consumePoints;
exports.fetchConsumeLogDetail = fetchConsumeLogDetail;
exports.fetchMyOrderList = fetchMyOrderList;
exports.fetchShopConsumeLogList = fetchShopConsumeLogList;
exports.fetchShopPointsAccountList = fetchShopPointsAccountList;
exports.purchaseProducts = purchaseProducts;
exports.rechargeMemberPoints = rechargeMemberPoints;
exports.refundOrder = refundOrder;
exports.verifyOrderQrcode = verifyOrderQrcode;
