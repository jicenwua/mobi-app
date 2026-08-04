package com.xcz.member.customer.application.service.point;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xcz.commons.core.exception.ServiceException;
import com.xcz.member.customer.api.dto.request.point.PointReq;
import com.xcz.member.customer.api.dto.response.point.ConsumeLogDetailRes;
import com.xcz.member.customer.api.dto.response.point.ConsumeLogRes;
import com.xcz.member.customer.api.dto.response.point.PointsAccountListRes;
import com.xcz.member.customer.application.assemblers.PointAssembler;
import com.xcz.member.customer.domain.dto.points.PointsShopQuery;
import com.xcz.member.customer.domain.dto.shop.ShopBriefDTO;
import com.xcz.member.customer.domain.service.MobiPointsAccountService;
import com.xcz.member.customer.domain.service.MobiPointsLogService;
import com.xcz.member.customer.domain.service.MobiShopService;
import com.xcz.member.customer.domain.service.MobiShopUserService;
import com.xcz.member.customer.domain.vo.PointsAccountVO;
import com.xcz.member.customer.domain.vo.PointsLogBriefVO;
import com.xcz.member.customer.infrastructure.cache.ShopCache;
import com.xcz.member.customer.utils.ShopAccessUtils;
import com.xcz.commons.security.utils.SecurityUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 积分读侧应用服务：权限校验、总店账户解析、列表与详情编排。
 */
@Service
public class PointApplicationQueryService {

    @Resource
    private MobiPointsAccountService mobiPointsAccountService;
    @Resource
    private MobiPointsLogService mobiPointsLogService;
    @Resource
    private MobiShopUserService mobiShopUserService;
    @Resource
    private MobiShopService mobiShopService;

    /**
     * 校验店铺 ID 非空。
     */
    private static Long requireShopId(Long shopId) {
        if (shopId == null) {
            throw new ServiceException("店铺ID不能为空");
        }
        return shopId;
    }

