"use strict";
const api_http_client = require("../http/client.js");
const api_constants_customer = require("../constants/customer.js");
require("../../common/vendor.js");
const utils_apiResponse = require("../../utils/api-response.js");
const api_modules_authToken = require("./auth-token.js");
const utils_multipartUpload = require("../../utils/multipart-upload.js");
const utils_fileTemp = require("../../utils/file-temp.js");
const utils_cryptoGateway = require("../../utils/crypto-gateway.js");
const utils_productStatus = require("../../utils/product-status.js");
function parseData(res) {
  const body = utils_apiResponse.unwrapResponseBody(res.data);
  const ok = res.statusCode === 200 && utils_apiResponse.isApiSuccess(body);
  return { ok, data: body.data, msg: body.msg || (ok ? "成功" : "失败") };
}
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
function fetchManageProducts(shopId, pageNum = 1, pageSize = 50) {
  return new Promise((resolve) => {
    api_http_client.request({
      service: "customer",
      path: api_constants_customer.CUSTOMER_API.PRODUCT_CATALOG,
      method: "GET",
      data: { shopId },
      success: (res) => {
        const parsed = parseData(res);
        if (!parsed.ok) {
          resolve({ ok: false, rows: [], categories: [], total: 0, msg: parsed.msg });
          return;
        }
        const categories = Array.isArray(parsed.data) ? parsed.data : [];
        const rows = flattenCatalogProducts(categories);
        resolve({ ok: true, rows, categories, total: rows.length, msg: parsed.msg });
      },
      fail: (err) => resolve({ ok: false, rows: [], categories: [], total: 0, msg: err.errMsg || "网络错误" })
    });
  });
}
function buildProductBody(payload) {
  const item = {
    shopId: payload.shopId,
    productName: payload.productName,
    price: payload.price,
    status: payload.status ?? 1
  };
  if (payload.productId != null)
    item.productId = payload.productId;
  if (payload.categoryId != null)
    item.categoryId = payload.categoryId;
  if (payload.description != null)
    item.description = payload.description;
  if (payload.imageUrl != null)
    item.imageUrl = payload.imageUrl;
  if (payload.stock != null)
    item.stock = payload.stock;
  return item;
}
function fetchManageProductCategories(shopId) {
  return new Promise((resolve) => {
    api_http_client.request({
      service: "customer",
      path: api_constants_customer.CUSTOMER_API.PRODUCT_CATEGORY,
      method: "GET",
      data: { shopId },
      success: (res) => {
        const parsed = parseData(res);
        const rows = Array.isArray(parsed.data) ? parsed.data : [];
        resolve({ ok: parsed.ok, rows, msg: parsed.msg });
      },
      fail: (err) => resolve({ ok: false, rows: [], msg: err.errMsg || "网络错误" })
    });
  });
}
function addManageProductCategories(items) {
  return new Promise((resolve) => {
    if (!(items == null ? void 0 : items.length)) {
      resolve({ ok: true, msg: "分类新增成功" });
      return;
    }
    api_http_client.request({
      service: "customer",
      path: api_constants_customer.CUSTOMER_API.PRODUCT_CATEGORY,
      method: "POST",
      data: items.map(
        (item) => Object.fromEntries(
          Object.entries({
            shopId: item.shopId,
            categoryName: item.categoryName,
            sortOrder: item.sortOrder
          }).filter(([, v]) => v != null)
        )
      ),
      success: (res) => {
        const body = utils_apiResponse.unwrapResponseBody(res.data);
        const ok = res.statusCode === 200 && utils_apiResponse.isApiSuccess(body);
        resolve({ ok, msg: body.msg || (ok ? "分类新增成功" : "添加失败") });
      },
      fail: (err) => resolve({ ok: false, msg: err.errMsg || "网络错误" })
    });
  });
}
function updateManageProductCategories(items) {
  return new Promise((resolve) => {
    if (!(items == null ? void 0 : items.length)) {
      resolve({ ok: true, msg: "分类更新成功" });
      return;
    }
    api_http_client.request({
      service: "customer",
      path: api_constants_customer.CUSTOMER_API.PRODUCT_CATEGORY,
      method: "PUT",
      data: items.map(
        (item) => Object.fromEntries(
          Object.entries({
            categoryId: item.categoryId,
            categoryName: item.categoryName,
            sortOrder: item.sortOrder
          }).filter(([, v]) => v != null)
        )
      ),
      success: (res) => {
        const body = utils_apiResponse.unwrapResponseBody(res.data);
        const ok = res.statusCode === 200 && utils_apiResponse.isApiSuccess(body);
        resolve({ ok, msg: body.msg || (ok ? "分类更新成功" : "更新失败") });
      },
      fail: (err) => resolve({ ok: false, msg: err.errMsg || "网络错误" })
    });
  });
}
function deleteManageProductCategory(categoryId) {
  return new Promise((resolve) => {
    api_http_client.request({
      service: "customer",
      path: `${api_constants_customer.CUSTOMER_API.PRODUCT_CATEGORY}/${categoryId}`,
      method: "DELETE",
      success: (res) => {
        const body = utils_apiResponse.unwrapResponseBody(res.data);
        const ok = res.statusCode === 200 && utils_apiResponse.isApiSuccess(body);
        resolve({ ok, msg: body.msg || (ok ? "分类删除成功" : "删除失败") });
      },
      fail: (err) => resolve({ ok: false, msg: err.errMsg || "网络错误" })
    });
  });
}
function parseMutationResponse(res) {
  utils_cryptoGateway.maybeDecryptResponse(res);
  const body = utils_apiResponse.unwrapResponseBody(res.data);
  const ok = res.statusCode === 200 && utils_apiResponse.isApiSuccess(body);
  return { ok, msg: body.msg || (ok ? "保存成功" : "保存失败") };
}
function buildProductMultipartParts(metaList, imagePaths = []) {
  const parts = [
    {
      name: "meta",
      data: JSON.stringify(metaList),
      filename: "meta.json",
      contentType: "application/json"
    }
  ];
  for (const filePath of imagePaths) {
    if (filePath && utils_fileTemp.isLocalUploadPath(filePath)) {
      parts.push({ name: "images", filePath });
    }
  }
  return parts;
}
function buildProductUpdateMultipartParts(meta, imagePath) {
  const parts = [
    {
      name: "meta",
      data: JSON.stringify(meta),
      filename: "meta.json",
      contentType: "application/json"
    }
  ];
  if (imagePath && utils_fileTemp.isLocalUploadPath(imagePath)) {
    parts.push({ name: "image", filePath: imagePath });
  }
  return parts;
}
function submitProductMultipart(url, metaList, imagePaths = []) {
  const token = api_modules_authToken.getToken();
  const header = {};
  if (token)
    header.Authorization = `Bearer ${token}`;
  return utils_multipartUpload.uploadMultipartForm({
    url,
    parts: buildProductMultipartParts(metaList, imagePaths),
    header,
    timeout: 12e4
  }).then(parseMutationResponse);
}
function addManageProducts(items) {
  const list = Array.isArray(items) ? items : [items];
  const metaList = list.map(({ imagePath, ...rest }) => buildProductBody(rest));
  const imagePaths = list.map((item) => utils_fileTemp.isLocalUploadPath(item.imagePath) ? item.imagePath : null);
  const paths = imagePaths.filter(Boolean);
  const metaWithIndex = metaList.map((meta, i) => {
    if (!imagePaths[i])
      return meta;
    const imageIndex = paths.indexOf(imagePaths[i]);
    return imageIndex >= 0 ? { ...meta, imageIndex } : meta;
  });
  return submitProductMultipart(api_http_client.buildGatewayUrl("customer", api_constants_customer.CUSTOMER_API.PRODUCT), metaWithIndex, paths);
}
function addManageProduct(payload) {
  return addManageProducts([payload]);
}
function updateManageProduct(productId, payload) {
  const { imagePath, ...rest } = payload || {};
  const body = buildProductBody({ ...rest, productId: Number(productId), status: void 0 });
  const token = api_modules_authToken.getToken();
  const header = {};
  if (token)
    header.Authorization = `Bearer ${token}`;
  return utils_multipartUpload.uploadMultipartForm({
    url: api_http_client.buildGatewayUrl("customer", api_constants_customer.CUSTOMER_API.PRODUCT),
    method: "PUT",
    parts: buildProductUpdateMultipartParts(body, imagePath),
    header,
    timeout: 12e4
  }).then((res) => {
    const parsed = parseMutationResponse(res);
    return {
      ok: parsed.ok,
      msg: parsed.msg,
      imageUrl: parsed.ok ? utils_apiResponse.pickApiData(utils_apiResponse.unwrapResponseBody(res.data)) : void 0
    };
  }).catch((err) => ({ ok: false, msg: err.message || "上传失败" }));
}
function updateManageProductStock(productId, stock) {
  if (productId == null || stock == null || stock === "") {
    return Promise.resolve({ ok: false, msg: "商品ID与库存数量不能为空" });
  }
  return new Promise((resolve) => {
    api_http_client.request({
      service: "customer",
      path: `${api_constants_customer.CUSTOMER_API.PRODUCT}/stock`,
      method: "PUT",
      data: {
        productId: Number(productId),
        stock: Number(stock)
      },
      success: (res) => resolve(parseMutationResponse(res)),
      fail: (err) => resolve({ ok: false, msg: err.errMsg || "网络错误" })
    });
  });
}
function updateManageProductStatus(productId, status) {
  if (status !== utils_productStatus.PRODUCT_STATUS.ON_SALE && status !== utils_productStatus.PRODUCT_STATUS.OFF_SHELF) {
    return Promise.resolve({ ok: false, msg: "仅支持设置为出售中或下架" });
  }
  return new Promise((resolve) => {
    api_http_client.request({
      service: "customer",
      path: `${api_constants_customer.CUSTOMER_API.PRODUCT}/${productId}/status?status=${status}`,
      method: "PUT",
      success: (res) => {
        const body = utils_apiResponse.unwrapResponseBody(res.data);
        const ok = res.statusCode === 200 && utils_apiResponse.isApiSuccess(body);
        resolve({ ok, msg: body.msg || (ok ? "状态更新成功" : "更新失败") });
      },
      fail: (err) => resolve({ ok: false, msg: err.errMsg || "网络错误" })
    });
  });
}
function deleteManageProduct(productId) {
  return new Promise((resolve) => {
    api_http_client.request({
      service: "customer",
      path: `${api_constants_customer.CUSTOMER_API.PRODUCT}/${productId}`,
      method: "DELETE",
      success: (res) => {
        const body = utils_apiResponse.unwrapResponseBody(res.data);
        const ok = res.statusCode === 200 && utils_apiResponse.isApiSuccess(body);
        resolve({ ok, msg: body.msg || (ok ? "删除成功" : "删除失败") });
      },
      fail: (err) => resolve({ ok: false, msg: err.errMsg || "网络错误" })
    });
  });
}
function fetchManageActivities(shopId, pageNum = 1, pageSize = 50) {
  return new Promise((resolve) => {
    api_http_client.request({
      service: "customer",
      path: api_constants_customer.CUSTOMER_API.ACTIVITY_LIST,
      method: "GET",
      data: { shopId, pageNum, pageSize },
      success: (res) => resolve(parsePage(res)),
      fail: (err) => resolve({ ok: false, rows: [], total: 0, msg: err.errMsg || "网络错误" })
    });
  });
}
function fetchManageActivityDetail(activityId) {
  return new Promise((resolve) => {
    api_http_client.request({
      service: "customer",
      path: `${api_constants_customer.CUSTOMER_API.ACTIVITY}/${activityId}`,
      method: "GET",
      success: (res) => resolve(parseData(res)),
      fail: (err) => resolve({ ok: false, data: null, msg: err.errMsg || "网络错误" })
    });
  });
}
function buildActivityPayload(payload) {
  var _a;
  const kind = payload.kind ?? 1;
  const item = {
    shopId: payload.shopId,
    activityName: payload.activityName,
    kind,
    description: ((_a = payload.description) == null ? void 0 : _a.trim()) || null,
    startTime: payload.startTime ?? null,
    endTime: payload.endTime ?? null
  };
  if (kind === 1) {
    item.activityType = payload.activityType;
    item.rules = payload.rules;
  }
  return item;
}
function saveManageActivity(payload) {
  const activityId = payload == null ? void 0 : payload.activityId;
  const isUpdate = activityId != null && activityId !== "";
  return new Promise((resolve) => {
    api_http_client.request({
      service: "customer",
      path: isUpdate ? `${api_constants_customer.CUSTOMER_API.ACTIVITY}/${activityId}` : api_constants_customer.CUSTOMER_API.ACTIVITY,
      method: isUpdate ? "PUT" : "POST",
      data: buildActivityPayload(payload),
      success: (res) => {
        const body = utils_apiResponse.unwrapResponseBody(res.data);
        const ok = res.statusCode === 200 && utils_apiResponse.isApiSuccess(body);
        resolve({ ok, data: body.data, msg: body.msg || (ok ? "保存成功" : "保存失败") });
      },
      fail: (err) => resolve({ ok: false, msg: err.errMsg || "网络错误" })
    });
  });
}
function fetchManageCoupons(shopId, pageNum = 1, pageSize = 50) {
  return new Promise((resolve) => {
    api_http_client.request({
      service: "customer",
      path: api_constants_customer.CUSTOMER_API.COUPON_TEMPLATE_LIST,
      method: "GET",
      data: { shopId, pageNum, pageSize },
      success: (res) => resolve(parsePage(res)),
      fail: (err) => resolve({ ok: false, rows: [], total: 0, msg: err.errMsg || "网络错误" })
    });
  });
}
function addManageCoupon(payload) {
  return new Promise((resolve) => {
    api_http_client.request({
      service: "customer",
      path: api_constants_customer.CUSTOMER_API.COUPON_TEMPLATE,
      method: "POST",
      data: payload,
      success: (res) => {
        const body = utils_apiResponse.unwrapResponseBody(res.data);
        const ok = res.statusCode === 200 && utils_apiResponse.isApiSuccess(body);
        resolve({ ok, msg: body.msg || (ok ? "添加成功" : "添加失败") });
      },
      fail: (err) => resolve({ ok: false, msg: err.errMsg || "网络错误" })
    });
  });
}
function addManageCouponStock(templateId, payload) {
  return new Promise((resolve) => {
    api_http_client.request({
      service: "customer",
      path: `${api_constants_customer.CUSTOMER_API.COUPON_TEMPLATE_STOCK}/${templateId}/stock`,
      method: "PUT",
      data: {
        shopId: payload.shopId,
        addQuantity: payload.addQuantity
      },
      success: (res) => {
        const body = utils_apiResponse.unwrapResponseBody(res.data);
        const ok = res.statusCode === 200 && utils_apiResponse.isApiSuccess(body);
        resolve({ ok, msg: body.msg || (ok ? "库存追加成功" : "操作失败") });
      },
      fail: (err) => resolve({ ok: false, msg: err.errMsg || "网络错误" })
    });
  });
}
function adjustManageCouponDistribution(templateId, payload) {
  return new Promise((resolve) => {
    api_http_client.request({
      service: "customer",
      path: `${api_constants_customer.CUSTOMER_API.COUPON_TEMPLATE}/${templateId}/distribution`,
      method: "PUT",
      data: {
        shopId: payload.shopId,
        distributionStartTime: payload.distributionStartTime,
        distributionEndTime: payload.distributionEndTime
      },
      success: (res) => {
        const body = utils_apiResponse.unwrapResponseBody(res.data);
        const ok = res.statusCode === 200 && utils_apiResponse.isApiSuccess(body);
        resolve({ ok, msg: body.msg || (ok ? "更新成功" : "操作失败") });
      },
      fail: (err) => resolve({ ok: false, msg: err.errMsg || "网络错误" })
    });
  });
}
function stopManageCouponDistribution(templateId, shopId) {
  return new Promise((resolve) => {
    api_http_client.request({
      service: "customer",
      path: `${api_constants_customer.CUSTOMER_API.COUPON_TEMPLATE}/${templateId}/stop?shopId=${shopId}`,
      method: "PUT",
      success: (res) => {
        const body = utils_apiResponse.unwrapResponseBody(res.data);
        const ok = res.statusCode === 200 && utils_apiResponse.isApiSuccess(body);
        resolve({ ok, msg: body.msg || (ok ? "已停止发放" : "操作失败") });
      },
      fail: (err) => resolve({ ok: false, msg: err.errMsg || "网络错误" })
    });
  });
}
function resumeManageCouponDistribution(templateId, shopId) {
  return new Promise((resolve) => {
    api_http_client.request({
      service: "customer",
      path: `${api_constants_customer.CUSTOMER_API.COUPON_TEMPLATE}/${templateId}/resume?shopId=${shopId}`,
      method: "PUT",
      success: (res) => {
        const body = utils_apiResponse.unwrapResponseBody(res.data);
        const ok = res.statusCode === 200 && utils_apiResponse.isApiSuccess(body);
        resolve({ ok, msg: body.msg || (ok ? "已恢复发放" : "操作失败") });
      },
      fail: (err) => resolve({ ok: false, msg: err.errMsg || "网络错误" })
    });
  });
}
function deleteManageCoupon(templateId) {
  return new Promise((resolve) => {
    api_http_client.request({
      service: "customer",
      path: `${api_constants_customer.CUSTOMER_API.COUPON_TEMPLATE}/${templateId}`,
      method: "DELETE",
      success: (res) => {
        const body = utils_apiResponse.unwrapResponseBody(res.data);
        const ok = res.statusCode === 200 && utils_apiResponse.isApiSuccess(body);
        resolve({ ok, msg: body.msg || (ok ? "删除成功" : "删除失败") });
      },
      fail: (err) => resolve({ ok: false, msg: err.errMsg || "网络错误" })
    });
  });
}
function dateToDateTime(dateStr) {
  if (!dateStr)
    return null;
  const d = String(dateStr).trim();
  if (!d)
    return null;
  return d.length > 10 ? d.replace(" ", "T") : `${d}T00:00:00`;
}
function dateToDateTimeEnd(dateStr) {
  if (!dateStr)
    return null;
  const d = String(dateStr).trim();
  if (!d)
    return null;
  if (d.length > 10)
    return d.replace(" ", "T");
  return `${d}T23:59:59`;
}
function parsePositiveAmount(value) {
  const n = parseFloat(String(value ?? "").trim());
  if (Number.isNaN(n) || n <= 0)
    return null;
  return n;
}
function parsePositiveInt(value) {
  const n = parseInt(String(value ?? "").trim(), 10);
  if (Number.isNaN(n) || n <= 0)
    return null;
  return n;
}
function parseOptionalPositiveInt(value) {
  const raw = String(value ?? "").trim();
  if (!raw)
    return null;
  return parsePositiveInt(raw);
}
function parseOptionalAmount(value) {
  const raw = String(value ?? "").trim();
  if (!raw)
    return 0;
  const n = parseFloat(raw);
  if (Number.isNaN(n) || n < 0)
    return null;
  return n;
}
function deleteManageActivity(activityId) {
  return new Promise((resolve) => {
    api_http_client.request({
      service: "customer",
      path: `${api_constants_customer.CUSTOMER_API.ACTIVITY}/${activityId}`,
      method: "DELETE",
      success: (res) => {
        const body = utils_apiResponse.unwrapResponseBody(res.data);
        const ok = res.statusCode === 200 && utils_apiResponse.isApiSuccess(body);
        resolve({ ok, msg: body.msg || (ok ? "删除成功" : "删除失败") });
      },
      fail: (err) => resolve({ ok: false, msg: err.errMsg || "网络错误" })
    });
  });
}
function stopManageActivity(activityId, shopId) {
  return new Promise((resolve) => {
    api_http_client.request({
      service: "customer",
      path: `${api_constants_customer.CUSTOMER_API.ACTIVITY}/${activityId}/stop?shopId=${shopId}`,
      method: "PUT",
      success: (res) => {
        const body = utils_apiResponse.unwrapResponseBody(res.data);
        const ok = res.statusCode === 200 && utils_apiResponse.isApiSuccess(body);
        resolve({ ok, msg: body.msg || (ok ? "活动已停止" : "操作失败") });
      },
      fail: (err) => resolve({ ok: false, msg: err.errMsg || "网络错误" })
    });
  });
}
function enableManageActivity(activityId, shopId) {
  return new Promise((resolve) => {
    api_http_client.request({
      service: "customer",
      path: `${api_constants_customer.CUSTOMER_API.ACTIVITY}/${activityId}/enable?shopId=${shopId}`,
      method: "PUT",
      success: (res) => {
        const body = utils_apiResponse.unwrapResponseBody(res.data);
        const ok = res.statusCode === 200 && utils_apiResponse.isApiSuccess(body);
        resolve({ ok, msg: body.msg || (ok ? "活动已启用" : "操作失败") });
      },
      fail: (err) => resolve({ ok: false, msg: err.errMsg || "网络错误" })
    });
  });
}
function parseProductPoints(value) {
  const n = parseInt(String(value ?? "").trim(), 10);
  if (Number.isNaN(n) || n <= 0)
    return null;
  return n;
}
exports.addManageCoupon = addManageCoupon;
exports.addManageCouponStock = addManageCouponStock;
exports.addManageProduct = addManageProduct;
exports.addManageProductCategories = addManageProductCategories;
exports.adjustManageCouponDistribution = adjustManageCouponDistribution;
exports.dateToDateTime = dateToDateTime;
exports.dateToDateTimeEnd = dateToDateTimeEnd;
exports.deleteManageActivity = deleteManageActivity;
exports.deleteManageCoupon = deleteManageCoupon;
exports.deleteManageProduct = deleteManageProduct;
exports.deleteManageProductCategory = deleteManageProductCategory;
exports.enableManageActivity = enableManageActivity;
exports.fetchManageActivities = fetchManageActivities;
exports.fetchManageActivityDetail = fetchManageActivityDetail;
exports.fetchManageCoupons = fetchManageCoupons;
exports.fetchManageProductCategories = fetchManageProductCategories;
exports.fetchManageProducts = fetchManageProducts;
exports.parseOptionalAmount = parseOptionalAmount;
exports.parseOptionalPositiveInt = parseOptionalPositiveInt;
exports.parsePositiveAmount = parsePositiveAmount;
exports.parsePositiveInt = parsePositiveInt;
exports.parseProductPoints = parseProductPoints;
exports.resumeManageCouponDistribution = resumeManageCouponDistribution;
exports.saveManageActivity = saveManageActivity;
exports.stopManageActivity = stopManageActivity;
exports.stopManageCouponDistribution = stopManageCouponDistribution;
exports.updateManageProduct = updateManageProduct;
exports.updateManageProductCategories = updateManageProductCategories;
exports.updateManageProductStatus = updateManageProductStatus;
exports.updateManageProductStock = updateManageProductStock;
