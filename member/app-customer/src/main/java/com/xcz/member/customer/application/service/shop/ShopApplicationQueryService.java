package com.xcz.member.customer.application.service.shop;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xcz.commons.core.exception.ServiceException;
import com.xcz.commons.core.utils.StringUtils;
import com.xcz.member.customer.api.dto.response.shop.ShopDetailRes;
import com.xcz.member.customer.api.dto.response.shop.StaffListRes;
import com.xcz.member.customer.application.assemblers.ShopAssembler;
import com.xcz.member.customer.application.command.shop.QueryShopCommand;
import com.xcz.member.customer.domain.dto.shop.ShopBriefDTO;
import com.xcz.member.customer.domain.service.MobiPointsAccountService;
import com.xcz.member.customer.domain.service.MobiShopService;
import com.xcz.member.customer.domain.service.MobiShopUserService;
import com.xcz.member.customer.domain.service.MobiUserService;
import com.xcz.member.customer.infrastructure.cache.ShopCache;
import com.xcz.member.customer.infrastructure.entity.MobiPointsAccount;
import com.xcz.member.customer.infrastructure.entity.MobiShop;
import com.xcz.member.customer.infrastructure.entity.MobiShopUser;
import com.xcz.member.customer.infrastructure.entity.MobiUser;
import com.xcz.member.customer.utils.ShopAccessUtils;
import com.xcz.member.feign.dto.shop.MobiShopFeign;
import com.xcz.commons.oss.service.UploadService;
import com.xcz.commons.security.utils.SecurityUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 店铺查询应用服务：负责读侧编排与缓存旁路逻辑。
 */
@Service
public class ShopApplicationQueryService {

    private static final DateTimeFormatter CREATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Resource
    private MobiShopService mobiShopService;

    @Resource
    private UploadService uploadService;

    @Resource
    private MobiShopUserService mobiShopUserService;

    @Resource
    private MobiPointsAccountService mobiPointsAccountService;

    @Resource
    private MobiUserService mobiUserService;

    /**
     * 店长查看本店店员列表。
     */
    public List<StaffListRes> listShopStaff(Long shopId) {
        if (shopId == null) {
            throw new ServiceException("店铺 ID 不能为空", 400);
        }
        ShopAccessUtils.assertShopManager(mobiShopUserService, shopId);
        List<MobiShopUser> clerks = mobiShopUserService.listClerksByShopId(shopId);
        if (clerks.isEmpty()) {
            return List.of();
        }
        List<Long> userIds = clerks.stream().map(MobiShopUser::getUserId).toList();
        Map<Long, MobiUser> userMap = mobiUserService.listByIds(userIds).stream()
                .collect(Collectors.toMap(MobiUser::getUserId, u -> u, (a, b) -> a));
        return clerks.stream()
                .map(relation -> {
                    MobiUser user = userMap.get(relation.getUserId());
                    return StaffListRes.builder()
                            .userId(relation.getUserId())
                            .nickname(user != null ? user.getNickname() : null)
                            .phone(user != null ? user.getPhone() : null)
                            .createTime(relation.getCreateTime())
                            .build();
                })
                .toList();
    }

