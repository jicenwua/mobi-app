package com.xcz.member.customer.application.service.shop;

import com.xcz.commons.core.exception.ServiceException;
import com.xcz.commons.core.utils.StringUtils;
import com.xcz.member.customer.api.dto.request.shop.ShopCreatePayloadReq;
import com.xcz.member.customer.api.dto.response.shop.StaffAddRes;
import com.xcz.member.customer.application.command.shop.AuditShopCommand;
import com.xcz.member.customer.application.command.shop.CreateShopCommand;
import com.xcz.member.customer.domain.service.MobiShopService;
import com.xcz.member.customer.domain.service.MobiShopUserService;
import com.xcz.member.customer.domain.service.MobiUserService;
import com.xcz.member.customer.domain.shop.exception.ShopDomainException;
import com.xcz.member.customer.domain.shop.model.Shop;
import com.xcz.member.customer.domain.shop.model.ShopApplicationInfo;
import com.xcz.member.customer.domain.shop.model.ShopCertificates;
import com.xcz.member.customer.domain.shop.repository.ShopRepository;
import com.xcz.member.customer.domain.shop.service.ShopCodeGenerator;
import com.xcz.member.customer.infrastructure.adapter.ocr.IdCardOcrService;
import com.xcz.member.customer.infrastructure.adapter.ocr.vo.IdCardOcrVO;
import com.xcz.member.customer.infrastructure.entity.MobiShop;
import com.xcz.member.customer.infrastructure.entity.MobiUser;
import com.xcz.member.customer.utils.CertFileUtils;
import com.xcz.member.customer.utils.IdCardUtils;
import com.xcz.member.customer.utils.ShopAccessUtils;
import com.xcz.member.customer.utils.StaffInviteUtils;
import com.xcz.commons.oss.service.UploadService;
import com.xcz.commons.security.utils.SecurityUtils;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * 店铺写侧应用服务：负责开店申请、审核等用例编排。
 */
@Service
public class ShopApplicationService {

    @Resource
    private ShopRepository shopRepository;
    @Resource
    private MobiShopUserService mobiShopUserService;
    @Resource
    private MobiShopService mobiShopService;
    @Resource
    private MobiUserService mobiUserService;
    @Resource
    private IdCardOcrService idCardOcrService;
    @Resource
    private UploadService uploadService;

    /**
     * 提交开店申请：完成证照识别、文件上传、聚合根创建与店长关系绑定。
     *
     * @param command        开店申请命令
     * @param managerUserId  指定店长用户 ID（管理后台必传；小程序端传 null 表示当前登录用户）
     * @return 新店铺 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long addShop(CreateShopCommand command, Long managerUserId) {
        Long targetManagerId = managerUserId != null ? managerUserId : SecurityUtils.getUserId();
        if (managerUserId != null) {
            MobiUser manager = mobiUserService.getById(managerUserId);
            if (manager == null) {
                throw new ServiceException("店长用户不存在，请先完成小程序注册", 400);
            }
        }
        Long shopId = persistShopApplication(command);
        try {
            mobiShopUserService.setShopManager(shopId, targetManagerId);
        } catch (Exception e) {
            throw new ServiceException("添加店长关系失败：" + e.getMessage());
        }
        return shopId;
    }

    /**
     * 小程序端提交开店申请（店长为当前登录用户）。
     */
    @Transactional(rollbackFor = Exception.class)
    public Long addShop(CreateShopCommand command) {
        return addShop(command, null);
    }

    private Long persistShopApplication(CreateShopCommand command) {
        ShopCreatePayloadReq meta = command.meta();
        IdCardOcrVO ocr;
        try {
            ocr = idCardOcrService.recognize(command.idCardFront());
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("身份证识别失败：" + e.getMessage(), 400);
        }
        if (!IdCardUtils.idCardMatchesForm(ocr, meta.getLegalPerson(), meta.getIdCardNo())) {
            throw new ServiceException("身份证信息不一致", 400);
        }
        String basePath = CertFileUtils.buildCertFolder(meta.getShopName(), meta.getLegalPerson());
        List<String> uploadedKeys = new ArrayList<>();
        try {
            String k1 = CertFileUtils.uploadToDir(command.idCardFront(), basePath, uploadService);
            uploadedKeys.add(k1);
            String k2 = CertFileUtils.uploadToDir(command.idCardBack(), basePath, uploadService);
            uploadedKeys.add(k2);
            String k3 = CertFileUtils.uploadToDir(command.businessLicensePic(), basePath, uploadService);
            uploadedKeys.add(k3);
            String k4 = CertFileUtils.uploadToDir(command.shopExterior(), basePath, uploadService);
            uploadedKeys.add(k4);
            String k5 = CertFileUtils.uploadToDir(command.shopInterior(), basePath, uploadService);
            uploadedKeys.add(k5);
            List<String> carouselKeys = new ArrayList<>();
            for (MultipartFile carouselImage : command.carouselImages()) {
                String carouselKey = CertFileUtils.uploadToDir(carouselImage, basePath, uploadService);
                carouselKeys.add(uploadService.getEnteralUrl(carouselKey));
                uploadedKeys.add(carouselKey);
            }

            Long parentId = resolveParentId(meta.getParentShopCode());
            ShopApplicationInfo applicationInfo = ShopApplicationInfo.of(meta, parentId);
            ShopCertificates certificates = new ShopCertificates(
                    k1, k2, k3, k4, k5, String.join(",", carouselKeys)
            );
            Shop shop = Shop.submitApplication(
                    applicationInfo,
                    certificates,
                    ShopCodeGenerator.nextCode(shopRepository)
            );
            Shop saved = shopRepository.save(shop);
            return saved.getId();
        } catch (ShopDomainException e) {
            CertFileUtils.rollbackOss(uploadedKeys, uploadService);
            throw new ServiceException(e.getMessage(), 400);
        } catch (ServiceException e) {
            CertFileUtils.rollbackOss(uploadedKeys, uploadService);
            throw e;
        } catch (Exception e) {
            CertFileUtils.rollbackOss(uploadedKeys, uploadService);
            throw new ServiceException(e.getMessage(), 500);
        }
    }

