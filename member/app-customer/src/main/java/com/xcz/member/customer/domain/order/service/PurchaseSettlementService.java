package com.xcz.member.customer.domain.order.service;

import com.xcz.commons.core.exception.ServiceException;
import com.xcz.member.customer.domain.dto.points.PointsConsumeItemDTO;
import com.xcz.member.customer.domain.enums.CouponType;
import com.xcz.member.customer.domain.enums.ProductStatus;
import com.xcz.member.customer.domain.order.model.PurchaseLineItem;
import com.xcz.member.customer.domain.order.model.PurchaseSettlement;
import com.xcz.member.customer.domain.promotion.CouponSettlementCalculator;
import com.xcz.member.customer.infrastructure.entity.MobiCouponTemplate;
import com.xcz.member.customer.infrastructure.entity.MobiShopProduct;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 购买结算领域服务：商品计价与优惠券抵扣。
 */
public final class PurchaseSettlementService {

    private PurchaseSettlementService() {
    }

    /**
     * 按购物清单计算结算结果（不含优惠券）。
     *
     * @param items      购买明细
     * @param productMap 已过滤的商品快照，key 为 productId
     * @return 结算结果
     */
    public static PurchaseSettlement settleWithoutCoupon(List<PointsConsumeItemDTO> items,
                                                         Map<Long, MobiShopProduct> productMap) {
        return settle(items, productMap, null, null);
    }

    /**
     * 根据购物清单与可选优惠券计算结算结果。
     *
     * @param items          购买明细，productId 与 count 均须有效
     * @param productMap     已按 shopId 过滤的商品快照
     * @param userCouponId   用户持券 ID，不使用券时传 null
     * @param couponTemplate 券模板，不使用券时传 null
     * @return 含行项目、原价、抵扣、应付积分的结算结果
     * @throws ServiceException 商品无效、下架、积分无效、券门槛或类型不合法
     */
    public static PurchaseSettlement settle(List<PointsConsumeItemDTO> items,
                                            Map<Long, MobiShopProduct> productMap,
                                            Long userCouponId,
                                            MobiCouponTemplate couponTemplate) {
        List<PurchaseLineItem> lines = buildLineItems(items, productMap);
        BigDecimal originalPoints = lines.stream()
                .map(PurchaseLineItem::linePoints)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (originalPoints.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("合计积分无效");
        }

        BigDecimal payablePoints = originalPoints;
        BigDecimal discountPoints = BigDecimal.ZERO;
        String couponName = null;
        Integer couponType = null;
        if (userCouponId != null && couponTemplate != null) {
            CouponType type = CouponType.getByCode(couponTemplate.getType());
            if (type == null) {
                throw new ServiceException("优惠券类型无效");
            }
            if (!CouponSettlementCalculator.meetsThreshold(couponTemplate.getThresholdAmount(), originalPoints)) {
                throw new ServiceException("未达到优惠券使用门槛");
            }
            payablePoints = CouponSettlementCalculator.calculatePayablePoints(
                    type, couponTemplate.getDiscountValue(), originalPoints);
            discountPoints = CouponSettlementCalculator.calculateDiscountPoints(originalPoints, payablePoints);
            couponName = couponTemplate.getCouponName();
            couponType = couponTemplate.getType();
        }

        return PurchaseSettlement.builder()
                .items(lines)
                .originalPoints(originalPoints)
                .discountPoints(discountPoints)
                .payablePoints(payablePoints)
                .userCouponId(userCouponId)
                .couponName(couponName)
                .couponType(couponType)
                .build();
    }

    /**
     * 从批量查询结果中构建结算用商品 Map（仅保留属于当前店铺的有效商品）。
     *
     * @param shopId   店铺 ID
     * @param items    购买明细
     * @param products 批量查库结果
     * @return key 为 productId 的商品 Map
     */
    public static Map<Long, MobiShopProduct> buildProductMap(Long shopId,
                                                             List<PointsConsumeItemDTO> items,
                                                             Collection<MobiShopProduct> products) {
        if (items == null || items.isEmpty()) {
            return Map.of();
        }
        Map<Long, MobiShopProduct> byId = products == null ? Map.of() : products.stream()
                .filter(Objects::nonNull)
                .filter(p -> p.getProductId() != null)
                .collect(Collectors.toMap(MobiShopProduct::getProductId, Function.identity(), (a, b) -> a));
        Map<Long, MobiShopProduct> map = new HashMap<>();
        for (PointsConsumeItemDTO item : items) {
            Long productId = item.getProductId();
            if (productId == null || map.containsKey(productId)) {
                continue;
            }
            MobiShopProduct product = byId.get(productId);
            if (product != null && shopId.equals(product.getShopId())) {
                map.put(productId, product);
            }
        }
        return map;
    }

    /**
     * 收集明细中的商品 ID（去重、忽略 null）。
     *
     * @param items 购买明细
     * @return 商品 ID 列表
     */
    public static List<Long> collectProductIds(List<PointsConsumeItemDTO> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        return items.stream()
                .map(PointsConsumeItemDTO::getProductId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    @FunctionalInterface
    public interface ProductLoader {
        MobiShopProduct load(Long productId);
    }

    private static List<PurchaseLineItem> buildLineItems(List<PointsConsumeItemDTO> items,
                                                         Map<Long, MobiShopProduct> productMap) {
        List<PurchaseLineItem> lines = new ArrayList<>();
        for (PointsConsumeItemDTO item : items) {
            if (item.getProductId() == null || item.getCount() == null || item.getCount() <= 0) {
                throw new ServiceException("商品数量无效");
            }
            MobiShopProduct product = productMap.get(item.getProductId());
            if (product == null) {
                throw new ServiceException("商品不存在或已下架");
            }
            if (product.getStatus() != null
                    && (product.getStatus() == ProductStatus.OFF_SHELF.getCode()
                    || product.getStatus() == ProductStatus.SOLD_OUT.getCode())) {
                throw new ServiceException("商品「" + product.getProductName() + "」暂不可兑换");
            }
            // 商品单价即为积分，乘以数量后四舍五入
            BigDecimal linePoints = product.getPrice()
                    .multiply(BigDecimal.valueOf(item.getCount()))
                    .setScale(0, RoundingMode.HALF_UP);
            if (linePoints.compareTo(BigDecimal.ZERO) <= 0) {
                throw new ServiceException("商品积分无效");
            }
            lines.add(PurchaseLineItem.builder()
                    .productId(item.getProductId())
                    .productName(product.getProductName())
                    .count(item.getCount())
                    .unitPrice(product.getPrice())
                    .linePoints(linePoints)
                    .build());
        }
        return lines;
    }
}
