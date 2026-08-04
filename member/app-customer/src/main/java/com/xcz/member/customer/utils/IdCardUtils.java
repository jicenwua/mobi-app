package com.xcz.member.customer.utils;



import com.xcz.member.customer.infrastructure.adapter.ocr.vo.IdCardOcrVO;

import java.util.Locale;

/**
 * 身份证信息处理工具类
 * 提供身份证OCR结果验证和标准化功能
 */
public class IdCardUtils {

    private IdCardUtils() {
        // 防止实例化
    }

    /**
     * 验证身份证OCR结果与表单数据是否一致
     *
     * @param ocr OCR识别结果
     * @param legalPerson 表单中的法人姓名
     * @param idCardNo 表单中的身份证号
     * @return true表示一致
     */
    public static boolean idCardMatchesForm(IdCardOcrVO ocr, String legalPerson, String idCardNo) {
        // OCR结果为空则不一致
        if (ocr == null) {
            return false;
        }

        // 标准化姓名字符串并比较
        String oName = normalizePersonName(ocr.getName());
        String fName = normalizePersonName(legalPerson);

        // 标准化身份证号并比较
        String oId = normalizeIdNumber(ocr.getIdNumber());
        String fId = normalizeIdNumber(idCardNo);

        // 如果标准化后为空则不一致
        if (oName.isEmpty() || oId.isEmpty()) {
            return false;
        }

        // 比较姓名和身份证号是否都一致
        return oName.equals(fName) && oId.equals(fId);
    }

    /**
     * 标准化姓名字符串，去除空格和特殊字符
     *
     * @param s 原始姓名
     * @return 标准化后的姓名
     */
    public static String normalizePersonName(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\u3000", "").replaceAll("\\s+", "").trim();
    }

    /**
     * 标准化身份证号，去除空格并转为大写
     *
     * @param s 原始身份证号
     * @return 标准化后的身份证号
     */
    public static String normalizeIdNumber(String s) {
        if (s == null) {
            return "";
        }
        return s.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }
}
