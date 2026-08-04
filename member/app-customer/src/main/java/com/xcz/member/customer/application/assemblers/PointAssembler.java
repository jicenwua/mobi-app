package com.xcz.member.customer.application.assemblers;

import com.xcz.member.customer.api.dto.request.point.*;
import com.xcz.member.customer.api.dto.response.point.ConsumeLogDetailRes;
import com.xcz.member.customer.api.dto.response.point.ConsumeLogRes;
import com.xcz.member.customer.api.dto.response.point.ConsumeReceiptItemRes;
import com.xcz.member.customer.api.dto.response.point.PointsAccountListRes;
import com.xcz.member.customer.api.dto.response.point.PointsRechargeRes;
import com.xcz.member.customer.domain.dto.points.*;
import com.xcz.member.customer.domain.vo.PointsAccountVO;
import com.xcz.member.customer.domain.vo.PointsLogBriefVO;
import com.xcz.member.customer.domain.vo.PointsReceiptItemVO;
import com.xcz.member.customer.domain.vo.PointsRechargeResultVO;
import com.xcz.member.feign.dto.points.PointsAccountFeign;
import com.xcz.member.feign.dto.points.PointsLogBriefFeign;
import com.xcz.member.feign.dto.points.PointsReceiptItemFeign;
import com.xcz.member.feign.dto.points.PointsShopFeignQuery;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.List;

/**
 * 积分相关对象组装器：Request / 读模型 / Feign DTO ↔ Response 之间的转换。
 */
public final class PointAssembler {

    private PointAssembler() {
    }

    /**
     * 将 HTTP 扣款请求转换为领域 DTO。
     */
    public static PointsConsumeDTO toConsumeDto(PointsConsumeReq req) {
        if (req == null) {
            return null;
        }
        return PointsConsumeDTO.builder()
                .shopId(req.getShopId())
                .token(req.getToken())
                .couponId(req.getCouponId())
                .items(toConsumeItemDtos(req.getItems()))
                .requestId(req.getRequestId())
                .build();
    }

    /**
     * 将 HTTP 充值请求转换为领域 DTO。
     */
    public static PointsRechargeDTO toRechargeDto(PointsRechargeReq req) {
        if (req == null) {
            return null;
        }
        return PointsRechargeDTO.builder()
                .shopId(req.getShopId())
                .userId(req.getUserId())
                .amountYuan(req.getAmountYuan())
                .requestId(req.getRequestId())
                .build();
    }

    /**
     * 将 HTTP 购买请求转换为领域 DTO。
     */
    public static PointsPurchaseDTO toPurchaseDto(PointsPurchaseReq req) {
        if (req == null) {
            return null;
        }
        return PointsPurchaseDTO.builder()
                .shopId(req.getShopId())
                .userCouponId(req.getUserCouponId())
                .items(toConsumeItemDtos(req.getItems()))
                .requestId(req.getRequestId())
                .build();
    }

    /**
     * 将充值执行结果转换为 API 响应。
     */
    public static PointsRechargeRes toRechargeRes(PointsRechargeResultVO result) {
        if (result == null) {
            return null;
        }
        return PointsRechargeRes.builder()
                .logId(result.getLogId())
                .basePoints(result.getBasePoints())
                .giftPoints(result.getGiftPoints())
                .totalPoints(result.getTotalPoints())
                .remainingPoints(result.getRemainingPoints())
                .build();
    }

    private static List<PointsConsumeItemDTO> toConsumeItemDtos(List<PointsConsumeItemReq> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        return items.stream()
                .map(item -> PointsConsumeItemDTO.builder()
                        .productId(item.getProductId())
                        .count(item.getCount())
                        .build())
                .toList();
    }

