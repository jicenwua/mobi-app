package com.xcz.member.gateway.util;

import lombok.experimental.UtilityClass;

import java.net.URI;
import java.util.regex.Pattern;

/**
 * 访问日志 URI 脱敏工具，避免 token 等敏感 query 明文落盘。
 */
@UtilityClass
public class SensitiveUriUtils {

    private static final Pattern SENSITIVE_QUERY_VALUE = Pattern.compile(
            "(?i)([?&](?:token|ticket)=)[^&]*");

    /**
     * 将 URI 中 token、ticket 等参数的 value 替换为 ***。
     */
    public static String redact(URI uri) {
        if (uri == null) {
            return "";
        }
        return SENSITIVE_QUERY_VALUE.matcher(uri.toString()).replaceAll("$1***");
    }
}