    /**
     * 将积分账户分页读模型转换为小程序 VO 分页结果。
     */
    private static Page<PointsAccountListRes> mapAccountPage(Page<PointsAccountVO> page) {
        Page<PointsAccountListRes> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(PointAssembler::toAccountVo).toList());
        return result;
    }

    /**
     * 将消费流水分页读模型转换为小程序 VO 分页结果。
     */
    private static Page<ConsumeLogRes> mapConsumeLogPage(Page<PointsLogBriefVO> page) {
        Page<ConsumeLogRes> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream().map(PointAssembler::toConsumeLogRes).toList());
        return result;
    }

    /**
     * 店员查看店铺会员积分账户（积分挂载总店）。
     */
    public Page<PointsAccountListRes> pageShopAccounts(PointReq dto) {
        Long shopId = requireShopId(dto.getShopId());
        ShopAccessUtils.assertShopStaff(mobiShopUserService, shopId);
        PointsShopQuery query = PointAssembler.toShopQuery(dto);
        query.setShopId(resolvePointsShopId(shopId));
        Page<PointsAccountVO> page = mobiPointsAccountService.pageByShop(query);
        return mapAccountPage(page);
    }

    /**
     * 店员查看店铺全部消费流水（默认仅消耗类、按消费时间倒序）。
     */
    public Page<ConsumeLogRes> pageShopConsumeLogs(PointReq dto) {
        Long shopId = requireShopId(dto.getShopId());
        ShopAccessUtils.assertShopStaff(mobiShopUserService, shopId);
        return mapConsumeLogPage(pageConsumeLogs(shopId, null, dto));
    }

    /**
     * 顾客查看本人在全部店铺的消费订单（分页，可按状态筛选）。
     */
    public Page<ConsumeLogRes> pageMyOrders(PointReq dto) {
        Long userId = SecurityUtils.getUserId();
        PointsShopQuery query = PointAssembler.toShopQuery(dto);
        query.setUserId(userId);
        return mapConsumeLogPage(mobiPointsLogService.pageLogByShop(query, true));
    }

    /**
     * 顾客查看本人在店铺的消费流水。
     */
    public Page<ConsumeLogRes> pageMyConsumeLogs(PointReq dto) {
        Long shopId = requireShopId(dto.getShopId());
        ShopAccessUtils.assertShopMember(mobiShopUserService, shopId);
        Long userId = SecurityUtils.getUserId();
        return mapConsumeLogPage(pageConsumeLogs(shopId, userId, dto));
    }

    /**
     * 消费记录详情（按需加载商品明细与优惠券信息）。
     */
    public ConsumeLogDetailRes getConsumeLogDetail(Long logId, Long shopId) {
        PointsLogBriefVO detail = mobiPointsLogService.getLogDetail(logId);
        if (detail == null || !shopId.equals(detail.getShopId())) {
            throw new ServiceException("消费记录不存在");
        }
        assertLogReadable(detail, shopId);
        return PointAssembler.toConsumeLogDetailRes(detail);
    }

    /**
     * 后台 Feign：店铺积分账户列表。
     */
    public Page<PointsAccountVO> pageAccountsForFeign(PointsShopQuery query) {
        if (query.getShopId() != null) {
            query.setShopId(resolvePointsShopId(query.getShopId()));
        }
        return mobiPointsAccountService.pageByShop(query);
    }

    /**
     * 后台 Feign：店铺消费流水（含商品明细，供管理端使用）。
     */
    public Page<PointsLogBriefVO> pageLogsForFeign(PointsShopQuery query) {
        if (query.getShopId() != null) {
            query.setShopId(resolvePointsShopId(query.getShopId()));
        }
        return mobiPointsLogService.pageLogByShop(query, true);
    }

    /**
     * 后台 Feign：消费流水详情（含商品明细与优惠券信息）。
     */
    public PointsLogBriefVO getLogDetailForFeign(Long logId, Long shopId) {
        if (logId == null) {
            throw new ServiceException("记录ID不能为空");
        }
        PointsLogBriefVO detail = mobiPointsLogService.getLogDetail(logId);
        if (detail == null) {
            throw new ServiceException("消费记录不存在");
        }
        if (shopId != null && !shopId.equals(detail.getShopId())) {
            throw new ServiceException("消费记录不存在");
        }
        return detail;
    }

    /**
     * 分页查询店铺消费流水；userId 非空时限定为指定顾客。
     */
    private Page<PointsLogBriefVO> pageConsumeLogs(Long shopId, Long userId, PointReq dto) {
        PointsShopQuery query = PointAssembler.toShopQuery(dto);
        query.setShopId(shopId);
        query.setUserId(userId);
        return mobiPointsLogService.pageLogByShop(query, false);
    }

    /**
     * 校验当前用户是否有权查看该消费记录（店员可查店铺内任意记录，顾客仅可查本人）。
     */
    private void assertLogReadable(PointsLogBriefVO detail, Long shopId) {
        //如果是店铺工作人员，则可以查看店铺内所有的
        if (ShopAccessUtils.isShopStaff(mobiShopUserService, shopId)) {
            return;
        }
        //如果是自己的记录，则可以直接查看自己的
        Long currentUserId = SecurityUtils.getUserId();
        if (!currentUserId.equals(detail.getUserId())) {
            throw new ServiceException("无权查看该消费记录");
        }
    }

    /**
     * 解析积分挂载的总店 ID（分店积分统一归属总店账户）。
     */
    private Long resolvePointsShopId(Long shopId) {
        Map<Long, ShopBriefDTO> cached = ShopCache.getByIds(List.of(shopId));
        ShopBriefDTO brief = cached.get(shopId);
        if (brief == null) {
            List<ShopBriefDTO> loaded = mobiShopService.listBriefByIds(List.of(shopId));
            if (loaded.isEmpty()) {
                throw new ServiceException("店铺不存在");
            }
            brief = loaded.getFirst();
            ShopCache.putAll(loaded);
        }
        return brief.headShopId();
    }

}
