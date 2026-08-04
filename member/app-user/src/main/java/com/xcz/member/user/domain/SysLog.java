package com.xcz.member.user.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

/**
 * 系统日志基类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("sys_log")
public class SysLog implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /***日志ID**/
    @TableId(type = IdType.AUTO)
    private Long logId;
    /***日志类型（1登录日志 2操作日志）**/
    private Integer logType;
    /***日志状态（0正常 1异常）**/
    private Integer status;
    /***用户ID**/
    private Long userId;
    /***用户账号**/
    private String username;
    /***操作模块**/
    private String module;
    /***操作类型**/
    private String operation;
    /***请求方法**/
    private String method;
    /***请求方式（GET POST PUT DELETE）**/
    private String requestMethod;
    /***请求URL**/
    private String url;
    /***请求参数**/
    private String params;
    /***返回结果**/
    private String result;
    /***错误信息**/
    private String errorMsg;
    /***执行时长（毫秒）**/
    private Long executeTime;
    /***操作IP地址**/
    private String ip;
    /***操作地点**/
    private String location;
    /***创建时间**/
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private Date createTime;
}
