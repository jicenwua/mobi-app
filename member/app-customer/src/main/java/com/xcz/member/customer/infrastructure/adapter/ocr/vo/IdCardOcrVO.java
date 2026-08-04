package com.xcz.member.customer.infrastructure.adapter.ocr.vo;

import lombok.Data;

/**
 * 身份证识别结果VO
 */
@Data
public class IdCardOcrVO {
    /***姓名**/
    private String name;
    /***身份证号**/
    private String idNumber;
    /***地址**/
    private String address;
    /***原始JSON数据**/
    private String rawJson;
}
