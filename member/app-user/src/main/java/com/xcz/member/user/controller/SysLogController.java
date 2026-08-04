package com.xcz.member.user.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xcz.commons.core.domain.ResponseEntity;
import com.xcz.commons.core.utils.StringUtils;
import com.xcz.member.user.annotation.OperateLog;
import com.xcz.member.user.domain.SysLog;
import com.xcz.member.user.domain.enums.OperationEnum;
import com.xcz.member.user.service.SysLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 系统日志控制器
 */
@RestController
@RequestMapping("/system/log")
@RequiredArgsConstructor
public class SysLogController {

    private final SysLogService logService;

    /**
     * 分页查询日志列表
     *
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @param logType  日志类型（1登录日志 2操作日志）
     * @param status   状态（0正常 1异常）
     * @param username 用户名
     * @param module   操作模块
     * @param address  操作 IP（模糊，对应库字段 ip）
     * @param operation 操作类型（精确，与库中 operation 一致）
     * @param minCreateTime 创建时间下限（与 createTime 比较，建议含时分秒）
     * @param maxCreateTime 创建时间上限（与 createTime 比较，建议含时分秒）
     */
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('system:log:list')")
    public ResponseEntity<List<SysLog>> list(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer logType,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) Integer operation,
            @RequestParam(required = false) String minCreateTime,
            @RequestParam(required = false) String maxCreateTime) {

        Page<SysLog> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<SysLog> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(logType != null, SysLog::getLogType, logType);
        wrapper.eq(status != null, SysLog::getStatus, status);
        wrapper.like(StringUtils.isNotEmpty( username),SysLog::getUsername,username);
        wrapper.like(StringUtils.isNotEmpty( module),SysLog::getModule,module);
        wrapper.like(StringUtils.isNotEmpty(address), SysLog::getIp, address);
        wrapper.eq(operation != null, SysLog::getOperation, OperationEnum.getByCode(operation).getMessage());
        wrapper.gt(StringUtils.isNotEmpty(minCreateTime), SysLog::getCreateTime, minCreateTime);
        wrapper.le(StringUtils.isNotEmpty(maxCreateTime), SysLog::getCreateTime, maxCreateTime);
        // 按创建时间倒序
        wrapper.orderByDesc(SysLog::getCreateTime);

        Page<SysLog> result = logService.page(page, wrapper);

        ResponseEntity<List<SysLog>> response = new ResponseEntity<>();
        response.setCode(200);
        response.setMsg("查询成功");
        response.setData(result.getRecords());
        response.setTotal(result.getTotal());
        return response;
    }


    /**
     * 查询日志详情
     */
    @GetMapping("/{logId}")
    @PreAuthorize("@ss.hasPermi('system:log:query')")
    public ResponseEntity<SysLog> getInfo(@PathVariable Long logId) {
        SysLog log = logService.getById(logId);

        ResponseEntity<SysLog> response = new ResponseEntity<>();
        response.setCode(200);
        response.setMsg("查询成功");
        response.setData(log);
        return response;
    }

    /**
     * 删除日志
     */
    @DeleteMapping("/{logIds}")
    @PreAuthorize("@ss.hasPermi('system:log:remove')")
    @OperateLog(module = "系统日志", operation = "删除日志")
    public ResponseEntity<Void> remove(@PathVariable Long[] logIds) {
        boolean result = logService.removeByIds(java.util.Arrays.asList(logIds));

        ResponseEntity<Void> response = new ResponseEntity<>();
        response.setCode(200);
        response.setMsg(result ? "删除成功" : "删除失败");
        return response;
    }

}
