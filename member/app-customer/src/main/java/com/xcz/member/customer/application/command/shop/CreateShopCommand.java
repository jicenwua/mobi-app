package com.xcz.member.customer.application.command.shop;

import com.xcz.commons.core.exception.ServiceException;
import com.xcz.commons.core.utils.StringUtils;
import com.xcz.member.customer.api.dto.request.shop.ShopCreatePayloadReq;
import org.springframework.web.multipart.MultipartFile;


/**
 * 开店申请命令，包含表单元数据与证照图片。
 */
public record CreateShopCommand(
        ShopCreatePayloadReq meta,
        MultipartFile idCardFront,
        MultipartFile idCardBack,
        MultipartFile businessLicensePic,
        MultipartFile shopExterior,
        MultipartFile shopInterior,
        MultipartFile[] carouselImages
) {
    public CreateShopCommand {
        // 验证元数据不为空
        if (meta == null) {
            throw new ServiceException("表单数据不能为空", 400);
        }
        // 验证店铺名称不为空
        if (StringUtils.isEmpty(meta.getShopName())) {
            throw new ServiceException("店铺名称不能为空", 400);
        }
        if (StringUtils.isEmpty(meta.getPhone())) {
            throw new ServiceException("联系电话不能为空", 400);
        }
        // 验证法人姓名不为空
        if (StringUtils.isEmpty(meta.getLegalPerson())) {
            throw new ServiceException("法人姓名不能为空", 400);
        }
        // 验证身份证号不为空
        if (StringUtils.isEmpty(meta.getIdCardNo())) {
            throw new ServiceException("身份证号不能为空", 400);
        }
        // 验证行业类目已选择
        if (meta.getCategoryId() == null) {
            throw new ServiceException("请选择行业类目", 400);
        }
        // 验证充值积分比率：正整数，创建后不可修改
        Integer ratio = meta.getRatio();
        if (ratio == null) {
            throw new ServiceException("请填写充值积分比率", 400);
        }
        if (ratio < 1 || ratio > 100000) {
            throw new ServiceException("充值积分比率须为 1～100000 的正整数", 400);
        }
        // 验证统一社会信用代码不为空
        if (StringUtils.isEmpty(meta.getBusinessLicenseNo())) {
            throw new ServiceException("统一社会信用代码不能为空", 400);
        }
        // 验证详细地址不为空
        if (StringUtils.isEmpty(meta.getAddress())) {
            throw new ServiceException("详细地址不能为空", 400);
        }
        // 验证必需的图片文件
        if (isEmptyFile(idCardFront) || isEmptyFile(idCardBack) || isEmptyFile(businessLicensePic)
                || isEmptyFile(shopExterior) || isEmptyFile(shopInterior)) {
            throw new ServiceException("请完整上传全部图片", 400);
        }
        // 验证轮播图：至少1个，最多3个
        if (carouselImages == null || carouselImages.length == 0) {
            throw new ServiceException("请至少上传1张轮播图", 400);
        }
        if (carouselImages.length > 3) {
            throw new ServiceException("最多只能上传3张轮播图", 400);
        }
        for (MultipartFile carouselImage : carouselImages) {
            if (isEmptyFile(carouselImage)) {
                throw new ServiceException("轮播图不能为空", 400);
            }
        }
    }

    private boolean isEmptyFile(MultipartFile f) {
        return f == null || f.isEmpty();
    }
}
