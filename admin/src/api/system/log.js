import request from '@/utils/request'

/**
 * 构造 GET /system/log/list 查询参数（与 SysLogController.list 对齐）
 * 后端：username、module、address、operation(Integer)、minCreateTime、maxCreateTime
 * operation 为 OperationEnum 的 code，由后端转为文案再与库字段比对
 */
export function buildLogListParams(q) {
  const p = {}
  if (q.pageNum != null) p.pageNum = q.pageNum
  if (q.pageSize != null) p.pageSize = q.pageSize

  if (q.logType != null && q.logType !== '') {
    if (q.logType === 'login' || q.logType === 1) p.logType = 1
    else if (q.logType === 'oper' || q.logType === 2) p.logType = 2
    else {
      const n = Number(q.logType)
      if (!Number.isNaN(n)) p.logType = n
    }
  }

  if (q.status !== '' && q.status != null && q.status !== undefined) {
    const s = Number(q.status)
    if (!Number.isNaN(s)) p.status = s
  }

  const username = q.username ?? q.userName
  if (username) p.username = username

  const module = q.module ?? q.title
  if (module) p.module = module

  const address = q.address ?? q.ip ?? q.ipaddr
  if (address) p.address = address

  const rawOp = q.operation ?? q.businessType
  if (rawOp !== '' && rawOp != null && rawOp !== undefined) {
    const opNum = Number(rawOp)
    if (!Number.isNaN(opNum)) p.operation = opNum
  }

  if (q.minCreateTime) p.minCreateTime = q.minCreateTime
  if (q.maxCreateTime) p.maxCreateTime = q.maxCreateTime
  if (
    p.minCreateTime == null &&
    p.maxCreateTime == null &&
    Array.isArray(q.dateRange) &&
    q.dateRange.length === 2 &&
    q.dateRange[0] &&
    q.dateRange[1]
  ) {
    p.minCreateTime = `${q.dateRange[0]} 00:00:00`
    p.maxCreateTime = `${q.dateRange[1]} 23:59:59`
  }

  return p
}

/** 将 SysLog 转为日志管理页表格行 */
export function mapSysLogToTableRow(row) {
  if (!row) return row
  const isLogin = row.logType === 1
  const op = row.operation != null ? String(row.operation) : ''
  return {
    ...row,
    id: row.logId,
    /** 后端原始 logType：1 登录 2 操作 */
    logTypeRaw: row.logType,
    logType: isLogin ? 'login' : 'oper',
    userName: row.username ?? '',
    title: row.module ?? '',
    ipaddr: row.ip ?? '',
    loginLocation: row.location ?? '',
    costTime: row.executeTime,
    operUrl: row.url,
    operParam: row.params,
    jsonResult: row.result,
    msg: row.errorMsg || row.result || row.params || (isLogin && op ? op : '') || '',
    businessType: op,
    requestMethod: row.requestMethod,
    method: row.method
  }
}

/** 将 SysLog 转为「登录日志」独立页表格行 */
export function mapSysLogToLogininforRow(row) {
  const base = mapSysLogToTableRow(row)
  return {
    ...base,
    infoId: row.logId,
    loginTime: row.createTime,
    status: row.status == null ? '' : String(row.status)
  }
}

// 分页查询日志（统一：/system/log/list）
export function listLog(query) {
  return request({
    url: '/system/log/list',
    method: 'get',
    params: buildLogListParams(query)
  })
}

// 查询登录日志（兼容旧调用：固定 logType=1 并映射字段）
export function listLoginLog(query) {
  return listLog({
    pageNum: query.pageNum,
    pageSize: query.pageSize,
    logType: 1,
    status: query.status,
    userName: query.userName,
    ipaddr: query.ipaddr,
    dateRange: query.dateRange,
    minCreateTime: query.minCreateTime,
    maxCreateTime: query.maxCreateTime
  }).then((res) => ({
    ...res,
    data: (Array.isArray(res.data) ? res.data : []).map(mapSysLogToLogininforRow)
  }))
}

// 查询操作日志（兼容旧调用：固定 logType=2）
export function listOperLog(query) {
  return listLog({
    pageNum: query.pageNum,
    pageSize: query.pageSize,
    logType: 2,
    status: query.status,
    userName: query.operName ?? query.userName,
    title: query.title,
    businessType: query.businessType,
    dateRange: query.dateRange,
    minCreateTime: query.minCreateTime,
    maxCreateTime: query.maxCreateTime
  }).then((res) => ({
    ...res,
    data: (Array.isArray(res.data) ? res.data : []).map(mapSysLogToTableRow)
  }))
}

// 查询日志详细
export function getLog(logId) {
  return request({
    url: `/system/log/${logId}`,
    method: 'get'
  })
}

// 删除日志（支持单个和批量，路径如 /system/log/1,2,3）
export function delLog(logIds) {
  const idStr = Array.isArray(logIds) ? logIds.join(',') : String(logIds)
  return request({
    url: `/system/log/${idStr}`,
    method: 'delete'
  })
}

/** 批量删除操作日志（与 delLog 相同接口） */
export function batchDelOperLog(ids) {
  return delLog(ids)
}

/** 批量删除登录日志（与 delLog 相同接口） */
export function batchDelLoginLog(ids) {
  return delLog(ids)
}

// 清空日志
export function cleanLog() {
  return request({
    url: '/system/log/clean',
    method: 'delete'
  })
}

/** 清空操作日志 */
export function cleanOperLog() {
  return cleanLog()
}

/** 清空登录日志 */
export function cleanLoginLog() {
  return cleanLog()
}

/**
 * 解锁用户登录锁定（占位路径；若后端未实现请对接实际接口）
 */
export function unlockLoginFail(id) {
  return request({
    url: `/system/log/unlock/${id}`,
    method: 'delete'
  })
}
