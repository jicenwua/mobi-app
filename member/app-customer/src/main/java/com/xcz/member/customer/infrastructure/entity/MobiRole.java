package com.xcz.member.customer.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 小程序端权限角色表 mobi_role
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("mobi_role")
public class MobiRole {

    /***主键id**/
    @TableId(type = IdType.AUTO)
    private Long id;

    /***权限标识**/
    private String roleKey;

    /***权限描述**/
    private String roleDescription;

    /***权限状态：true-启用 false-禁用**/
    private Boolean status;

    /***创建时间**/
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    /***创建者**/
    private Long createBy;

    /***更新时间**/
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;

    /***更新人**/
    private Long updateBy;

    /***逻辑删除**/
    @TableLogic(value = "0", delval = "1")
    private Integer delFlag;

}

