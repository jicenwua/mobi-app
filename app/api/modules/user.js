import {request, buildGatewayUrl} from '@/api/http/client.js'
import {uploadMultipartForm} from '@/utils/multipart-upload.js'
import {CUSTOMER_API} from '@/api/constants/customer.js'
import {getToken} from '@/api/modules/auth.js'
import {isApiSuccess, unwrapResponseBody} from '@/utils/api-response.js'
import {maybeDecryptResponse} from '@/utils/crypto-gateway.js'
import {isLocalUploadPath} from '@/utils/file-temp.js'

/**
 * 设置或修改 6 位数字支付密码 POST /app/password
 * @param {{ oldPassword?: string, newPassword: string }} params
 */
export function setPayPassword({oldPassword, newPassword}) {
    const pwd = String(newPassword || '').trim()
    if (!/^\d{6}$/.test(pwd)) {
        return Promise.resolve({ok: false, msg: '请输入 6 位数字密码'})
    }
    const data = {newPassword: pwd}
    if (oldPassword) {
        data.oldPassword = String(oldPassword).trim()
    }
    return new Promise((resolve) => {
        request({
            service: 'customer',
            path: CUSTOMER_API.PASSWORD,
            method: 'POST',
            data,
            success: (res) => {
                const body = unwrapResponseBody(res.data)
                const ok = res.statusCode === 200 && isApiSuccess(body)
                resolve({
                    ok,
                    msg: body.msg || (ok ? '设置成功' : '设置失败')
                })
            },
            fail: (err) => resolve({ok: false, msg: err.errMsg || '网络错误'})
        })
    })
}

/**
 * 校验 6 位数字支付密码 POST /app/password/verify
 * @param {string} password
 * @returns {Promise<{ ok: boolean, msg: string, type?: 'ok'|'password'|'forbidden'|'network' }>}
 */
export function verifyPayPassword(password) {
    const pwd = String(password || '').trim()
    if (!/^\d{6}$/.test(pwd)) {
        return Promise.resolve({ ok: false, msg: '请输入 6 位数字密码', type: 'password' })
    }
    return new Promise((resolve) => {
        request({
            service: 'customer',
            path: CUSTOMER_API.PASSWORD_VERIFY,
            method: 'POST',
            data: { password: pwd },
            success: (res) => {
                const body = unwrapResponseBody(res.data)
                const ok = res.statusCode === 200 && isApiSuccess(body)
                const msg = body.msg || (ok ? '校验成功' : '支付密码错误')
                if (res.statusCode === 403 || /access denied/i.test(msg)) {
                    resolve({ ok: false, msg: '无权限校验支付密码', type: 'forbidden' })
                    return
                }
                resolve({
                    ok,
                    msg,
                    type: ok ? 'ok' : 'password'
                })
            },
            fail: (err) => resolve({ ok: false, msg: err.errMsg || '网络错误', type: 'network' })
        })
    })
}

function parseUpdateUploadResponse(res) {
    maybeDecryptResponse(res)
    const body = unwrapResponseBody(res.data)
    const ok = res.statusCode === 200 && isApiSuccess(body)
    return {
        ok,
        msg: body.msg || (ok ? '更新成功' : '更新失败')
    }
}

/**
 * 更新用户资料 POST /app/update
 * multipart：nickName（昵称）+ 单个文件 file（头像，可选）
 * 微信小程序一次 uploadFile 只传一个文件，昵称走 formData，不是第二个文件部件。
 *
 * @param {{ nickName: string, avatarPath?: string, requireAvatar?: boolean }} payload
 */
export function updateUserInfo(payload) {
    const nickName = (payload?.nickName || '').trim()
    if (!nickName) {
        return Promise.resolve({ok: false, msg: '请填写昵称'})
    }

    const avatarPath = (payload?.avatarPath || '').trim()
    const requireAvatar = !!payload?.requireAvatar

    if (requireAvatar && !isLocalUploadPath(avatarPath)) {
        return Promise.resolve({ok: false, msg: '请先设置头像'})
    }

    const token = getToken()
    const header = {}
    if (token) header.Authorization = `Bearer ${token}`

    // 有本地头像：单文件上传 + 表单 nickName（点击保存时才会调用）
    if (isLocalUploadPath(avatarPath)) {
        return new Promise((resolve) => {
            uni.uploadFile({
                url: buildGatewayUrl('customer', CUSTOMER_API.UPDATE),
                filePath: avatarPath,
                name: 'file',
                formData: {
                    nickName
                },
                header,
                timeout: 60000,
                success: (res) => resolve(parseUpdateUploadResponse(res)),
                fail: (err) => resolve({ok: false, msg: err.errMsg || '上传失败'})
            })
        })
    }

    // 仅改昵称：后端 /app/update 只接受 multipart，nickName 走表单字段、file 可选
    return uploadMultipartForm({
        url: buildGatewayUrl('customer', CUSTOMER_API.UPDATE),
        header,
        timeout: 60000,
        parts: [{name: 'nickName', data: nickName, text: true}]
    })
        .then((res) => parseUpdateUploadResponse(res))
        .catch((err) => ({ok: false, msg: err?.message || '网络错误'}))
}