    /**
     * 将小程序积分查询请求转换为领域查询条件。
     *
     * @param dto 积分查询参数
     * @return 领域查询条件；req 为 null 时返回空条件
     */
    public static PointsShopQuery toShopQuery(PointReq dto) {
        if (dto == null) {
            return PointsShopQuery.builder().build();
        }
        PointsShopQuery.PointsShopQueryBuilder builder = PointsShopQuery.builder()
                .shopId(dto.getShopId())
                .actionType(dto.getActionType())
                .status(dto.getStatus())
                .excludeStatus(dto.getExcludeStatus())
                .sortType(dto.getSortType())
                .pageNum(dto.getPageNum())
                .pageSize(dto.getPageSize());
        applyMemberKeyword(builder, dto.getKeyword(), dto.getNickname(), dto.getPhone());
        return builder.build();
    }

    private static void applyMemberKeyword(
            PointsShopQuery.PointsShopQueryBuilder builder,
            String keyword,
            String nickname,
            String phone) {
        if (keyword != null && !keyword.isBlank()) {
            String trimmed = keyword.trim();
            builder.keyword(trimmed);
            builder.keywordSearchPhone(trimmed.matches("\\d+"));
            return;
        }
        builder.nickname(nickname);
        builder.phone(phone);
    }

    /**
     * 将 Feign 查询参数转换为领域查询条件。
     *
     * @param query Feign 查询参数
     * @return 领域查询条件；query 为 null 时返回空条件
     */
    public static PointsShopQuery toShopQuery(PointsShopFeignQuery query) {
        if (query == null) {
            return PointsShopQuery.builder().build();
        }
        return PointsShopQuery.builder()
                .shopId(query.getShopId())
                .userId(query.getUserId())
                .shopName(query.getShopName())
                .nickname(query.getNickname())
                .phone(query.getPhone())
                .actionType(query.getActionType())
                .preBasePoints(query.getPreBasePoints())
                .preBonusPoints(query.getPreBonusPoints())
                .changeBase(query.getChangeBase())
                .changeBonus(query.getChangeBonus())
                .afterBasePoints(query.getAfterBasePoints())
                .afterBonusPoints(query.getAfterBonusPoints())
                .status(query.getStatus())
                .sortType(query.getSortType())
                .pageNum(query.getPageNum())
                .pageSize(query.getPageSize())
                .build();
    }

    /**
     * 将积分账户读模型转换为小程序列表响应。
     *
     * @param source 积分账户读模型
     * @return 列表响应；读模型为 null 时返回 null
     */
    public static PointsAccountListRes toAccountVo(PointsAccountVO source) {
        if (source == null) {
            return null;
        }
        return PointsAccountListRes.builder()
                .shopId(source.getShopId())
                .userId(source.getUserId())
                .shopName(source.getShopName())
                .nickname(source.getNickname())
                .phone(source.getPhone())
                .basePoints(source.getBasePoints())
                .bonusPoints(source.getBonusPoints())
                .remainingPoints(source.getRemainingPoints())
                .totalUsedPoints(source.getTotalUsedPoints())
                .updateTime(source.getUpdateTime())
                .build();
    }

    /**
     * 将积分账户读模型转换为 Feign 传输对象。
     *
     * @param source 积分账户读模型
     * @return Feign 传输对象；读模型为 null 时返回 null
     */
    public static PointsAccountFeign toAccountFeign(PointsAccountVO source) {
        if (source == null) {
            return null;
        }
        return PointsAccountFeign.builder()
                .shopId(source.getShopId())
                .userId(source.getUserId())
                .shopName(source.getShopName())
                .nickname(source.getNickname())
                .phone(source.getPhone())
                .basePoints(source.getBasePoints())
                .bonusPoints(source.getBonusPoints())
                .remainingPoints(source.getRemainingPoints())
                .totalUsedPoints(source.getTotalUsedPoints())
                .updateTime(source.getUpdateTime())
                .build();
    }

