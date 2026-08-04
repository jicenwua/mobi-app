package com.xcz.member.customer.utils;

import com.xcz.commons.core.exception.ServiceException;

import java.security.SecureRandom;
import java.util.function.Predicate;

/**
 * 店铺编码生成工具类
 */
public final class ShopCodeUtils {

    private static final String SHOP_CODE_ALPHABET =
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-/";

    private static final int SHOP_CODE_LEN = 8;

    private static final SecureRandom SHOP_CODE_RANDOM = new SecureRandom();

    private static final int MAX_RETRY_COUNT = 50;

    private ShopCodeUtils() {
    }

    public static String generateUniqueCode(Predicate<String> existsChecker) {
        for (int i = 0; i < MAX_RETRY_COUNT; i++) {
            String code = generateRandomShopCode();
            if (!existsChecker.test(code)) {
                return code;
            }
        }
        throw new ServiceException("生成店铺编码失败", 500);
    }

    private static String generateRandomShopCode() {
        StringBuilder sb = new StringBuilder(SHOP_CODE_LEN);
        for (int j = 0; j < SHOP_CODE_LEN; j++) {
            sb.append(SHOP_CODE_ALPHABET.charAt(
                    SHOP_CODE_RANDOM.nextInt(SHOP_CODE_ALPHABET.length())));
        }
        return sb.toString();
    }
}
