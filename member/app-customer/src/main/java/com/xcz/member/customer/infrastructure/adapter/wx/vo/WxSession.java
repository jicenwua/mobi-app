package com.xcz.member.customer.infrastructure.adapter.wx.vo;

import lombok.Builder;

/**
 * 微信小程序登录会话（code2Session 结果）。
 *
 * @param openId     用户 openId
 * @param sessionKey 会话密钥
 */
@Builder
public record WxSession(String openId, String sessionKey) {
}
