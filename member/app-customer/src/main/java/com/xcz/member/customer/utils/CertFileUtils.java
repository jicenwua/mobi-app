package com.xcz.member.customer.utils;

import com.xcz.commons.core.exception.ServiceException;
import com.xcz.member.customer.domain.enums.Constants;
import com.xcz.commons.oss.service.UploadService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 证书文件处理工具类
 * 提供证书目录构建、文件上传和回滚等功能
 */
public class CertFileUtils {

    private CertFileUtils() {
        // 防止实例化
    }

    /**
     * 拼接头像文件路径
     *
     * @param fileName 头像文件名
     * @return 保存路径
     */
    public static String avatarFolder(String fileName) {
        if(fileName == null)
            throw new ServiceException("请上传正确文件格式");
        String ext = null;
        String name = null;
        if (fileName.contains(".")) {
            ext = fileName.substring(fileName.lastIndexOf("."));
            name = StringUtils.trim(fileName.substring(0, fileName.lastIndexOf(".")));
        } else {
            ext = ".jpg";
            name = String.valueOf(System.currentTimeMillis());
        }
        return Constants.AVATAR_DIRECTORY + name + ext;
    }

    /**
     * 构建证书文件存储目录名（店铺名+法人）
     *
     * @param shopName    店铺名称
     * @param legalPerson 法人姓名
     * @return 安全的目录名
     */
    public static String buildCertFolder(String shopName, String legalPerson) {
        // 拼接店铺名和法人姓名
        String raw = shopName.trim() + "_" + legalPerson.trim();

        // sanitization处理，替换非法字符
        String folder = sanitizeOssDirSegment(raw);
        if (folder.isEmpty()) {
            throw new ServiceException("店铺名与法人姓名不能为空", 400);
        }

        // 限制目录名长度不超过120个字符
        if (folder.length() > 120) {
            folder = folder.substring(0, 120);
        }
        return Constants.SHOP_CERT_DIRECTORY + folder;
    }

    /**
     * 清理OSS目录段中的非法字符
     *
     * @param raw 原始字符串
     * @return 清理后的安全字符串
     */
    public static String sanitizeOssDirSegment(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        // 替换路径分隔符、特殊字符和连续空格为下划线
        return raw.replaceAll("[\\\\/:*?\"<>|\\s]+", "_").replaceAll("_{2,}", "_");
    }

    /**
     * 上传文件到指定目录
     *
     * @param file          待上传的文件
     * @param baseDir       基础目录路径
     * @param uploadService OSS上传服务
     * @return OSS对象key
     * @throws IOException IO异常
     */
    public static String uploadToDir(MultipartFile file, String baseDir, UploadService uploadService) throws IOException {
        // 获取原始文件名，如果为空则使用默认名称
        String orig = file.getOriginalFilename();
        if (orig == null || orig.isBlank()) {
            orig = "image.jpg";
        }

        // 确保文件名包含扩展名
        if (!orig.contains(".")) {
            orig = orig + ".jpg";
        }

        // 生成唯一的对象key并上传
        String objectKey = baseDir + "/" + orig;
        return uploadService.simpleUpload(file, objectKey);
    }

    /**
     * 回滚已上传的OSS文件
     *
     * @param keys          需要删除的OSS对象key列表
     * @param uploadService OSS上传服务
     */
    public static void rollbackOss(List<String> keys, UploadService uploadService) {
        // 遍历所有key并尝试删除
        for (String key : keys) {
            if (StringUtils.isNotEmpty(key)) {
                try {
                    uploadService.delete(key);
                } catch (Exception ignored) {
                    // 尽力回滚，忽略异常
                }
            }
        }
    }
}
