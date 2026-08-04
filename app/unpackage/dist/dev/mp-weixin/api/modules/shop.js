"use strict";
Object.defineProperty(exports, Symbol.toStringTag, { value: "Module" });
const common_vendor = require("../../common/vendor.js");
const api_http_client = require("../http/client.js");
const api_constants_customer = require("../constants/customer.js");
const utils_apiResponse = require("../../utils/api-response.js");
const api_modules_authToken = require("./auth-token.js");
const utils_cryptoGateway = require("../../utils/crypto-gateway.js");
const utils_multipartUpload = require("../../utils/multipart-upload.js");
const utils_shopDetailCache = require("../../utils/shop-detail-cache.js");
const PAGE_SIZE = 10;
function cacheShopListRows(rows) {
  for (const row of rows || []) {
    const normalized = normalizeShopDetail(row);
    if (normalized)
      utils_shopDetailCache.setCachedShopDetail(normalized.id, normalized);
  }
}
function getShopCarouselImages(shop) {
  if (!shop)
    return [];
  const raw = shop.carouselImages ?? shop.picture;
  if (Array.isArray(raw))
    return raw.map((s) => (s || "").trim()).filter(Boolean);
  if (typeof raw === "string" && raw.trim()) {
    return raw.split(",").map((s) => s.trim()).filter(Boolean);
  }
  const ext = (shop.shopExterior || "").trim();
  return ext ? [ext] : [];
}
function normalizeShopDetail(row) {
  if (!row || typeof row !== "object")
    return null;
  const carouselImages = getShopCarouselImages(row);
  const shopId = row.shopId ?? row.id;
  if (shopId == null || shopId === "")
    return null;
  return {
    ...row,
    id: shopId,
    shopId,
    remainingPoints: row.remainingPoints ?? row.amount ?? row.basePoints,
    carouselImages
  };
}
function normalizeShopRow(row) {
  return normalizeShopDetail(row) ?? row;
}
function isUsableShopDetail(shop) {
  const normalized = normalizeShopDetail(shop);
  return !!((normalized == null ? void 0 : normalized.id) && (normalized.shopName || normalized.shopCode));
}
function filterShopsByName(rows, query) {
  const q = (query || "").trim().toLowerCase();
  if (!q)
    return rows;
  return rows.filter((row) => (row.shopName || "").toLowerCase().includes(q));
}
function parseShopListPage(res, pageSize = PAGE_SIZE) {
  const body = utils_apiResponse.unwrapResponseBody(res.data);
  if (utils_apiResponse.isUnauthorizedResponse(res, body)) {
    return {
      ok: false,
      unauthorized: true,
      rows: [],
      total: 0,
      hasMore: false,
      msg: body.msg || "登录已失效"
    };
  }
  const ok = res.statusCode === 200 && utils_apiResponse.isApiSuccess(body);
  const rows = utils_apiResponse.pickApiList(body).map(normalizeShopRow);
  const total = Number(body.total) || 0;
  const pageRows = rows.length;
  return {
    ok,
    rows,
    total,
    hasMore: pageRows >= pageSize,
    msg: body.msg || (ok ? "查询成功" : "加载失败")
  };
}
function formatShopAddress(shop) {
  if (!shop)
    return "";
  const parts = [shop.province, shop.city, shop.district, shop.address].map((s) => (s || "").trim()).filter(Boolean);
  return parts.join("");
}
function fetchUserShops({ pageNum, query }) {
  const data = {
    pageNum,
    pageSize: PAGE_SIZE
  };
  return new Promise((resolve) => {
    api_http_client.request({
      service: "customer",
      path: api_constants_customer.CUSTOMER_API.SHOP_LIST,
      method: "GET",
      data,
      success: (res) => {
        const parsed = parseShopListPage(res);
        if (!parsed.ok) {
          resolve(parsed);
          return;
        }
        const rows = filterShopsByName(parsed.rows, query);
        cacheShopListRows(rows);
        resolve({
          ...parsed,
          rows
        });
      },
      fail: (err) => resolve({ ok: false, rows: [], hasMore: false, msg: err.errMsg || "网络错误" })
    });
  });
}
function getTempBasePath() {
  var _a, _b;
  if (typeof common_vendor.wx$1 !== "undefined" && ((_a = common_vendor.wx$1.env) == null ? void 0 : _a.USER_DATA_PATH)) {
    return common_vendor.wx$1.env.USER_DATA_PATH;
  }
  return `${((_b = common_vendor.index.env) == null ? void 0 : _b.USER_DATA_PATH) || ""}`;
}
function writeMetaTempFile(meta) {
  return new Promise((resolve, reject) => {
    const base = getTempBasePath();
    if (!base) {
      reject(new Error("无法写入临时目录"));
      return;
    }
    const filePath = `${base}/shop_meta_${Date.now()}.json`;
    common_vendor.index.getFileSystemManager().writeFile({
      filePath,
      data: JSON.stringify(meta),
      encoding: "utf8",
      success: () => resolve(filePath),
      fail: (e) => reject(new Error(e.errMsg || "写入表单失败"))
    });
  });
}
function buildShopUploadParts(meta, files) {
  const parts = [
    {
      name: "meta",
      data: JSON.stringify(meta),
      filename: "meta.json",
      contentType: "application/json"
    },
    { name: "idCardFront", filePath: files.idCardFront },
    { name: "idCardBack", filePath: files.idCardBack },
    { name: "businessLicensePic", filePath: files.businessLicensePic },
    { name: "shopExterior", filePath: files.shopExterior },
    { name: "shopInterior", filePath: files.shopInterior }
  ];
  for (const filePath of files.carouselImages || []) {
    if (filePath)
      parts.push({ name: "carouselImages", filePath });
  }
  return parts.filter((p) => p.filePath || p.data != null);
}
function parseShopAddFullResponse(res) {
  utils_cryptoGateway.maybeDecryptResponse(res);
  const body = utils_apiResponse.unwrapResponseBody(res.data);
  const ok = res.statusCode === 200 && utils_apiResponse.isApiSuccess(body);
  return {
    ok,
    id: body.data,
    msg: body.msg || (ok ? "提交成功，请等待审核" : "提交失败")
  };
}
function submitShopAddFull(meta, files) {
  return new Promise(async (resolve) => {
    try {
      const url = api_http_client.buildGatewayUrl("customer", api_constants_customer.CUSTOMER_API.SHOP_ADD_FULL);
      const token = api_modules_authToken.getToken();
      const header = {};
      if (token)
        header.Authorization = `Bearer ${token}`;
      const res = await utils_multipartUpload.uploadMultipartForm({
        url,
        parts: buildShopUploadParts(meta, files),
        header,
        timeout: 12e4
      });
      resolve(parseShopAddFullResponse(res));
      return;
      const metaPath = await writeMetaTempFile(meta);
      const uploadFiles = [
        { name: "meta", uri: metaPath },
        { name: "idCardFront", uri: files.idCardFront },
        { name: "idCardBack", uri: files.idCardBack },
        { name: "businessLicensePic", uri: files.businessLicensePic },
        { name: "shopExterior", uri: files.shopExterior },
        { name: "shopInterior", uri: files.shopInterior }
      ];
      for (const uri of files.carouselImages || []) {
        if (uri)
          uploadFiles.push({ name: "carouselImages", uri });
      }
      common_vendor.index.uploadFile({
        url,
        files: uploadFiles,
        header,
        timeout: 12e4,
        success: (res2) => resolve(parseShopAddFullResponse(res2)),
        fail: (err) => resolve({ ok: false, msg: err.errMsg || "上传失败" })
      });
    } catch (e) {
      resolve({ ok: false, msg: e.message || "提交失败" });
    }
  });
}
function fetchMemberShops({ pageNum }) {
  return fetchUserShops({ pageNum });
}
function fetchEnterShopByCode(shopCode) {
  const code = (shopCode || "").trim();
  return new Promise((resolve) => {
    api_http_client.request({
      service: "customer",
      path: `${api_constants_customer.CUSTOMER_API.SHOP_ENTER_INFO}/${encodeURIComponent(code)}`,
      method: "GET",
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
async function fetchShopFromUserList(shopId) {
  const targetId = shopId != null && shopId !== "" ? String(shopId) : "";
  if (!targetId) {
    return { ok: false, data: null, msg: "店铺信息无效" };
  }
  const cached = utils_shopDetailCache.getCachedShopDetail(targetId);
  if (cached) {
    return { ok: true, data: cached, msg: "查询成功" };
  }
  let pageNum = 1;
  while (pageNum <= 20) {
    const res = await fetchUserShops({ pageNum });
    if (!res.ok) {
      return { ok: false, data: null, msg: res.msg || "加载失败" };
    }
    const found = (res.rows || []).find((row) => String(row.id) === targetId);
    if (found) {
      const data = normalizeShopDetail(found);
      if (data)
        utils_shopDetailCache.setCachedShopDetail(targetId, data);
      return { ok: true, data, msg: "查询成功" };
    }
    if (!res.hasMore)
      break;
    pageNum += 1;
  }
  return { ok: false, data: null, msg: "店铺不存在" };
}
function parseListBody(body) {
  return utils_apiResponse.pickApiList(body);
}
function flattenCatalogProducts(categories) {
  const rows = [];
  for (const category of categories || []) {
    for (const product of (category == null ? void 0 : category.products) || []) {
      if (product)
        rows.push(product);
    }
  }
  return rows;
}
function catalogRequest(shopId) {
  return new Promise((resolve) => {
    api_http_client.request({
      service: "customer",
      path: api_constants_customer.CUSTOMER_API.PRODUCT_CATALOG,
      method: "GET",
      data: { shopId },
      success: (res) => {
        const body = utils_apiResponse.unwrapResponseBody(res.data);
        const ok = res.statusCode === 200 && utils_apiResponse.isApiSuccess(body);
        const categories = ok && Array.isArray(body.data) ? body.data : [];
        resolve({
          ok,
          data: categories,
          rows: ok ? flattenCatalogProducts(categories) : [],
          msg: body.msg || (ok ? "查询成功" : "加载失败")
        });
      },
      fail: (err) => resolve({ ok: false, data: null, rows: [], msg: err.errMsg || "网络错误" })
    });
  });
}
function fetchShopProductCatalog(shopId) {
  return catalogRequest(shopId);
}
function fetchShopOngoingActivities(shopId) {
  return new Promise((resolve) => {
    api_http_client.request({
      service: "customer",
      path: api_constants_customer.CUSTOMER_API.ACTIVITY_ONGOING,
      method: "GET",
      data: { shopId },
      success: (res) => {
        const body = utils_apiResponse.unwrapResponseBody(res.data);
        const ok = res.statusCode === 200 && utils_apiResponse.isApiSuccess(body);
        resolve({
          ok,
          rows: ok ? parseListBody(body) : [],
          msg: body.msg || (ok ? "查询成功" : "加载失败")
        });
      },
      fail: (err) => resolve({ ok: false, rows: [], msg: err.errMsg || "网络错误" })
    });
  });
}
function fetchShopLatestAnnouncement(shopId) {
  return new Promise((resolve) => {
    api_http_client.request({
      service: "customer",
      path: api_constants_customer.CUSTOMER_API.ACTIVITY_ANNOUNCEMENT_LATEST,
      method: "GET",
      data: { shopId },
      success: (res) => {
        const body = utils_apiResponse.unwrapResponseBody(res.data);
        const ok = res.statusCode === 200 && utils_apiResponse.isApiSuccess(body);
        resolve({
          ok,
          data: ok ? body.data || null : null,
          msg: body.msg || (ok ? "查询成功" : "加载失败")
        });
      },
      fail: (err) => resolve({ ok: false, data: null, msg: err.errMsg || "网络错误" })
    });
  });
}
function fetchShopDistributingCoupons(shopId) {
  return new Promise((resolve) => {
    api_http_client.request({
      service: "customer",
      path: api_constants_customer.CUSTOMER_API.COUPON_DISTRIBUTING,
      method: "GET",
      data: { shopId },
      success: (res) => {
        const body = utils_apiResponse.unwrapResponseBody(res.data);
        const ok = res.statusCode === 200 && utils_apiResponse.isApiSuccess(body);
        resolve({
          ok,
          rows: ok ? parseListBody(body) : [],
          msg: body.msg || (ok ? "查询成功" : "加载失败")
        });
      },
      fail: (err) => resolve({ ok: false, rows: [], msg: err.errMsg || "网络错误" })
    });
  });
}
async function fetchShopMemberExtras(shopId, { includeCoupons = true } = {}) {
  const id = shopId != null && shopId !== "" ? Number(shopId) : NaN;
  if (Number.isNaN(id)) {
    return {
      ok: false,
      activities: [],
      announcement: null,
      coupons: [],
      msg: "店铺信息无效"
    };
  }
  const [actRes, annRes, couponRes] = await Promise.all([
    fetchShopOngoingActivities(id),
    fetchShopLatestAnnouncement(id),
    includeCoupons ? fetchShopDistributingCoupons(id) : Promise.resolve({ ok: true, rows: [] })
  ]);
  const ok = actRes.ok || annRes.ok || couponRes.ok;
  const msgs = [actRes, annRes, couponRes].filter((r) => !r.ok && r.msg).map((r) => r.msg);
  return {
    ok,
    activities: actRes.ok ? actRes.rows : [],
    announcement: annRes.ok ? annRes.data : null,
    coupons: couponRes.ok ? couponRes.rows : [],
    msg: ok ? "查询成功" : msgs[0] || "加载失败"
  };
}
function claimShopCoupon(templateId, shopId) {
  return new Promise((resolve) => {
    api_http_client.request({
      service: "customer",
      path: api_constants_customer.CUSTOMER_API.COUPON_CLAIM,
      method: "POST",
      data: {
        templateId,
        shopId: shopId != null && shopId !== "" ? Number(shopId) : void 0
      },
      success: (res) => {
        const body = utils_apiResponse.unwrapResponseBody(res.data);
        const ok = res.statusCode === 200 && utils_apiResponse.isApiSuccess(body);
        resolve({
          ok,
          data: body.data,
          msg: body.msg || (ok ? "领取成功" : "领取失败")
        });
      },
      fail: (err) => resolve({ ok: false, data: null, msg: err.errMsg || "网络错误" })
    });
  });
}
const ENTER_TIME_COOLDOWN_MS = 6e4;
const enterTimeLastAt = /* @__PURE__ */ new Map();
function reportShopEnterTimeAsync(shopId) {
  const id = shopId != null && shopId !== "" ? String(shopId) : "";
  if (!id)
    return;
  const now = Date.now();
  const last = enterTimeLastAt.get(id);
  if (last != null && now - last < ENTER_TIME_COOLDOWN_MS)
    return;
  enterTimeLastAt.set(id, now);
  api_http_client.request({
    service: "customer",
    path: api_constants_customer.CUSTOMER_API.SHOP_ENTER_TIME,
    method: "GET",
    data: { shopId: Number(id) },
    success: () => {
    },
    fail: () => {
      enterTimeLastAt.delete(id);
    }
  });
}
function joinShop(shopId) {
  return new Promise((resolve) => {
    api_http_client.request({
      service: "customer",
      path: api_constants_customer.CUSTOMER_API.SHOP_ENTER,
      method: "POST",
      data: { shopId },
      success: (res) => {
        const body = utils_apiResponse.unwrapResponseBody(res.data);
        const ok = res.statusCode === 200 && utils_apiResponse.isApiSuccess(body);
        resolve({
          ok,
          msg: body.msg || (ok ? "加入成功" : "加入失败")
        });
      },
      fail: (err) => resolve({ ok: false, msg: err.errMsg || "网络错误" })
    });
  });
}
function addShopStaffByToken({ shopId, token }) {
  return new Promise((resolve) => {
    api_http_client.request({
      service: "customer",
      path: api_constants_customer.CUSTOMER_API.SHOP_STAFF_ADD,
      method: "POST",
      data: { shopId, token },
      success: (res) => {
        const body = utils_apiResponse.unwrapResponseBody(res.data);
        const ok = res.statusCode === 200 && utils_apiResponse.isApiSuccess(body);
        resolve({
          ok,
          data: body.data || null,
          msg: body.msg || (ok ? "添加成功" : "添加失败")
        });
      },
      fail: (err) => resolve({ ok: false, data: null, msg: err.errMsg || "网络错误" })
    });
  });
}
function fetchShopStaffList(shopId) {
  return new Promise((resolve) => {
    api_http_client.request({
      service: "customer",
      path: api_constants_customer.CUSTOMER_API.SHOP_STAFF_LIST,
      method: "GET",
      data: { shopId },
      success: (res) => {
        const body = utils_apiResponse.unwrapResponseBody(res.data);
        const ok = res.statusCode === 200 && utils_apiResponse.isApiSuccess(body);
        resolve({
          ok,
          rows: ok ? parseListBody(body) : [],
          msg: body.msg || (ok ? "查询成功" : "加载失败")
        });
      },
      fail: (err) => resolve({ ok: false, rows: [], msg: err.errMsg || "网络错误" })
    });
  });
}
function removeShopStaff({ shopId, userId }) {
  return new Promise((resolve) => {
    api_http_client.request({
      service: "customer",
      path: api_constants_customer.CUSTOMER_API.SHOP_STAFF_REMOVE,
      method: "DELETE",
      data: { shopId, userId },
      success: (res) => {
        const body = utils_apiResponse.unwrapResponseBody(res.data);
        const ok = res.statusCode === 200 && utils_apiResponse.isApiSuccess(body);
        resolve({
          ok,
          msg: body.msg || (ok ? "已移除店员" : "操作失败")
        });
      },
      fail: (err) => resolve({ ok: false, msg: err.errMsg || "网络错误" })
    });
  });
}
function fetchShopStatistics({ shopId, filterShopId, startDate, endDate }) {
  return new Promise((resolve) => {
    const data = { shopId };
    if (filterShopId != null && filterShopId !== "") {
      data.filterShopId = filterShopId;
    }
    if (startDate) {
      data.startDate = startDate;
    }
    if (endDate) {
      data.endDate = endDate;
    }
    api_http_client.request({
      service: "customer",
      path: api_constants_customer.CUSTOMER_API.SHOP_STATISTICS,
      method: "GET",
      data,
      success: (res) => {
        const body = utils_apiResponse.unwrapResponseBody(res.data);
        const ok = res.statusCode === 200 && utils_apiResponse.isApiSuccess(body);
        resolve({
          ok,
          data: ok ? body.data || null : null,
          msg: body.msg || (ok ? "查询成功" : "加载失败")
        });
      },
      fail: (err) => resolve({ ok: false, data: null, msg: err.errMsg || "网络错误" })
    });
  });
}
exports.PAGE_SIZE = PAGE_SIZE;
exports.addShopStaffByToken = addShopStaffByToken;
exports.claimShopCoupon = claimShopCoupon;
exports.fetchEnterShopByCode = fetchEnterShopByCode;
exports.fetchMemberShops = fetchMemberShops;
exports.fetchShopDistributingCoupons = fetchShopDistributingCoupons;
exports.fetchShopFromUserList = fetchShopFromUserList;
exports.fetchShopLatestAnnouncement = fetchShopLatestAnnouncement;
exports.fetchShopMemberExtras = fetchShopMemberExtras;
exports.fetchShopOngoingActivities = fetchShopOngoingActivities;
exports.fetchShopProductCatalog = fetchShopProductCatalog;
exports.fetchShopStaffList = fetchShopStaffList;
exports.fetchShopStatistics = fetchShopStatistics;
exports.fetchUserShops = fetchUserShops;
exports.formatShopAddress = formatShopAddress;
exports.getShopCarouselImages = getShopCarouselImages;
exports.isUsableShopDetail = isUsableShopDetail;
exports.joinShop = joinShop;
exports.normalizeShopDetail = normalizeShopDetail;
exports.removeShopStaff = removeShopStaff;
exports.reportShopEnterTimeAsync = reportShopEnterTimeAsync;
exports.submitShopAddFull = submitShopAddFull;
