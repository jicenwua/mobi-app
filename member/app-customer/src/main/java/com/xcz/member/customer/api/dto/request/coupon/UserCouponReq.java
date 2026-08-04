package com.xcz.member.customer.api.dto.request.coupon;

import com.xcz.member.customer.api.dto.request.BaseReq;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户持券 HTTP 请求参数。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class UserCouponReq extends BaseReq {

    /***店铺 ID**/
    private Long shopId;
    /***券模板 ID（领取接口使用）**/
    private Long templateId;
    /***持券状态：0-未使用 1-已使用 2-已过期**/
    private Integer status;
}
