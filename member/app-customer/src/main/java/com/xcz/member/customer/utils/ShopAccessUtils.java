package com.xcz.member.customer.utils;

import com.xcz.commons.core.constant.SecurityConstants;
import com.xcz.commons.core.exception.ServiceException;
import com.xcz.commons.core.utils.ServletUtils;
import com.xcz.commons.core.utils.SpringUtils;
import com.xcz.commons.core.utils.StringUtils;
import com.xcz.member.customer.domain.enums.Constants;
import com.xcz.member.customer.domain.enums.ShopUserRole;
import com.xcz.member.customer.domain.service.MobiShopUserService;
import com.xcz.member.customer.infrastructure.entity.MobiShopUser;
import com.xcz.member.feign.config.properties.InternalServiceProperties;
import com.xcz.commons.security.utils.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RMapCache;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 校验当前登录用户对店铺的管理权限（店长/店员/顾客）
 */
@Slf4j
@UtilityClass
public class ShopAccessUtils {

    private static final String SHOP_ROLE_KEY = "shop:role:";
    private static final long SHOP_ROLE_EXPIRE_MILLIS = 1000 * 60 * 60L;

    /**
     * 当前请求是否为管理后台经 Feign 转发的内部调用。
     * 仅信任 {@link SecurityConstants#INTERNAL_SERVICE_HEADER} 与配置令牌匹配。
     */
    public static boolean isFeignInvoke() {
        HttpServletRequest request = ServletUtils.getRequest();
        if (request == null) {
            return false;
        }
        InternalServiceProperties props = SpringUtils.getBean(InternalServiceProperties.class);
        String internal = ServletUtils.getHeader(request, SecurityConstants.INTERNAL_SERVICE_HEADER);
        return StringUtils.isNotEmpty(internal) && props.getToken().equals(internal.trim());
    }

    /**
     * 校验当前用户是否已加入该店铺（任意角色）
     */
    public static void assertShopMember(MobiShopUserService shopUserService, Long shopId) {
        if (isFeignInvoke()) {
            return;
        }
        if (getRoleQuietly(shopUserService, shopId) == null) {
            throw new ServiceException("未加入该店铺");
        }
    }

    /**
     * 校验当前用户是否为该店铺的店长或店员
     */
    public static void assertShopStaff(MobiShopUserService shopUserService, Long shopId) {
        if (isFeignInvoke()) {
            return;
        }
        if (!isShopStaff(shopUserService, shopId)) {
            throw new ServiceException("无权访问该店铺");
        }
    }

    /**
     * 判断当前用户是否为该店铺的店长或店员
     */
    public static boolean isShopStaff(MobiShopUserService shopUserService, Long shopId) {
        if (isFeignInvoke()) {
            return true;
        }
        Integer roleCode = getRoleQuietly(shopUserService, shopId);
        return ShopUserRole.isClerkRole(roleCode);
    }

    /**
     * 校验当前用户是否为该店铺的顾客（已加入会员）
     */
    public static void assertShopCustomer(MobiShopUserService shopUserService, Long shopId) {
        if (isFeignInvoke()) {
            return;
        }
        ShopUserRole role = ShopUserRole.getByCode(getRole(shopUserService, shopId));
        if (role == null || !role.isCustomerCapable()) {
            throw new ServiceException("未加入该店铺");
        }
    }

    /**
     * 校验指定用户是否为该店铺的顾客（店员扫码扣款时使用）
     */
    public static void assertUserIsShopCustomer(MobiShopUserService shopUserService, Long userId, Long shopId) {
        if (userId == null || shopId == null) {
            throw new ServiceException("会员或店铺信息无效");
        }
        MobiShopUser relation = shopUserService.getByShopIdAndUserId(shopId, userId);
        if (relation == null || !ShopUserRole.isCustomerRole(relation.getRole())) {
            throw new ServiceException("该会员未加入本店");
        }
    }

    /**
     * 校验当前用户是否为该店铺的店长
     */
    public static void assertShopManager(MobiShopUserService shopUserService, Long shopId) {
        if (isFeignInvoke()) {
            return;
        }
        ShopUserRole role = ShopUserRole.getByCode(getRole(shopUserService, shopId));
        if (role != ShopUserRole.MANAGER) {
            throw new ServiceException("无权限操作，请联系店长");
        }
    }

    private static Integer getRole(MobiShopUserService shopUserService, Long shopId) {
        Integer role = getRoleQuietly(shopUserService, shopId);
        if (role == null) {
            throw new ServiceException("不在该店铺内");
        }
        return role;
    }

    private static Integer getRoleQuietly(MobiShopUserService shopUserService, Long shopId) {
        Long userId = SecurityUtils.getUserId();
        RMapCache<Long, Integer> mapCache = Constants.CACHE.getMapCache(SHOP_ROLE_KEY + userId);
        Integer role = mapCache.get(shopId);
        if (role == null) {
            Map<Long, Integer> collect = shopUserService.listByUserId(userId)
                    .stream()
                    .collect(Collectors.toMap(
                            MobiShopUser::getShopId,
                            MobiShopUser::getRole
                    ));
            mapCache.putAll(collect, SHOP_ROLE_EXPIRE_MILLIS, TimeUnit.MILLISECONDS);
            role = collect.get(shopId);
        }
        return role;
    }

    /**
     * 清除用户店铺角色权限缓存。
     * <p>
     * 通常由 {@link com.xcz.member.customer.infrastructure.service.MobiShopUserServiceImpl}
     * 在角色关系变更成功后自动调用，应用层无需手动清理。
     */
    public static void clearRoleCache(Long userId) {
        Constants.CACHE.getMapCache(SHOP_ROLE_KEY + userId).delete();
    }
}
