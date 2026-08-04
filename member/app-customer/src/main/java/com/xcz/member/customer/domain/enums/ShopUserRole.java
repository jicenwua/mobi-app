package com.xcz.member.customer.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 店铺用户角色枚举
 */
@Getter
@AllArgsConstructor
public enum ShopUserRole {

    /** 店长 */
    MANAGER(1, "店长"),

    /** 店员 */
    CLERK(2, "店员"),

    /** 顾客 */
    CUSTOMER(3, "顾客"),
    ;

    /***角色代码**/
    private final Integer code;
    /***角色描述**/
    private final String description;

    /** 已废弃的历史编码：顾客兼店员（读侧兼容，新数据不再写入） */
    private static final int LEGACY_CUST_AND_CLERK = 4;

    /**
     * 根据角色代码获取枚举。
     *
     * @param code 角色代码
     * @return 对应枚举，未匹配时返回 null
     */
    public static ShopUserRole getByCode(Integer code) {
        if (code == null) {
            return null;
        }
        if (code == LEGACY_CUST_AND_CLERK) {
            return CUSTOMER;
        }
        for (ShopUserRole role : values()) {
            if (role.getCode().equals(code)) {
                return role;
            }
        }
        return null;
    }

    /**
     * 是否具备顾客身份（可查看/使用顾客积分）。
     */
    public boolean isCustomerCapable() {
        return this == CUSTOMER;
    }

    /**
     * 是否具备店员身份。
     */
    public boolean isClerkCapable() {
        return this == CLERK || this == MANAGER;
    }

    /**
     * 判断角色编码是否具备顾客身份。
     *
     * @param code 角色编码
     * @return 是否具备顾客身份
     */
    public static boolean isCustomerRole(Integer code) {
        if (code == null) {
            return false;
        }
        if (code == LEGACY_CUST_AND_CLERK || code == CUSTOMER.code) {
            return true;
        }
        ShopUserRole role = getByCode(code);
        return role != null && role.isCustomerCapable();
    }

    /**
     * 判断角色编码是否具备店员身份。
     *
     * @param code 角色编码
     * @return 是否具备店员身份
     */
    public static boolean isClerkRole(Integer code) {
        if (code == null) {
            return false;
        }
        if (code == LEGACY_CUST_AND_CLERK || code == CLERK.code) {
            return true;
        }
        ShopUserRole role = getByCode(code);
        return role != null && role.isClerkCapable();
    }

    /**
     * 用户以顾客身份加入店铺时，在已有角色基础上合并顾客身份（单条记录，不新增关系）。
     * <ul>
     *   <li>无记录时由调用方新建 {@link #CUSTOMER}</li>
     *   <li>已是店长/店员/顾客 → 保持原角色</li>
     * </ul>
     *
     * @param currentRoleCode 当前角色编码，无记录时传 null
     * @return 合并后的角色编码
     */
    public static Integer mergeCustomerRole(Integer currentRoleCode) {
        ShopUserRole current = getByCode(currentRoleCode);
        if (current == null) {
            return CUSTOMER.code;
        }
        return switch (current) {
            case MANAGER, CLERK, CUSTOMER -> current.code;
        };
    }

}
