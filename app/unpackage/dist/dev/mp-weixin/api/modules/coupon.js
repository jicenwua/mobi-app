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
function fetchMyCoupons(params = {}) {
  return new Promise((resolve) => {
    api_http_client.request({
      service: "customer",
      path: api_constants_customer.CUSTOMER_API.COUPON_MY,
      method: "GET",
      data: params,
      success: (res) => resolve(parsePage(res)),
      fail: (err) => resolve({ ok: false, rows: [], total: 0, msg: err.errMsg || "网络错误" })
    });
  });
}
async function fetchUnusedCouponCount() {
  const res = await fetchMyCoupons({ status: 0, pageNum: 1, pageSize: 1 });
  if (!res.ok)
    return { ok: false, count: 0, msg: res.msg };
  return { ok: true, count: Number(res.total) || 0, msg: res.msg };
}
exports.fetchMyCoupons = fetchMyCoupons;
exports.fetchUnusedCouponCount = fetchUnusedCouponCount;
