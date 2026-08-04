package com.xcz.member.user.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Tree基类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TreeVOParams implements Serializable {
    private static final long serialVersionUID = 1L;

    /***菜单ID**/
    private Long menuId;
    /***父菜单ID**/
    private Long parentId;
    /***显示顺序**/
    private Integer orderNum;
    /***菜单名称**/
    private String menuName;
    /***路由地址**/
    private String path;
    /***组件路径**/
    private String component;
    /***路由参数**/
    private String query;
    /***是否为外链（0是 1否）**/
    private Integer isFrame;
    /***是否缓存（0缓存 1不缓存）**/
    private Integer isCache;
    /***菜单类型（M目录 C菜单 F按钮）**/
    private String menuType;
    /***权限标识**/
    private String perms;
    /***菜单图标**/
    private String icon;
    /***显示状态（0显示 1隐藏）**/
    private String visible;
    /***菜单状态（0正常 1停用）**/
    private String status;
    /***创建者**/
    private String createBy;
    /***创建时间**/
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime createTime;
    /***更新者**/
    private String updateBy;
    /***更新时间**/
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    private LocalDateTime updateTime;
    /***子菜单**/
    private List<?> children = new ArrayList<>();
}
