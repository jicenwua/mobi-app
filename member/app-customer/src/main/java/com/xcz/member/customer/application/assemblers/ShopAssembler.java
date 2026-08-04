package com.xcz.member.customer.application.assemblers;

import com.xcz.member.customer.api.dto.request.shop.ShopCreatePayloadReq;
import com.xcz.member.customer.api.dto.request.shop.ShopReq;
import com.xcz.member.customer.api.dto.response.shop.ShopDetailRes;
import com.xcz.member.customer.api.dto.response.shop.ShopStatisticsRes;
import com.xcz.member.customer.application.command.shop.QueryShopCommand;
import com.xcz.member.customer.domain.dto.shop.ShopBriefDTO;
import com.xcz.member.customer.infrastructure.entity.MobiShop;
import com.xcz.member.feign.dto.shop.MobiShopFeign;
import com.xcz.member.feign.dto.shop.ShopCreateFeign;
import com.xcz.member.feign.dto.shop.ShopStatisticsFeign;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * 店铺相关对象组装器：负责 Request、读模型与 Response 之间的转换。
 */
public class ShopAssembler {

    /**
     * 将店铺列表查询请求转换为应用层查询命令。
     *
     * @param shopDTO 店铺列表查询参数
     * @return 查询命令
     */
    public static QueryShopCommand toQueryShopCommand(ShopReq shopDTO) {
        if (shopDTO == null) {
            return new QueryShopCommand(1, 10);
        }
        return new QueryShopCommand(shopDTO.getPageNum(), shopDTO.getPageSize());
    }

    /**
     * 将店铺简要读模型转换为详情响应。
     *
     * @param info 店铺简要读模型
     * @param role 用户在店铺中的角色编码
     * @return 店铺详情响应
     */
    public static ShopDetailRes toDetailVo(ShopBriefDTO info, Integer role) {
        if (info == null) {
            return null;
        }
        return ShopDetailRes.builder()
                .shopId(info.id())
                .parentId(info.parentId())
                .shopName(info.shopName())
                .shopCode(info.shopCode())
                .ratio(info.ratio())
                .picture(toPictureList(info.picture()))
                .phone(info.phone())
                .address(toFullAddress(info.province(), info.city(), info.district(), info.address()))
                .role(role)
                .build();
    }

    /**
     * 将逗号分隔的图片字段解析为列表。
     *
     * @param picture 图片字段
     * @return 图片 URL 列表
     */
    private static List<String> toPictureList(String picture) {
        if (picture == null || picture.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(picture.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /**
     * 拼接省市区与详细地址为完整地址字符串。
     *
     * @param province 省
     * @param city     市
     * @param district 区
     * @param address  详细地址
     * @return 完整地址
     */
    private static String toFullAddress(String province, String city, String district, String address) {
        return String.join("",
                province == null ? "" : province,
                city == null ? "" : city,
                district == null ? "" : district,
                address == null ? "" : address);
    }

    /**
     * Feign 开店元数据 → 小程序开店请求体。
     */
    public static ShopCreatePayloadReq toCreatePayload(ShopCreateFeign feign) {
        if (feign == null) {
            return null;
        }
        return ShopCreatePayloadReq.builder()
                .parentShopCode(feign.getParentShopCode())
                .shopName(feign.getShopName())
                .phone(feign.getPhone())
                .categoryId(feign.getCategoryId())
                .province(feign.getProvince())
                .city(feign.getCity())
                .district(feign.getDistrict())
                .address(feign.getAddress())
                .legalPerson(feign.getLegalPerson())
                .idCardNo(feign.getIdCardNo())
                .businessLicenseNo(feign.getBusinessLicenseNo())
                .ratio(feign.getRatio())
                .managerUserId(feign.getManagerUserId())
                .build();
    }

    /**
     * 店铺实体 → Feign DTO（图片字段经 urlResolver 转为可访问 URL）。
     */
    public static MobiShopFeign toFeign(MobiShop shop, Function<String, String> urlResolver) {
        if (shop == null) {
            return null;
        }
        return MobiShopFeign.builder()
                .id(shop.getId())
                .parentId(shop.getParentId())
                .shopName(shop.getShopName())
                .shopCode(shop.getShopCode())
                .ratio(shop.getRatio())
                .phone(shop.getPhone())
                .categoryId(shop.getCategoryId())
                .province(shop.getProvince())
                .city(shop.getCity())
                .district(shop.getDistrict())
                .address(shop.getAddress())
                .legalPerson(shop.getLegalPerson())
                .idCardNo(shop.getIdCardNo())
                .businessLicenseNo(shop.getBusinessLicenseNo())
                .idCardFront(resolveUrl(shop.getIdCardFront(), urlResolver))
                .idCardBack(resolveUrl(shop.getIdCardBack(), urlResolver))
                .businessLicensePic(resolveUrl(shop.getBusinessLicensePic(), urlResolver))
                .shopExterior(resolveUrl(shop.getShopExterior(), urlResolver))
                .shopInterior(resolveUrl(shop.getShopInterior(), urlResolver))
                .carouselImages(toPictureList(shop.getPicture()))
                .auditStatus(shop.getAuditStatus())
                .auditReason(shop.getAuditReason())
                .auditTime(shop.getAuditTime())
                .auditId(shop.getAuditId())
                .isEnabled(shop.getIsEnabled())
                .createTime(shop.getCreateTime())
                .updateTime(shop.getUpdateTime())
                .delFlag(shop.getDelFlag())
                .build();
    }

    private static String resolveUrl(String key, Function<String, String> urlResolver) {
        if (key == null || key.isBlank() || urlResolver == null) {
            return key;
        }
        return urlResolver.apply(key.trim());
    }

    public static ShopStatisticsFeign toStatisticsFeign(ShopStatisticsRes source) {
        if (source == null) {
            return null;
        }
        return ShopStatisticsFeign.builder()
                .todayNewUsers(source.getTodayNewUsers())
                .todayOrders(source.getTodayOrders())
                .totalPointsUsed(source.getTotalPointsUsed())
                .todayRechargeAmount(source.getTodayRechargeAmount())
                .trend(source.getTrend() == null ? List.of() : source.getTrend().stream()
                        .map(point -> ShopStatisticsFeign.TrendPoint.builder()
                                .date(point.getDate())
                                .newUsers(point.getNewUsers())
                                .orders(point.getOrders())
                                .pointsUsed(point.getPointsUsed())
                                .rechargeAmount(point.getRechargeAmount())
                                .build())
                        .toList())
                .headShop(source.getHeadShop())
                .branches(source.getBranches() == null ? List.of() : source.getBranches().stream()
                        .map(branch -> ShopStatisticsFeign.BranchShop.builder()
                                .id(branch.getId())
                                .shopName(branch.getShopName())
                                .build())
                        .toList())
                .scope(source.getScope())
                .filterShopId(source.getFilterShopId())
                .build();
    }
}
