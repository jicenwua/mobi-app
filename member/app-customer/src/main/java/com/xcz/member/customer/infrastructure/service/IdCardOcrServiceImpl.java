package com.xcz.member.customer.infrastructure.service;

import com.aliyun.ocr_api20210707.Client;
import com.aliyun.ocr_api20210707.models.RecognizeIdcardRequest;
import com.aliyun.ocr_api20210707.models.RecognizeIdcardResponse;
import com.aliyun.teaopenapi.models.Config;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xcz.commons.core.exception.ServiceException;
import com.xcz.member.customer.infrastructure.adapter.ocr.IdCardOcrService;
import com.xcz.member.customer.infrastructure.adapter.ocr.vo.IdCardOcrVO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 调用阿里云 RecognizeIdcard，解析 data JSON
 */
@Service
public class IdCardOcrServiceImpl implements IdCardOcrService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Value("${aliyun.ocr.enabled:false}")
    private boolean ocrEnabled;

    @Value("${aliyun.ocr.endpoint:ocr-api.cn-hangzhou.aliyuncs.com}")
    private String ocrEndpoint;

    @Value("${aliyun.oss.access-key-id:}")
    private String accessKeyId;

    @Value("${aliyun.oss.access-key-secret:}")
    private String accessKeySecret;

    /**
     * 识别身份证图片信息
     *
     * @param file 身份证图片文件
     * @return 身份证识别结果VO
     * @throws Exception 识别异常
     */
    @Override
    public IdCardOcrVO recognize(MultipartFile file) throws Exception {
        // 检查OCR功能是否启用
        if (!ocrEnabled) {
            throw new ServiceException("未开启身份证 OCR，请在配置中设置 aliyun.ocr.enabled=true 并保证 AccessKey 具备 OCR 权限", 400);
        }

        // 验证AccessKey配置是否完整
        if (accessKeyId == null || accessKeyId.isBlank() || accessKeySecret == null || accessKeySecret.isBlank()) {
            throw new ServiceException("未配置 aliyun.oss.access-key-id / access-key-secret，OCR 无法调用", 400);
        }

        // 创建阿里云OCR客户端配置
        Config config = new Config()
                .setAccessKeyId(accessKeyId.trim())
                .setAccessKeySecret(accessKeySecret.trim())
                .setEndpoint(ocrEndpoint);
        Client client = new Client(config);

        // 构建身份证识别请求
        RecognizeIdcardRequest request = new RecognizeIdcardRequest()
                .setBody(file.getInputStream());

        // 执行识别请求
        RecognizeIdcardResponse response = client.recognizeIdcard(request);
        if (response == null || response.getBody() == null) {
            throw new ServiceException("OCR 无返回", 502);
        }

        // 获取识别结果JSON字符串
        String dataStr = response.getBody().getData();
        IdCardOcrVO vo = new IdCardOcrVO();
        vo.setRawJson(dataStr);
        if (dataStr == null || dataStr.isBlank()) {
            return vo;
        }

        // 解析JSON数据
        JsonNode root = MAPPER.readTree(dataStr);
        JsonNode face = root.path("face").path("data");
        JsonNode back = root.path("back").path("data");

        // 优先使用正面数据，如果不存在则使用反面数据
        JsonNode block = face.isMissingNode() || face.isNull() ? back : face;
        vo.setName(text(block, "name"));
        vo.setIdNumber(text(block, "idNumber"));
        vo.setAddress(text(block, "address"));

        // 如果正面数据不存在，尝试从根节点获取
        if (vo.getName() == null && face.isMissingNode()) {
            JsonNode alt = root.path("data");
            vo.setName(text(alt, "name"));
            vo.setIdNumber(text(alt, "idNumber"));
            vo.setAddress(text(alt, "address"));
        }
        return vo;
    }

    /**
     * 从JSON节点中提取文本字段值
     *
     * @param node JSON节点
     * @param field 字段名
     * @return 提取的文本值，如果不存在或为空则返回null
     */
    private static String text(JsonNode node, String field) {
        // 检查节点是否为空
        if (node == null || node.isMissingNode()) {
            return null;
        }

        // 获取指定字段
        JsonNode v = node.get(field);
        if (v == null || v.isNull()) {
            return null;
        }

        // 返回修剪后的文本值
        String s = v.asText();
        return s == null || s.isBlank() ? null : s.trim();
    }
}
