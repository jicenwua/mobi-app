package com.xcz.member.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.xcz.commons.security.extend.LoginUser;
import com.xcz.member.user.domain.SysLog;

/**
 * 系统日志 Service
 */
public interface SysLogService extends IService<SysLog> {

    /**
     * 保存登录日志
     *
     * @param status   状态（0成功 1失败）
     * @param msg      消息
     * @param loginUser 登录用户
     */
    void saveLoginLog(Integer status, String msg, LoginUser loginUser);

    /**
     * 保存操作日志
     *
     * @param sysLog 日志对象
     */
    void saveOperateLog(SysLog sysLog);
}
