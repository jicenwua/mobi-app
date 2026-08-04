package com.xcz.member.customer.application.assemblers;

import com.xcz.member.customer.api.dto.response.point.PointsPurchaseRes;
import com.xcz.member.customer.domain.vo.PointsPurchaseResultVO;

/**
 * 购买结算对象转换器。
 */
public final class PurchaseAssembler {

    private PurchaseAssembler() {
    }

    /**
     * 转换为购买结果响应。
     *
     * @param result 购买执行结果
     * @return API 响应对象
     */
    public static PointsPurchaseRes toPurchaseRes(PointsPurchaseResultVO result) {
        if (result == null) {
            return null;
        }
        return PointsPurchaseRes.builder()
                .logId(result.getLogId())
                .originalPoints(result.getOriginalPoints() == null ? null : result.getOriginalPoints().intValue())
                .discountPoints(result.getDiscountPoints())
                .consumePoints(result.getConsumePoints())
                .remainingPoints(result.getRemainingPoints())
                .build();
    }
}
