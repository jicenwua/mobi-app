import request from '@/utils/request'

// 登录
export function login(data) {
  return request({
    url: '/system/auth/login',
    method: 'post',
    data: data,
    skipSessionRefresh: true
  })
}

// 登出
export function logout() {
  return request({
    url: '/system/auth/logout',
    method: 'post'
  })
}

/** 获取登录验证码（网关：GET /mobi/dashboard/code，返回体含 uuid、img） */
export function getCodeImg() {
  return request({
    url: '/code',
    method: 'get',
    skipToken: true,
    headers: {
      Accept: 'text/plain, */*'
    }
  })
}

// 获取当前用户信息
export function getUserInfo() {
  return request({
    url: '/system/user/info',
    method: 'get'
  })
}

// 获取菜单路由
export function getMenuList() {
  return request({
    url: '/system/menu/tree',
    method: 'get'
  })
}

// 查询用户列表
export function listUser(query) {
  return request({
    url: '/system/user/list',
    method: 'post',
    data: query
  })
}

// 查询用户详细
export function getUser(userId) {
  return request({
    url: `/system/user/info/${userId}`,
    method: 'get'
  })
}

// 新增用户
export function addUser(data) {
  return request({
    url: '/system/user',
    method: 'post',
    data: data
  })
}

// 修改用户
export function updateUser(data) {
  return request({
    url: '/system/user',
    method: 'put',
    data: data
  })
}

// 删除用户（支持单个和批量）
export function delUser(userIds) {
  return request({
    url: '/system/user',
    method: 'get',
    params: { ids: userIds }
  })
}

// 修改用户状态
export function changeUserStatus(userId, status) {
  const data = {
    userId,
    status
  }
  return request({
    url: '/system/user',
    method: 'put',
    data: data
  })
}