    /**
     * 分页查询当前用户关联的店铺列表，按最后进入时间倒序。
     *
     * @param command 查询命令（分页参数）
     * @return 店铺详情分页结果（含用户在各店铺的剩余积分）
     */
    public Page<ShopDetailRes> getShopList(QueryShopCommand command) {
        //按照最后查看的店铺时间排序查询用户店铺
        Long userId = SecurityUtils.getUserId();
        Page<MobiShopUser> mobiShopUserPage = mobiShopUserService.page(
                new Page<>(command.pageNum(), command.pageSize()),
                new LambdaQueryWrapper<MobiShopUser>()
                        .eq(MobiShopUser::getUserId, userId)
                        .orderByDesc(MobiShopUser::getLastEnterTime)
        );
        //如果是空直接返回
        if (mobiShopUserPage.getRecords().isEmpty()) {
            return new Page<>(command.pageNum(), command.pageSize(), 0);
        }
        //提取店铺id以及对应的角色id
        List<MobiShopUser> shopUsers = mobiShopUserPage.getRecords();
        List<Long> ids = shopUsers.stream()
                .map(MobiShopUser::getShopId)
                .toList();
        Map<Long, Integer> shopRoleMap = shopUsers.stream()
                .collect(Collectors.toMap(MobiShopUser::getShopId, MobiShopUser::getRole, (a, b) -> a));
        //根据店铺id查询店铺详情
        List<ShopBriefDTO> shops = resolveShopBriefs(ids);
        List<ShopDetailRes> vos = shops.stream()
                .map(it -> ShopAssembler.toDetailVo(it, shopRoleMap.get(it.id())))
                .toList();
        enrichShopPoints(vos, userId);

        Page<ShopDetailRes> page = new Page<>(command.pageNum(), command.pageSize());
        page.setRecords(vos);
        page.setTotal(mobiShopUserPage.getTotal());
        return page;
    }

    /**
     * 根据店铺编码查询店铺详情
     * @param shopCode  店铺code
     * @return          店铺详细信息
     */
    public ShopDetailRes getDetailByShopCode(String shopCode) {
        return mobiShopService.getShopDetail(shopCode);
    }

    /**
     * 缓存旁路：优先读缓存，未命中时回源数据库并回填缓存。
     *
     * @param shopIds 店铺 ID 列表（保持入参顺序）
     * @return 命中的店铺简要信息列表（跳过不存在的店铺）
     */
    private List<ShopBriefDTO> resolveShopBriefs(List<Long> shopIds) {
        if (shopIds == null || shopIds.isEmpty()) {
            return List.of();
        }

        Map<Long, ShopBriefDTO> cached = ShopCache.getByIds(shopIds);
        List<Long> missedIds = shopIds.stream()
                .filter(id -> !cached.containsKey(id))
                .toList();
        //如果有为查询到的id，则尝试从数据库中查询
        if (!missedIds.isEmpty()) {
            List<ShopBriefDTO> loaded = mobiShopService.listBriefByIds(missedIds);
            ShopCache.putAll(loaded);
            loaded.forEach(dto -> cached.put(dto.id(), dto));
        }
        List<ShopBriefDTO> result = new ArrayList<>(shopIds.size());
        for (Long shopId : shopIds) {
            ShopBriefDTO brief = cached.get(shopId);
            if (brief != null) {
                result.add(brief);
            }
        }
        return result;
    }

    /**
     * 为店铺列表补充用户在对应店铺的积分信息（积分挂载在总店）。
     *
     * @param vos    店铺详情列表
     * @param userId 当前用户 ID
     */
    private void enrichShopPoints(List<ShopDetailRes> vos, Long userId) {
        if (vos.isEmpty()) {
            return;
        }
        //根据总店id来查询用户的积分
        List<Long> ids = vos.stream()
                .map(vo -> resolvePointsShopId(vo.getParentId(), vo.getShopId()))
                .distinct()
                .toList();
        Map<Long, MobiPointsAccount> accountMap = mobiPointsAccountService.list(
                        new LambdaQueryWrapper<MobiPointsAccount>()
                                .eq(MobiPointsAccount::getUserId, userId)
                                .in(MobiPointsAccount::getShopId, ids)
                ).stream()
                .collect(Collectors.toMap(MobiPointsAccount::getShopId, it -> it));

        vos.forEach(vo -> {
            Long pointsShopId = resolvePointsShopId(vo.getParentId(), vo.getShopId());
            applyPoints(vo, accountMap.get(pointsShopId));
        });
    }