    /**
     * 用户加入店铺：无关系时设为顾客；已有关系时仅更新进入时间。
     *
     * @param shopId 店铺 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void enterShop(Long shopId) {
        if (shopId == null) {
            throw new ServiceException("店铺 ID 不能为空", 400);
        }
        MobiShop shop = mobiShopService.getById(shopId);
        if (shop == null) {
            throw new ServiceException("店铺不存在", 400);
        }
        Long userId = SecurityUtils.getUserId();
        mobiShopUserService.enterShopAsCustomer(shopId, userId);
    }

    /**
     * 店长扫码用户店员邀请码，将其添加为本店店员。
     *
     * @param shopId 店铺 ID
     * @param token  店员邀请 token
     * @return 被添加用户信息
     */
    @Transactional(rollbackFor = Exception.class)
    public StaffAddRes addClerkByInviteToken(Long shopId, String token) {
        if (shopId == null) {
            throw new ServiceException("店铺 ID 不能为空", 400);
        }
        if (token == null || token.isBlank()) {
            throw new ServiceException("邀请码无效", 400);
        }
        MobiShop shop = mobiShopService.getById(shopId);
        if (shop == null) {
            throw new ServiceException("店铺不存在", 400);
        }
        ShopAccessUtils.assertShopManager(mobiShopUserService, shopId);
        Long userId = StaffInviteUtils.getUserId(token.trim());
        if (userId == null) {
            throw new ServiceException("邀请码已过期或无效");
        }
        Long operatorId = SecurityUtils.getUserId();
        if (operatorId.equals(userId)) {
            throw new ServiceException("不能添加自己为店员");
        }
        MobiUser user = mobiUserService.getById(userId);
        if (user == null) {
            throw new ServiceException("用户不存在");
        }
        mobiShopUserService.assignShopClerk(shopId, userId);
        StaffInviteUtils.invalidate(token.trim());
        return StaffAddRes.builder()
                .userId(userId)
                .nickname(user.getNickname())
                .build();
    }

    /**
     * 店长移除店员（降为顾客，清除店员权限）。
     */
    @Transactional(rollbackFor = Exception.class)
    public void removeClerk(Long shopId, Long userId) {
        if (shopId == null) {
            throw new ServiceException("店铺 ID 不能为空", 400);
        }
        if (userId == null) {
            throw new ServiceException("用户 ID 不能为空", 400);
        }
        ShopAccessUtils.assertShopManager(mobiShopUserService, shopId);
        Long operatorId = SecurityUtils.getUserId();
        if (operatorId.equals(userId)) {
            throw new ServiceException("不能移除自己");
        }
        mobiShopUserService.removeShopClerk(shopId, userId);
    }

    /**
     * 审核店铺消息
     * @param command       审核命令
     * @param auditUserId   审核id
     */
    @Transactional(rollbackFor = Exception.class)
    public void auditShop(AuditShopCommand command, Long auditUserId) {
        Shop shop = shopRepository.findById(command.shopId())
                .orElseThrow(() -> new ServiceException("店铺不存在", 400));
        try {
            //根据命令设置聚合根
            if (command.auditStatus() == 1) {
                shop.approve(auditUserId);
            } else {
                shop.reject(auditUserId, command.auditReason());
            }
            //保存审核信息
            shopRepository.save(shop);
        } catch (ShopDomainException e) {
            throw new ServiceException(e.getMessage(), 400);
        }
    }

    /**
     * 判断上级店铺
     * @param parentShopCode    店铺code
     * @return  店铺id
     */
    private Long resolveParentId(String parentShopCode) {
        if (StringUtils.isEmpty(parentShopCode)) {
            return 0L;
        }
        Shop parent = shopRepository.findByShopCode(StringUtils.trim(parentShopCode))
                .orElseThrow(() -> new ServiceException("上级店铺代码不存在", 400));
        try {
            parent.assertApprovedHeadShop();
        } catch (ShopDomainException e) {
            throw new ServiceException(e.getMessage(), 400);
        }
        return parent.getId();
    }

    public void enterShopTime(Long shopId) {
        mobiShopUserService.recordShopEnter(shopId, SecurityUtils.getUserId());
    }
}
