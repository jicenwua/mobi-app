"use strict";
const common_vendor = require("../../common/vendor.js");
const api_http_client = require("../http/client.js");
const api_constants_customer = require("../constants/customer.js");
const utils_apiResponse = require("../../utils/api-response.js");
const api_modules_authToken = require("./auth-token.js");
const utils_cryptoGateway = require("../../utils/crypto-gateway.js");
const utils_fileTemp = require("../../utils/file-temp.js");
function setPayPassword({ oldPassword, newPassword }) {
  const pwd = String(newPassword || "").trim();
  if (!/^\d{6}$/.test(pwd)) {
    return Promise.resolve({ ok: false, msg: "请输入 6 位数字密码" });
  }
  const data = { newPassword: pwd };
  if (oldPassword) {
    data.oldPassword = String(oldPassword).trim();
  }
  return new Promise((resolve) => {
    api_http_client.request({
      service: "customer",
      path: api_constants_customer.CUSTOMER_API.PASSWORD,
      method: "POST",
      data,
      success: (res) => {
        const body = utils_apiResponse.unwrapResponseBody(res.data);
        const ok = res.statusCode === 200 && utils_apiResponse.isApiSuccess(body);
        resolve({
          ok,
          msg: body.msg || (ok ? "设置成功" : "设置失败")
        });
      },
      fail: (err) => resolve({ ok: false, msg: err.errMsg || "网络错误" })
    });
  });
}
function verifyPayPassword(password) {
  const pwd = String(password || "").trim();
  if (!/^\d{6}$/.test(pwd)) {
    return Promise.resolve({ ok: false, msg: "请输入 6 位数字密码", type: "password" });
  }
  return new Promise((resolve) => {
    api_http_client.request({
      service: "customer",
      path: api_constants_customer.CUSTOMER_API.PASSWORD_VERIFY,
      method: "POST",
      data: { password: pwd },
      success: (res) => {
        const body = utils_apiResponse.unwrapResponseBody(res.data);
        const ok = res.statusCode === 200 && utils_apiResponse.isApiSuccess(body);
        const msg = body.msg || (ok ? "校验成功" : "支付密码错误");
        if (res.statusCode === 403 || /access denied/i.test(msg)) {
          resolve({ ok: false, msg: "无权限校验支付密码", type: "forbidden" });
          return;
        }
        resolve({
          ok,
          msg,
          type: ok ? "ok" : "password"
        });
      },
      fail: (err) => resolve({ ok: false, msg: err.errMsg || "网络错误", type: "network" })
    });
  });
}
function parseUpdateUploadResponse(res) {
  utils_cryptoGateway.maybeDecryptResponse(res);
  const body = utils_apiResponse.unwrapResponseBody(res.data);
  const ok = res.statusCode === 200 && utils_apiResponse.isApiSuccess(body);
  return {
    ok,
    msg: body.msg || (ok ? "更新成功" : "更新失败")
  };
}
function updateUserInfo(payload) {
  const nickName = ((payload == null ? void 0 : payload.nickName) || "").trim();
  if (!nickName) {
    return Promise.resolve({ ok: false, msg: "请填写昵称" });
  }
  const avatarPath = ((payload == null ? void 0 : payload.avatarPath) || "").trim();
  const requireAvatar = !!(payload == null ? void 0 : payload.requireAvatar);
  if (requireAvatar && !utils_fileTemp.isLocalUploadPath(avatarPath)) {
    return Promise.resolve({ ok: false, msg: "请先设置头像" });
  }
  const token = api_modules_authToken.getToken();
  const header = {};
  if (token)
    header.Authorization = `Bearer ${token}`;
  if (utils_fileTemp.isLocalUploadPath(avatarPath)) {
    return new Promise((resolve) => {
      common_vendor.index.uploadFile({
        url: api_http_client.buildGatewayUrl("customer", api_constants_customer.CUSTOMER_API.UPDATE),
        filePath: avatarPath,
        name: "file",
        formData: {
          "nickname": nickName
        },
        header,
        timeout: 6e4,
        success: (res) => resolve(parseUpdateUploadResponse(res)),
        fail: (err) => resolve({ ok: false, msg: err.errMsg || "上传失败" })
      });
    });
  }
  return new Promise((resolve) => {
    api_http_client.request({
      service: "customer",
      path: api_constants_customer.CUSTOMER_API.UPDATE_NICKNAME || api_constants_customer.CUSTOMER_API.UPDATE,
      method: "POST",
      data: { nickName },
      header,
      timeout: 6e4,
      success: (res) => {
        const body = utils_apiResponse.unwrapResponseBody(res.data);
        const ok = res.statusCode === 200 && utils_apiResponse.isApiSuccess(body);
        resolve({
          ok,
          msg: body.msg || (ok ? "更新成功" : "更新失败")
        });
      },
      fail: (err) => resolve({ ok: false, msg: err.errMsg || "网络错误" })
    });
  });
}
exports.setPayPassword = setPayPassword;
exports.updateUserInfo = updateUserInfo;
exports.verifyPayPassword = verifyPayPassword;
