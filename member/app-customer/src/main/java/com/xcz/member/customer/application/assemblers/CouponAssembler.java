package com.xcz.member.customer.application.assemblers;

import com.xcz.commons.core.exception.ServiceException;
import com.xcz.commons.core.utils.StringUtils;
import com.xcz.member.customer.api.dto.request.coupon.CouponTemplateReq;
import com.xcz.member.customer.api.dto.request.coupon.UserCouponReq;
import com.xcz.member.customer.api.dto.response.coupon.CouponTemplateRes;
import com.xcz.member.customer.api.dto.response.coupon.UserCouponRes;
import com.xcz.member.customer.application.command.coupon.MutateCouponTemplateCommand;
import com.xcz.member.customer.application.command.coupon.QueryCouponTemplateCommand;
import com.xcz.member.customer.application.command.coupon.QueryUserCouponCommand;
import com.xcz.member.customer.domain.dto.coupon.CouponTemplateBriefDTO;
import com.xcz.member.customer.domain.dto.coupon.UserCouponBriefDTO;
import com.xcz.member.customer.domain.enums.CouponDistributionStatus;
import com.xcz.member.customer.domain.enums.CouponType;
import com.xcz.member.customer.infrastructure.entity.MobiCouponTemplate;
import com.xcz.member.feign.dto.coupon.MobiCouponFeign;
import com.xcz.member.feign.dto.coupon.MobiUserCouponFeign;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 优惠券对象转换器：Controller Req ↔ Command ↔ 领域读模型 ↔ API Res。
 * 写操作按场景校验字段后组装为 {@link MutateCouponTemplateCommand}。
 */
public final class CouponAssembler {

    private CouponAssembler() {
    }

    /**
     * 将 HTTP 查询请求转为券模板列表查询命令。
     *
     * @param req 查询参数
     * @return 查询命令
     */
    public static QueryCouponTemplateCommand toQueryTemplateCommand(CouponTemplateReq dto) {
        if (dto.getShopId() == null) {
            throw new ServiceException("店铺 ID 不能为空", 400);
        }
        return new QueryCouponTemplateCommand(
                dto.getShopId(),
                dto.getDistributionStatus(),
                dto.getPageNum(),
                dto.getPageSize()
        );
    }

    /**
     * 将 HTTP 请求体转为创建券模板命令（校验全量字段）。
     *
     * @param req 券模板信息
     * @return 写操作命令
     */
    public static MutateCouponTemplateCommand toCreateCommand(CouponTemplateReq dto) {
        assertShopId(dto.getShopId());
        assertFullTemplate(dto);
        return new MutateCouponTemplateCommand(
                null,
                dto.getShopId(),
                dto.getCouponName(),
                dto.getType(),
                dto.getThresholdAmount(),
                dto.getDiscountValue(),
                dto.getValidDays(),
                dto.getTotalQuantity(),
                null,
                dto.getDistributionStartTime(),
                dto.getDistributionEndTime()
        );
    }

    /**
     * 将 HTTP 请求体转为全量更新券模板命令（校验全量字段）。
     *
     * @param templateId 模板 ID
     * @param req        券模板信息
     * @return 写操作命令
     */
    public static MutateCouponTemplateCommand toUpdateCommand(Long templateId, CouponTemplateReq dto) {
        assertTemplateId(templateId);
        assertShopId(dto.getShopId());
        assertFullTemplate(dto);
        return new MutateCouponTemplateCommand(
                templateId,
                dto.getShopId(),
                dto.getCouponName(),
                dto.getType(),
                dto.getThresholdAmount(),
                dto.getDiscountValue(),
                dto.getValidDays(),
                dto.getTotalQuantity(),
                null,
                dto.getDistributionStartTime(),
                dto.getDistributionEndTime()
        );
    }

