package com.xcz.member.customer.infrastructure.adapter.ocr;

import com.xcz.member.customer.infrastructure.adapter.ocr.vo.IdCardOcrVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 身份证 OCR 识别
 */
public interface IdCardOcrService {

    /**
     * 识别身份证正面
     *
     * @param file 身份证图片
     * @return 姓名、身份证号、地址等
     * @throws Exception 调用 OCR 或解析失败时抛出
     */
    IdCardOcrVO recognize(MultipartFile file) throws Exception;
}
