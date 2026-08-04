package com.xcz.member.customer.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 店铺用户权限表 mobi_shop_user
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@TableName("mobi_shop_user")
public class MobiShopUser implements Serializable {
    private static final long serialVersionUID = 1L;

    /***主键ID**/
    @TableId(type = IdType.AUTO)
    private Long id;
    /***店铺ID**/
    private Long shopId;
    /***用户ID**/
    private Long userId;
    /***用户角色: 1-店长, 2-店员, 3-顾客（历史编码 4 读侧兼容）**/
    private Integer role;
    /***最后进入时间**/
    private LocalDateTime lastEnterTime;
    /***创建时间**/
    private LocalDateTime createTime;
}
