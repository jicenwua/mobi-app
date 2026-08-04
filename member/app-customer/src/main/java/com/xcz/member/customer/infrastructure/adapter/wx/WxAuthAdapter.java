package com.xcz.member.customer.infrastructure.adapter.wx;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.binarywang.wx.miniapp.bean.WxMaPhoneNumberInfo;
import com.xcz.member.customer.infrastructure.adapter.wx.vo.WxSession;
import com.xcz.member.customer.infrastructure.cache.UserCache;
import jakarta.annotation.Resource;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.stereotype.Service;

/**
 * 微信小程序认证适配器：code 换 session、手机号授权。
 */
@Service
public class WxAuthAdapter {

    @Resource
    private WxMaService wxMaService;

    /**
     * 使用 wx.login 返回的 code 换取 openId 与 session_key。
     *
     * @param code 微信临时登录凭证
     * @return 微信会话信息
     * @throws WxErrorException 微信 API 调用失败
     */
    public WxSession code2Session(String code) throws WxErrorException {
        WxMaJscode2SessionResult sessionInfo = wxMaService.getUserService().getSessionInfo(code);
        String openid = sessionInfo.getOpenid();
        String sessionKey = sessionInfo.getSessionKey();
        UserCache.addSession(openid, sessionKey);
        return new WxSession(openid, sessionKey);
    }

    /**
     * 使用手机号授权 code 换取用户手机号。
     *
     * @param phoneCode 微信手机号授权 code
     * @return 纯数字手机号
     * @throws WxErrorException 微信 API 调用失败
     */
    public String code2Phone(String phoneCode) throws WxErrorException {
        WxMaPhoneNumberInfo phoneNumberInfo = wxMaService.getUserService().getPhoneNumber(phoneCode);
        return phoneNumberInfo.getPurePhoneNumber();
    }
}
