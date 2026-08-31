"use strict";
function safeJsonParse(str) {
  try {
    return JSON.parse(str);
  } catch {
    return {};
  }
}
function unwrapResponseBody(data) {
  if (data == null || data === "")
    return {};
  if (typeof data === "string")
    return safeJsonParse(data);
  if (typeof data === "object")
    return data;
  return {};
}
function isApiSuccess(body) {
  return (body == null ? void 0 : body.code) === 200;
}
function isUnauthorizedResponse(res, body) {
  return (res == null ? void 0 : res.statusCode) === 401 || (body == null ? void 0 : body.code) === 401;
}
function isServerErrorResponse(res) {
  const code = Number(res == null ? void 0 : res.statusCode);
  return code >= 500 && code < 600;
}
function pickApiData(body) {
  return body == null ? void 0 : body.data;
}
function pickApiList(body) {
  var _a, _b;
  if (Array.isArray(body == null ? void 0 : body.data))
    return body.data;
  if (Array.isArray((_a = body == null ? void 0 : body.data) == null ? void 0 : _a.records))
    return body.data.records;
  if (Array.isArray((_b = body == null ? void 0 : body.data) == null ? void 0 : _b.list))
    return body.data.list;
  return [];
}
function parseSetPasswordFlag(data) {
  if (data == null || typeof data !== "object")
    return false;
  if (data.setPassword !== void 0)
    return !!data.setPassword;
  if (data.isSetPassword !== void 0)
    return !!data.isSetPassword;
  return false;
}
function parseLoginData(data) {
  if (data == null) {
    return {
      token: "",
      register: false,
      nickName: "",
      avatar: "",
      permission: [],
      setPassword: false
    };
  }
  if (typeof data === "string") {
    return {
      token: data,
      register: false,
      nickName: "",
      avatar: "",
      permission: [],
      setPassword: false
    };
  }
  return {
    token: data.token || data.accessToken || "",
    register: !!data.register,
    nickName: data.nickName || data.nickname || "",
    avatar: data.avatar || data.avatarUrl || "",
    permission: data.permission ?? data.permissions ?? [],
    setPassword: parseSetPasswordFlag(data)
  };
}
exports.isApiSuccess = isApiSuccess;
exports.isServerErrorResponse = isServerErrorResponse;
exports.isUnauthorizedResponse = isUnauthorizedResponse;
exports.parseLoginData = parseLoginData;
exports.pickApiData = pickApiData;
exports.pickApiList = pickApiList;
exports.unwrapResponseBody = unwrapResponseBody;