    /**
     * 解析积分账户所属的总店 ID。
     *
     * @param parentId 父店铺 ID
     * @param shopId   当前店铺 ID
     * @return 总店 ID
     */
    private Long resolvePointsShopId(Long parentId, Long shopId) {
        if (parentId != null && parentId != 0L) {
            return parentId;
        }
        return shopId;
    }

    /**
     * 将积分账户数据写入店铺详情 VO。
     *
     * @param vo      店铺详情
     * @param account 积分账户；为 null 时不处理
     */
    private void applyPoints(ShopDetailRes vo, MobiPointsAccount account) {
        if (account == null) {
            return;
        }

        vo.setAmount(account.getBasePoints().add(account.getBonusPoints()));
        vo.setTotalAmount(account.getTotalUsedPoints());
    }

    /**
     * 管理后台 Feign：分页查询店铺（支持多条件筛选）。
     */
    public Page<MobiShopFeign> pageForFeign(Integer pageNum, Integer pageSize, String shopName,
                                            String legalPerson, Integer auditStatus, Integer isEnabled,
                                            Integer categoryId, String minCreateTime, String maxCreateTime) {
        int pn = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int ps = pageSize == null || pageSize < 1 ? 10 : pageSize;
        Page<MobiShop> page = mobiShopService.page(
                new Page<>(pn, ps),
                buildAdminShopWrapper(shopName, legalPerson, auditStatus, isEnabled, categoryId, minCreateTime, maxCreateTime)
        );
        Page<MobiShopFeign> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(page.getRecords().stream()
                .map(shop -> ShopAssembler.toFeign(shop, uploadService::getUrl))
                .toList());
        return result;
    }

    /**
     * 管理后台 Feign：待审/驳回店铺列表（auditStatus != 1）。
     */
    public Page<MobiShopFeign> auditListForFeign(Integer pageNum, Integer pageSize, String shopName,
                                                 String legalPerson, String minCreateTime, String maxCreateTime) {
        return pageForFeign(pageNum, pageSize, shopName, legalPerson, 0, null, null, minCreateTime, maxCreateTime);
    }

    /**
     * 管理后台 Feign：店铺详情。
     */
    public MobiShopFeign getForFeign(Long id) {
        if (id == null) {
            throw new ServiceException("店铺ID不能为空", 400);
        }
        MobiShop shop = mobiShopService.getById(id);
        if (shop == null) {
            throw new ServiceException("店铺不存在", 404);
        }
        return ShopAssembler.toFeign(shop, uploadService::getUrl);
    }

    private LambdaQueryWrapper<MobiShop> buildAdminShopWrapper(String shopName, String legalPerson,
                                                               Integer auditStatus, Integer isEnabled,
                                                               Integer categoryId, String minCreateTime,
                                                               String maxCreateTime) {
        LambdaQueryWrapper<MobiShop> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(StringUtils.isNotEmpty(shopName), MobiShop::getShopName, shopName);
        wrapper.like(StringUtils.isNotEmpty(legalPerson), MobiShop::getLegalPerson, legalPerson);
        if (auditStatus != null) {
            if (auditStatus == 1) {
                wrapper.eq(MobiShop::getAuditStatus, 1);
            } else {
                wrapper.ne(MobiShop::getAuditStatus, 1);
            }
        }
        wrapper.eq(isEnabled != null, MobiShop::getIsEnabled, isEnabled);
        wrapper.eq(categoryId != null, MobiShop::getCategoryId, categoryId);
        if (StringUtils.isNotEmpty(minCreateTime)) {
            wrapper.ge(MobiShop::getCreateTime, LocalDateTime.parse(minCreateTime.trim(), CREATE_TIME_FMT));
        }
        if (StringUtils.isNotEmpty(maxCreateTime)) {
            wrapper.le(MobiShop::getCreateTime, LocalDateTime.parse(maxCreateTime.trim(), CREATE_TIME_FMT));
        }
        wrapper.orderByDesc(MobiShop::getCreateTime);
        return wrapper;
    }

}