    /**
     * 将积分流水读模型转换为消费记录列表响应（汇总积分，不拆分基础/赠送）。
     *
     * @param source 积分流水读模型
     * @return 消费记录列表响应；读模型为 null 时返回 null
     */
    public static ConsumeLogRes toConsumeLogRes(PointsLogBriefVO source) {
        if (source == null) {
            return null;
        }
        return ConsumeLogRes.builder()
                .logId(source.getLogId())
                .shopId(source.getShopId())
                .shopName(source.getShopName())
                .userId(source.getUserId())
                .nickname(source.getNickname())
                .phone(source.getPhone())
                .actionType(source.getActionType())
                .status(source.getStatus())
                .remark(source.getRemark())
                .createTime(source.getCreateTime())
                .consumeTime(source.getConsumeTime())
                .consumePoints(source.getConsumePoints())
                .remainingPoints(sum(source.getAfterBasePoints(), source.getAfterBonusPoints()))
                .couponId(source.getCouponId())
                .items(toBriefReceiptItems(source.getItems()))
                .build();
    }

    /**
     * 将积分流水读模型转换为消费记录详情响应（含商品明细与优惠券抵扣）。
     *
     * @param source 积分流水读模型
     * @return 消费记录详情响应；读模型为 null 时返回 null
     */
    public static ConsumeLogDetailRes toConsumeLogDetailRes(PointsLogBriefVO source) {
        if (source == null) {
            return null;
        }
        List<ConsumeReceiptItemRes> items = toReceiptItemVos(source.getItems());
        int originalPoints = items.stream()
                .mapToInt(item -> item.getLinePoints() == null ? 0 : item.getLinePoints())
                .sum();
        int consumePoints = source.getConsumePoints() == null ? 0 : source.getConsumePoints();
        Integer couponDiscountPoints = null;
        if (source.getCouponId() != null && originalPoints > consumePoints) {
            couponDiscountPoints = originalPoints - consumePoints;
        }
        return ConsumeLogDetailRes.builder()
                .logId(source.getLogId())
                .shopId(source.getShopId())
                .shopName(source.getShopName())
                .userId(source.getUserId())
                .nickname(source.getNickname())
                .phone(source.getPhone())
                .actionType(source.getActionType())
                .status(source.getStatus())
                .remark(source.getRemark())
                .createTime(source.getCreateTime())
                .consumeTime(source.getConsumeTime())
                .consumePoints(source.getConsumePoints())
                .remainingPoints(sum(source.getAfterBasePoints(), source.getAfterBonusPoints()))
                .couponId(source.getCouponId())
                .originalPoints(originalPoints)
                .couponDiscountPoints(couponDiscountPoints)
                .couponName(source.getCouponName())
                .couponType(source.getCouponType())
                .items(items)
                .build();
    }

    /**
     * 将积分流水读模型转换为 Feign 传输对象。
     *
     * @param source 积分流水读模型
     * @return Feign 传输对象；读模型为 null 时返回 null
     */
    public static PointsLogBriefFeign toLogFeign(PointsLogBriefVO source) {
        if (source == null) {
            return null;
        }
        return PointsLogBriefFeign.builder()
                .logId(source.getLogId())
                .shopId(source.getShopId())
                .shopName(source.getShopName())
                .userId(source.getUserId())
                .nickname(source.getNickname())
                .phone(source.getPhone())
                .actionType(source.getActionType())
                .preBasePoints(source.getPreBasePoints())
                .preBonusPoints(source.getPreBonusPoints())
                .changeBase(source.getChangeBase())
                .changeBonus(source.getChangeBonus())
                .afterBasePoints(source.getAfterBasePoints())
                .afterBonusPoints(source.getAfterBonusPoints())
                .status(source.getStatus())
                .remark(source.getRemark())
                .createTime(source.getCreateTime())
                .consumeTime(source.getConsumeTime())
                .consumePoints(source.getConsumePoints())
                .couponId(source.getCouponId())
                .items(toReceiptItemFeigns(source.getItems()))
                .build();
    }

