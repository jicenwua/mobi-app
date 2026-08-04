package com.xcz.member.user.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xcz.commons.core.exception.ServiceException;
import com.xcz.commons.core.utils.ServletUtils;
import com.xcz.commons.core.utils.ip.IpUtils;
import com.xcz.commons.core.utils.ip.Ipv6Utils;
import com.xcz.commons.security.extend.LoginUser;
import com.xcz.member.user.domain.SysLog;
import com.xcz.member.user.domain.enums.LogTypeEnum;
import com.xcz.member.user.domain.enums.OperationEnum;
import com.xcz.member.user.mapper.SysLogMapper;
import com.xcz.member.user.service.SysLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Date;

/**
 * 系统日志 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysLogServiceImpl extends ServiceImpl<SysLogMapper, SysLog> implements SysLogService {

    /**
     * 保存登录日志（异步执行，不影响主流程）
     */
    @Override
    public void saveLoginLog(Integer status, String msg, LoginUser loginUser) {
        try {
            SysLog sysLog = new SysLog();
            sysLog.setLogType(LogTypeEnum.LOGIN.getCode());
            sysLog.setStatus(status);
            sysLog.setUsername(loginUser.getUsername());
            sysLog.setModule("用户登录");
            sysLog.setOperation(OperationEnum.LOGIN.getMessage());
            sysLog.setIp(loginUser.getIpaddr() != null ? loginUser.getIpaddr() :Ipv6Utils.getClientIp(ServletUtils.getRequest()));
            sysLog.setLocation(loginUser.getLoginLocation() != null ? loginUser.getLoginLocation() :IpUtils.getIpLocation(sysLog.getIp()));
            sysLog.setErrorMsg(status == 1 ? msg : null);
            sysLog.setCreateTime(new Date());

            this.save(sysLog);
            log.info("保存登录日志成功：username={}, status={}", loginUser.getUsername(), status);
        } catch (Exception e) {
            throw new ServiceException("保存登录日志失败：" + e.getMessage());
        }
    }

    /**
     * 保存操作日志（异步执行，不影响主流程）
     */
    @Override
    public void saveOperateLog(SysLog sysLog) {
        try {
            if (sysLog.getCreateTime() == null) {
                sysLog.setCreateTime(new Date());
            }

            this.save(sysLog);
            log.debug("保存操作日志成功：userId={}, module={}, operation={}",
                sysLog.getUserId(), sysLog.getModule(), sysLog.getOperation());
        } catch (Exception e) {
            log.error("保存操作日志失败", e);
        }
    }
}
