"use strict";
const api_http_client = require("../http/client.js");
const api_constants_customer = require("../constants/customer.js");
const utils_apiResponse = require("../../utils/api-response.js");
function parsePage(res) {
  const body = utils_apiResponse.unwrapResponseBody(res.data);
  const ok = res.statusCode === 200 && utils_apiResponse.isApiSuccess(body);
  return {
    ok,
    rows: utils_apiResponse.pickApiList(body),
    total: body.total ?? 0,
    msg: body.msg || (ok ? "查询成功" : "加载失败")
  };
}
function parseData(res) {
  const body = utils_apiResponse.unwrapResponseBody(res.data);
  const ok = res.statusCode === 200 && utils_apiResponse.isApiSuccess(body);
  return {
    ok,
    data: body.data,
    msg: body.msg || (ok ? "操作成功" : "操作失败")
  };
}
function fetchTicketUnreadCount() {
  return new Promise((resolve) => {
    api_http_client.request({
      service: "customer",
      path: api_constants_customer.CUSTOMER_API.TICKET_UNREAD_COUNT,
      method: "GET",
      success: (res) => {
        const parsed = parseData(res);
        resolve({
          ok: parsed.ok,
          count: Number(parsed.data) || 0,
          msg: parsed.msg
        });
      },
      fail: (err) => resolve({ ok: false, count: 0, msg: err.errMsg || "网络错误" })
    });
  });
}
function fetchMyTickets(params = {}) {
  return new Promise((resolve) => {
    api_http_client.request({
      service: "customer",
      path: api_constants_customer.CUSTOMER_API.TICKET_MY,
      method: "GET",
      data: params,
      success: (res) => resolve(parsePage(res)),
      fail: (err) => resolve({ ok: false, rows: [], total: 0, msg: err.errMsg || "网络错误" })
    });
  });
}
function createTicket(data) {
  return new Promise((resolve) => {
    api_http_client.request({
      service: "customer",
      path: api_constants_customer.CUSTOMER_API.TICKET,
      method: "POST",
      data,
      success: (res) => resolve(parseData(res)),
      fail: (err) => resolve({ ok: false, data: null, msg: err.errMsg || "网络错误" })
    });
  });
}
function fetchTicketDetail(ticketId) {
  return new Promise((resolve) => {
    api_http_client.request({
      service: "customer",
      path: `${api_constants_customer.CUSTOMER_API.TICKET}/${ticketId}`,
      method: "GET",
      success: (res) => resolve(parseData(res)),
      fail: (err) => resolve({ ok: false, data: null, msg: err.errMsg || "网络错误" })
    });
  });
}
function replyTicket(ticketId, content) {
  return new Promise((resolve) => {
    api_http_client.request({
      service: "customer",
      path: `${api_constants_customer.CUSTOMER_API.TICKET}/${ticketId}/message`,
      method: "POST",
      data: { content },
      success: (res) => resolve(parseData(res)),
      fail: (err) => resolve({ ok: false, data: null, msg: err.errMsg || "网络错误" })
    });
  });
}
exports.createTicket = createTicket;
exports.fetchMyTickets = fetchMyTickets;
exports.fetchTicketDetail = fetchTicketDetail;
exports.fetchTicketUnreadCount = fetchTicketUnreadCount;
exports.replyTicket = replyTicket;
