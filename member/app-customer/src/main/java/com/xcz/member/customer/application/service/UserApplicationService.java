package com.xcz.member.customer.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xcz.commons.core.exception.ServiceException;
import com.xcz.commons.core.utils.ServletUtils;
import com.xcz.commons.core.utils.StringUtils;
import com.xcz.commons.core.utils.ip.Ipv6Utils;
import com.xcz.commons.oss.service.UploadService;
import com.xcz.commons.security.extend.LoginUser;
import com.xcz.commons.security.utils.SecurityUtils;
import com.xcz.member.customer.api.dto.request.user.PasswordSetReq;
import com.xcz.member.customer.api.dto.request.user.PasswordVerifyReq;
import com.xcz.member.customer.api.dto.request.user.WxLoginReq;
import com.xcz.member.customer.domain.service.MobiShopUserService;
import com.xcz.member.customer.domain.service.MobiUserService;
import com.xcz.member.customer.infrastructure.adapter.auth.AuthTokenIssuer;
import com.xcz.member.customer.infrastructure.adapter.wx.WxAuthAdapter;
import com.xcz.member.customer.infrastructure.adapter.wx.vo.WxSession;
import com.xcz.member.customer.infrastructure.cache.UserCache;
import com.xcz.member.customer.infrastructure.cache.dto.UserInfoCacheDTO;
import com.xcz.member.customer.infrastructure.entity.MobiUser;
import com.xcz.member.customer.utils.CertFileUtils;
import com.xcz.member.feign.dto.user.MobiUserFeign;
import jakarta.annotation.Resource;
import me.chanjar.weixin.common.error.WxErrorException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户应用服务：微信登录、资料与支付密码管理。
 */
@Service
public class UserApplicationService {

    @Resource
    private MobiUserService mobiUserService;
    @Resource
    private WxAuthAdapter wxAuthAdapter;
    @Resource
    private AuthTokenIssuer authTokenIssuer;
    @Resource
    private UploadService uploadService;
    @Resource
    private PasswordEncoder passwordEncoder;
    @Resource
    private MobiShopUserService mobiShopUserService;