    /**
     * 将积分流水读模型转换为 Feign 详情传输对象（含优惠券与商品原价）。
     */
    public static PointsLogBriefFeign toLogDetailFeign(PointsLogBriefVO source) {
        if (source == null) {
            return null;
        }
        List<PointsReceiptItemFeign> itemFeigns = toReceiptItemFeigns(source.getItems());
        int originalPoints = itemFeigns == null ? 0 : itemFeigns.stream()
                .mapToInt(item -> {
                    if (item.getPrice() == null || item.getCount() == null) {
                        return 0;
                    }
                    return item.getPrice().multiply(BigDecimal.valueOf(item.getCount())).intValue();
                })
                .sum();
        int consumePoints = source.getConsumePoints() == null ? 0 : source.getConsumePoints();
        Integer couponDiscountPoints = null;
        if (source.getCouponId() != null && originalPoints > consumePoints) {
            couponDiscountPoints = originalPoints - consumePoints;
        }
        return PointsLogBriefFeign.builder()
                .logId(source.getLogId())
                .shopId(source.getShopId())
                .shopName(source.getShopName())
                .userId(source.getUserId())
                .nickname(source.getNickname())
                .phone(source.getPhone())
                .actionType(source.getActionType())
                .preBasePoints(source.getPreBasePoints())
                .preBonusPoints(source.getPreBonusPoints())
                .changeBase(source.getChangeBase())
                .changeBonus(source.getChangeBonus())
                .afterBasePoints(source.getAfterBasePoints())
                .afterBonusPoints(source.getAfterBonusPoints())
                .status(source.getStatus())
                .remark(source.getRemark())
                .createTime(source.getCreateTime())
                .consumeTime(source.getConsumeTime())
                .consumePoints(source.getConsumePoints())
                .couponId(source.getCouponId())
                .couponName(source.getCouponName())
                .couponType(source.getCouponType())
                .originalPoints(originalPoints > 0 ? originalPoints : null)
                .couponDiscountPoints(couponDiscountPoints)
                .items(itemFeigns)
                .build();
    }

    /**
     * 列表展示用：仅含商品名称与数量。
     */
    private static List<ConsumeReceiptItemRes> toBriefReceiptItems(List<PointsReceiptItemVO> items) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        return items.stream()
                .map(item -> ConsumeReceiptItemRes.builder()
                        .receiptId(item.getReceiptId())
                        .productName(item.getProductName())
                        .count(item.getCount())
                        .build())
                .toList();
    }

    /**
     * 将小票商品行读模型转换为详情响应列表。
     */
    private static List<ConsumeReceiptItemRes> toReceiptItemVos(List<PointsReceiptItemVO> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        return items.stream()
                .map(item -> ConsumeReceiptItemRes.builder()
                        .receiptId(item.getReceiptId())
                        .productName(item.getProductName())
                        .count(item.getCount())
                        .price(item.getPrice())
                        .linePoints(calcLinePoints(item.getPrice(), item.getCount()))
                        .build())
                .toList();
    }

    /**
     * 将小票商品行读模型转换为 Feign 传输对象列表。
     */
    private static List<PointsReceiptItemFeign> toReceiptItemFeigns(List<PointsReceiptItemVO> items) {
        if (items == null || items.isEmpty()) {
            return Collections.emptyList();
        }
        return items.stream()
                .map(item -> PointsReceiptItemFeign.builder()
                        .receiptId(item.getReceiptId())
                        .logId(item.getLogId())
                        .productName(item.getProductName())
                        .count(item.getCount())
                        .price(item.getPrice())
                        .build())
                .toList();
    }

    /**
     * 计算单行积分（单价 × 数量，四舍五入取整）。
     */
    private static int calcLinePoints(BigDecimal price, Integer count) {
        if (price == null || count == null || count <= 0) {
            return 0;
        }
        return price.multiply(BigDecimal.valueOf(count))
                .setScale(0, RoundingMode.HALF_UP)
                .intValue();
    }

    /**
     * 安全求和两个可空整数。
     */
    private static int sum(Integer left, Integer right) {
        return (left == null ? 0 : left) + (right == null ? 0 : right);
    }
}
