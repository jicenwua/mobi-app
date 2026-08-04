import request from '@/utils/request'

function withCreateTimeRange(params, dateRange) {
  const p = { ...params }
  delete p.dateRange
  if (Array.isArray(dateRange) && dateRange.length === 2 && dateRange[0] && dateRange[1]) {
    p.minCreateTime = `${dateRange[0]} 00:00:00`
    p.maxCreateTime = `${dateRange[1]} 23:59:59`
  }
  return p
}

/** 去掉空值，避免「全部」时仍带上 auditStatus 等参数 */
function stripEmpty(params) {
  const p = { ...params }
  Object.keys(p).forEach((key) => {
    if (p[key] === undefined || p[key] === null || p[key] === '') {
      delete p[key]
    }
  })
  return p
}

/**
 * 待审列表（未通过）：与 listShop 相同接口，auditStatus 传非 1（使用 0）
 * 后端条件：auditStatus != 1 时查询待审(0)或驳回(2)
 */
export function listShopAuditQueue(query) {
  return listShop({
    ...query,
    auditStatus: 0
  })
}

/** 全部店铺列表；auditStatus 不传表示查询全部 */
export function listShop(query) {
  return request({
    url: '/shop/list',
    method: 'get',
    params: stripEmpty(withCreateTimeRange(query, query.dateRange))
  })
}

export function getShop(id) {
  return request({
    url: `/shop/info/${id}`,
    method: 'get'
  })
}

export function addShop(data) {
  return request({
    url: '/shop',
    method: 'post',
    data
  })
}

/**
 * 新增店铺：meta JSON + 五张图片一次提交（后端 OCR 校验后再上传 OSS）
 */
export function addShopFull(meta, files) {
  const fd = new FormData()
  fd.append(
    'meta',
    new Blob([JSON.stringify(meta)], { type: 'application/json;charset=UTF-8' }),
    'meta.json'
  )
  fd.append('idCardFront', files.idCardFront)
  fd.append('idCardBack', files.idCardBack)
  fd.append('businessLicensePic', files.businessLicensePic)
  fd.append('shopExterior', files.shopExterior)
  fd.append('shopInterior', files.shopInterior)
  return request({
    url: '/shop/add-full',
    method: 'post',
    data: fd,
    timeout: 120000
  })
}

export function auditShop(data) {
  return request({
    url: '/shop/audit',
    method: 'put',
    data
  })
}

export function uploadShopFile(file) {
  const fd = new FormData()
  fd.append('file', file)
  return request({
    url: '/shop/oss/upload',
    method: 'post',
    data: fd
  })
}

export function getOssPreviewUrl(objectKey) {
  return request({
    url: '/shop/oss/preview-url',
    method: 'get',
    params: { objectKey }
  })
}

export function ocrIdCard(file) {
  const fd = new FormData()
  fd.append('file', file)
  return request({
    url: '/shop/ocr/idcard',
    method: 'post',
    data: fd
  })
}