    /**
     * 将 HTTP 请求体转为发放时间调整命令（仅校验发放时间）。
     *
     * @param templateId 模板 ID
     * @param req        发放调整参数
     * @return 写操作命令
     */
    public static MutateCouponTemplateCommand toAdjustCommand(Long templateId, CouponTemplateReq dto) {
        assertTemplateId(templateId);
        assertShopId(dto.getShopId());
        assertDistributionTime(dto.getDistributionStartTime(), dto.getDistributionEndTime());
        return new MutateCouponTemplateCommand(
                templateId,
                dto.getShopId(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                dto.getDistributionStartTime(),
                dto.getDistributionEndTime()
        );
    }

    /**
     * 将 HTTP 请求体转为追加库存命令。
     *
     * @param templateId 模板 ID
     * @param req        追加库存参数
     * @return 写操作命令
     */
    public static MutateCouponTemplateCommand toAddStockCommand(Long templateId, CouponTemplateReq dto) {
        assertTemplateId(templateId);
        assertShopId(dto.getShopId());
        if (dto.getAddQuantity() == null || dto.getAddQuantity() <= 0L) {
            throw new ServiceException("追加库存必须大于 0", 400);
        }
        return new MutateCouponTemplateCommand(
                templateId,
                dto.getShopId(),
                null,
                null,
                null,
                null,
                null,
                null,
                dto.getAddQuantity(),
                null,
                null
        );
    }

    /**
     * 将 HTTP 查询请求转为我的优惠券查询命令。
     *
     * @param req 查询参数
     * @return 查询命令
     */
    public static QueryUserCouponCommand toQueryUserCouponCommand(UserCouponReq dto) {
        return new QueryUserCouponCommand(
                dto.getShopId(),
                dto.getStatus(),
                dto.getPageNum(),
                dto.getPageSize()
        );
    }

    /**
     * 将券模板实体转为简要读模型（含计算阶段与剩余库存）。
     *
     * @param template 券模板实体
     * @param now      当前时间
     * @return 简要读模型
     */
    public static CouponTemplateBriefDTO toBriefDto(MobiCouponTemplate template, LocalDateTime now) {
        CouponDistributionStatus status = CouponDistributionStatus.resolve(template, now);
        return CouponTemplateBriefDTO.builder()
                .templateId(template.getTemplateId())
                .shopId(template.getShopId())
                .couponName(template.getCouponName())
                .type(template.getType())
                .thresholdAmount(template.getThresholdAmount())
                .discountValue(template.getDiscountValue())
                .validDays(template.getValidDays())
                .totalQuantity(template.getTotalQuantity())
                .issuedQuantity(template.getIssuedQuantity())
                .remainingQuantity(CouponDistributionStatus.remainingStock(template))
                .distributionStartTime(template.getDistributionStartTime())
                .distributionEndTime(template.getDistributionEndTime())
                .distributionStatus(status == null ? template.getDistributionStatus() : status.getCode())
                .createTime(template.getCreateTime())
                .build();
    }

    /** 将券模板实体转为简要读模型（使用当前时间计算阶段）。 */
    public static CouponTemplateBriefDTO toBriefDto(MobiCouponTemplate template) {
        return toBriefDto(template, LocalDateTime.now());
    }

    /** 将券模板简要读模型转为 API 响应。 */
    public static CouponTemplateRes toVo(CouponTemplateBriefDTO source) {
        if (source == null) {
            return null;
        }
        return CouponTemplateRes.builder()
                .templateId(source.getTemplateId())
                .shopId(source.getShopId())
                .couponName(source.getCouponName())
                .type(source.getType())
                .thresholdAmount(source.getThresholdAmount())
                .discountValue(source.getDiscountValue())
                .validDays(source.getValidDays())
                .totalQuantity(source.getTotalQuantity())
                .issuedQuantity(source.getIssuedQuantity())
                .remainingQuantity(source.getRemainingQuantity())
                .distributionStartTime(source.getDistributionStartTime())
                .distributionEndTime(source.getDistributionEndTime())
                .distributionStatus(source.getDistributionStatus())
                .createTime(source.getCreateTime())
                .build();
    }

    /** 将用户持券简要读模型转为 API 响应。 */
    public static UserCouponRes toVo(UserCouponBriefDTO source) {
        if (source == null) {
            return null;
        }
        return UserCouponRes.builder()
                .userCouponId(source.getUserCouponId())
                .userId(source.getUserId())
                .nickname(source.getNickname())
                .shopId(source.getShopId())
                .templateId(source.getTemplateId())
                .couponName(source.getCouponName())
                .type(source.getType())
                .thresholdAmount(source.getThresholdAmount())
                .discountValue(source.getDiscountValue())
                .status(source.getStatus())
                .receiveTime(source.getReceiveTime())
                .usedTime(source.getUsedTime())
                .expireTime(source.getExpireTime())
                .shopName(source.getShopName())
                .build();
    }

    /** 券模板响应 → Feign DTO。 */
    public static MobiCouponFeign toFeign(CouponTemplateRes source) {
        if (source == null) {
            return null;
        }
        return MobiCouponFeign.builder()
                .templateId(source.getTemplateId())
                .shopId(source.getShopId())
                .couponName(source.getCouponName())
                .type(source.getType())
                .thresholdAmount(source.getThresholdAmount())
                .discountValue(source.getDiscountValue())
                .validDays(source.getValidDays())
                .totalQuantity(source.getTotalQuantity())
                .issuedQuantity(source.getIssuedQuantity())
                .distributionStartTime(source.getDistributionStartTime())
                .distributionEndTime(source.getDistributionEndTime())
                .distributionStatus(source.getDistributionStatus())
                .shopName(source.getShopName())
                .createTime(source.getCreateTime())
                .build();
    }

    /** Feign DTO → 创建券模板命令。 */
    public static MutateCouponTemplateCommand toCreateCommand(MobiCouponFeign body) {
        CouponTemplateReq req = toTemplateReq(body);
        return toCreateCommand(req);
    }

    /** Feign DTO → 更新券模板命令。 */
    public static MutateCouponTemplateCommand toUpdateCommand(MobiCouponFeign body) {
        CouponTemplateReq req = toTemplateReq(body);
        return toUpdateCommand(body.getTemplateId(), req);
    }

    /** 用户持券响应 → Feign DTO。 */
    public static MobiUserCouponFeign toFeign(UserCouponRes source) {
        if (source == null) {
            return null;
        }
        return MobiUserCouponFeign.builder()
                .userCouponId(source.getUserCouponId())
                .userId(source.getUserId())
                .nickname(source.getNickname())
                .shopId(source.getShopId())
                .shopName(source.getShopName())
                .templateId(source.getTemplateId())
                .couponName(source.getCouponName())
                .status(source.getStatus())
                .receiveTime(source.getReceiveTime())
                .usedTime(source.getUsedTime())
                .expireTime(source.getExpireTime())
                .build();
    }

    private static CouponTemplateReq toTemplateReq(MobiCouponFeign body) {
        if (body == null) {
            throw new ServiceException("优惠券数据不能为空", 400);
        }
        CouponTemplateReq req = new CouponTemplateReq();
        req.setShopId(body.getShopId());
        req.setCouponName(body.getCouponName());
        req.setType(body.getType());
        req.setThresholdAmount(body.getThresholdAmount());
        req.setDiscountValue(body.getDiscountValue());
        req.setValidDays(body.getValidDays());
        req.setTotalQuantity(body.getTotalQuantity());
        req.setDistributionStartTime(body.getDistributionStartTime());
        req.setDistributionEndTime(body.getDistributionEndTime());
        return req;
    }

    private static void assertShopId(Long shopId) {
        if (shopId == null) {
            throw new ServiceException("店铺 ID 不能为空", 400);
        }
    }

    private static void assertTemplateId(Long templateId) {
        if (templateId == null) {
            throw new ServiceException("优惠券模板 ID 不能为空", 400);
        }
    } 

    private static void assertFullTemplate(CouponTemplateReq dto) {
        if (StringUtils.isEmpty(dto.getCouponName())) {
            throw new ServiceException("优惠券名称不能为空", 400);
        }
        if (CouponType.getByCode(dto.getType()) == null) {
            throw new ServiceException("优惠券类型无效", 400);
        }
        if (dto.getThresholdAmount() != null && dto.getThresholdAmount().compareTo(BigDecimal.ZERO) < 0) {
            throw new ServiceException("使用门槛不能为负数", 400);
        }
        if (dto.getDiscountValue() == null || dto.getDiscountValue().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("优惠力度必须大于 0", 400);
        }
        if (dto.getType() != null && dto.getType() == CouponType.DISCOUNT.getCode()
                && dto.getDiscountValue().compareTo(new BigDecimal("100")) > 0) {
            throw new ServiceException("折扣力度不能超过 100", 400);
        }
        if (dto.getValidDays() != null && dto.getValidDays() <= 0) {
            throw new ServiceException("领取后有效天数必须大于 0", 400);
        }
        if (dto.getTotalQuantity() != null && dto.getTotalQuantity() <= 0L) {
            throw new ServiceException("发放数量须为正整数，或留空表示无限", 400);
        }
        assertDistributionTime(dto.getDistributionStartTime(), dto.getDistributionEndTime());
    }

    private static void assertDistributionTime(LocalDateTime start, LocalDateTime end) {
        if (start != null && end != null && !start.isBefore(end)) {
            throw new ServiceException("结束发放时间须晚于开始发放时间", 400);
        }
    }
}