    /**
     * 微信小程序登录：换取 openId、可选绑定手机号，并签发访问 token。
     *
     * @param wxLogin 微信 code 与可选手机号授权 code
     * @return 登录 token
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> doLogin(WxLoginReq wxLogin){
        try {
            WxSession wxSession = wxAuthAdapter.code2Session(wxLogin.getCode());
            MobiUser user = mobiUserService.getOne(
                    new LambdaQueryWrapper<MobiUser>()
                            .eq(MobiUser::getOpenid, wxSession.openId())
            );
            boolean exists = user != null;
            if(!exists){
                int length = wxSession.openId().length();
                user = MobiUser.builder()
                        .openid(wxSession.openId())
                        .nickname("用户" + wxSession.openId().substring(length - 4,length))
                        .status(1)
                        .build();
            }
            if(user.getPhone() == null && wxLogin.getPhoneCode() != null){
                String phoneNum = wxAuthAdapter.code2Phone(wxLogin.getPhoneCode());
                user.setPhone(phoneNum);
            }
            user.setLoginIp(Ipv6Utils.getClientIp(ServletUtils.getRequest()));
            user.setLastLoginTime(LocalDateTime.now());
            mobiUserService.saveOrUpdate(user);
            if(!exists){
                // 新用户默认加入平台默认店铺，角色缓存由 enterShopAsCustomer 内部清除
                mobiShopUserService.enterShopAsCustomer(1L,user.getUserId());
            }
            LoginUser loginUser = authTokenIssuer.issue(user);
            Map<String, Object> result = new HashMap<>();
            result.put("token", loginUser.getToken());
            result.put("register", !exists);
            return result;
        } catch (WxErrorException e) {
            throw new ServiceException("微信登录失败：" + e.getMessage());
        }
    }

    /**
     * 更新当前用户昵称与头像。
     *
     * @param nickName    新昵称（可选）
     * @param avatarFile  新头像文件（可选）
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateUserInfo(String nickName, MultipartFile avatarFile){
        try {
            if(nickName == null && avatarFile == null){
                throw new ServiceException("请填写表单");
            }
            Long userId = SecurityUtils.getUserId();
            MobiUser userInfo = mobiUserService.getById(userId);
            if(userInfo == null){
                throw new ServiceException("用户不存在");
            }
            if(avatarFile != null){
                String objectKey = CertFileUtils.avatarFolder(avatarFile.getOriginalFilename());
                uploadService.simpleUpload(avatarFile, objectKey);
                uploadService.delete(userInfo.getAvatarUrl());
                userInfo.setAvatarUrl(uploadService.getEnteralUrl(objectKey));
            }
            if(nickName != null){
                userInfo.setNickname(nickName);
            }
            mobiUserService.updateById(userInfo);
            LoginUser loginUser = SecurityUtils.getLoginUser();
            loginUser.setName(userInfo.getNickname());
            loginUser.setAvatar(userInfo.getAvatarUrl());
            authTokenIssuer.refreshLoginInfo(loginUser);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 设置或修改当前用户支付密码（6 位数字）。
     *
     * @param password 旧密码（已设置时必填）与新密码
     */
    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(PasswordSetReq password){

        Long userId = SecurityUtils.getUserId();
        MobiUser userInfo = mobiUserService.getById(userId);
        if(userInfo == null){
            throw new ServiceException("用户不存在");
        }
        //进行密码判断
        if(StringUtils.isNotEmpty(userInfo.getPassword())){
            if(StringUtils.isEmpty(password.getOldPassword())){
                throw new ServiceException("请输入旧密码");
            }
            if(!passwordEncoder.matches(password.getOldPassword(), userInfo.getPassword())){
                throw new ServiceException("旧密码错误");
            }
            if(passwordEncoder.matches(password.getNewPassword(), userInfo.getPassword())){
                throw new ServiceException("新密码不能与旧密码相同");
            }
        }
        String newPassword = passwordEncoder.encode(password.getNewPassword());
        MobiUser build = MobiUser.builder()
                .userId(userId)
                .password(newPassword)
                .updateTime(LocalDateTime.now())
                .build();
        // 更新密码
        mobiUserService.updateById( build);
        LoginUser loginUser = SecurityUtils.getLoginUser();
        loginUser.setPassword(newPassword);
        authTokenIssuer.refreshLoginInfo(loginUser);
    }

    /**
     * 校验当前用户支付密码（付款码展示等场景使用）。
     *
     * @param password 6 位数字支付密码
     */
    public void verifyPassword(PasswordVerifyReq password) {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        if (StringUtils.isEmpty(loginUser.getPassword())) {
            throw new ServiceException("请先设置支付密码");
        }
        if (!passwordEncoder.matches(password.getPassword(), loginUser.getPassword())) {
            throw new ServiceException("支付密码错误");
        }
    }

    /**
     * 获取当前登录用户信息
     *
     * @return 用户信息；token 临近过期时自动续签
     */
    public Map<String, Object> getUserInfo() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        UserCache.saveUserInfo(new UserInfoCacheDTO(loginUser.getUserId(), loginUser.getName(), loginUser.getAvatar()));

        // 构建返回信息
        Map<String, Object> info = new HashMap<>();
        info.put("token", loginUser.getToken());  // 始终返回有效token
        info.put("permission", loginUser.getPermissions());
        info.put("nickName", loginUser.getName());
        info.put("avatar", uploadService.getUrl(loginUser.getAvatar()));
        info.put("setPassword", loginUser.getPassword() != null);
        return info;
    }

    /**
     * 管理后台 Feign：更新小程序用户。
     */
    public void updateForSys(MobiUserFeign body) {
        mobiUserService.updateForSys(body);
    }

}
