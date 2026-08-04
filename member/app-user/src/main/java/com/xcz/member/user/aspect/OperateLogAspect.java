package com.xcz.member.user.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xcz.commons.core.utils.ip.Ipv6Utils;
import com.xcz.commons.security.extend.LoginUser;
import com.xcz.commons.security.utils.SecurityUtils;
import com.xcz.member.user.annotation.OperateLog;
import com.xcz.member.user.domain.SysLog;
import com.xcz.member.user.service.SysLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.Date;

/**
 * 操作日志切面
 * 自动记录标注了 @OperateLog 注解的方法执行情况
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperateLogAspect {

    private final SysLogService logService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Around("@annotation(operateLog)")
    public Object around(ProceedingJoinPoint joinPoint, OperateLog operateLog) throws Throwable {
        long startTime = System.currentTimeMillis();

        // 创建日志对象
        SysLog sysLog = new SysLog();
        sysLog.setLogType(2);
        sysLog.setModule(operateLog.module());
        sysLog.setOperation(operateLog.operation());
        sysLog.setCreateTime(new Date());

        try {
            // 获取请求信息
            HttpServletRequest request = getRequest();
            if (request != null) {
                sysLog.setUrl(request.getRequestURI());
                sysLog.setRequestMethod(request.getMethod());
                sysLog.setIp(Ipv6Utils.getClientIp(request));
            }

            // 获取当前用户
            try {
                LoginUser loginUser = SecurityUtils.getLoginUser();
                if (loginUser != null) {
                    sysLog.setUserId(loginUser.getUserId());
                    sysLog.setUsername(loginUser.getUsername());
                }
            } catch (Exception e) {
                log.warn("获取当前用户信息失败", e);
            }

            // 获取方法信息
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            sysLog.setMethod(method.getDeclaringClass().getName() + "." + method.getName());

            // 保存请求参数
            if (operateLog.saveParams()) {
                try {
                    String params = objectToJson(joinPoint.getArgs());
                    sysLog.setParams(params);
                } catch (Exception e) {
                    log.warn("序列化请求参数失败", e);
                }
            }

            // 执行目标方法
            Object result = joinPoint.proceed();

            // 计算执行时间
            long executeTime = System.currentTimeMillis() - startTime;
            sysLog.setExecuteTime(executeTime);
            sysLog.setStatus(0); // 成功

            // 保存返回结果
            if (operateLog.saveResult() && result != null) {
                try {
                    String resultJson = objectToJson(result);
                    // 限制结果长度，避免过大
                    if (resultJson.length() > 2000) {
                        resultJson = resultJson.substring(0, 2000) + "...";
                    }
                    sysLog.setResult(resultJson);
                } catch (Exception e) {
                    log.warn("序列化返回结果失败", e);
                }
            }

            // 异步保存日志
            logService.saveOperateLog(sysLog);

            return result;

        } catch (Throwable throwable) {
            // 记录异常信息
            long executeTime = System.currentTimeMillis() - startTime;
            sysLog.setExecuteTime(executeTime);
            sysLog.setStatus(1); // 异常
            sysLog.setErrorMsg(throwable.getMessage());

            // 异步保存日志
            logService.saveOperateLog(sysLog);

            throw throwable;
        }
    }

    /**
     * 获取 HttpServletRequest
     */
    private HttpServletRequest getRequest() {
        try {
            ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 对象转 JSON
     */
    private String objectToJson(Object obj) {
        try {
            if (obj == null) {
                return null;
            }
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return obj.toString();
        }
    }
}
