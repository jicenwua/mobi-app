package com.xcz.member.customer.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 店铺信息表 Mobi_shop
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@TableName("Mobi_shop")
public class MobiShop implements Serializable {
    private static final long serialVersionUID = 1L;

    /***店铺ID**/
    @TableId(type = IdType.AUTO)
    private Long id;
    /***父店铺ID**/
    private Long parentId;
    /***店铺名称**/
    private String shopName;
    /***店铺唯一标识**/
    private String shopCode;
    /***积分兑换比率（1元*比率 = 充值一元的积分）不可更改**/
    private Integer ratio;
    /***店铺图片**/
    private String picture;
    /***联系电话**/
    private String phone;
    /***行业类目ID**/
    private Integer categoryId;
    /***省**/
    private String province;
    /***市**/
    private String city;
    /***区县**/
    private String district;
    /***详细地址**/
    private String address;
    /***法人姓名**/
    private String legalPerson;
    /***法人身份证号**/
    private String idCardNo;
    /***统一社会信用代码**/
    private String businessLicenseNo;
    /***身份证正面OSS**/
    private String idCardFront;
    /***身份证背面OSS**/
    private String idCardBack;
    /***营业执照图片**/
    private String businessLicensePic;
    /***店铺外景**/
    private String shopExterior;
    /***店铺内景**/
    private String shopInterior;
    /***审核状态 0待审 1通过 2驳回**/
    private Integer auditStatus;
    /***驳回原因**/
    private String auditReason;
    /***审核时间**/
    private LocalDateTime auditTime;
    /***审核人ID**/
    private Long auditId;
    /***0禁用 1启用**/
    private Integer isEnabled;
    /***创建时间**/
    private LocalDateTime createTime;
    /***更新时间**/
    private LocalDateTime updateTime;
    /***0存在 1删除**/
    @TableLogic
    private Integer delFlag;
    /***最小创建时间（查询条件）**/
    @TableField(exist = false)
    private String minCreateTime;
    /***最大创建时间（查询条件）**/
    @TableField(exist = false)
    private String maxCreateTime;

    /**
     * 将逗号分隔的轮播图 OSS key 转为列表
     *
     * @return 轮播图 key 列表
     */
    public List<String> getPictureList() {
        return Arrays.stream(this.picture.split(",")).toList();
    }
}
