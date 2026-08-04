package com.xcz.member.customer.infrastructure.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xcz.commons.core.exception.ServiceException;
import com.xcz.member.customer.api.dto.response.shop.ShopDetailRes;
import com.xcz.member.customer.domain.dto.product.ProductBriefDTO;
import com.xcz.member.customer.domain.dto.product.ShopProductCatalogDTO;
import com.xcz.member.customer.domain.dto.shop.ShopBriefDTO;
import com.xcz.member.customer.domain.service.MobiShopProductCategoryService;
import com.xcz.member.customer.domain.service.MobiShopProductService;
import com.xcz.member.customer.domain.service.MobiShopService;
import com.xcz.member.customer.infrastructure.cache.ProductCache;
import com.xcz.member.customer.infrastructure.cache.ShopCache;
import com.xcz.member.customer.infrastructure.entity.MobiShop;
import com.xcz.member.customer.infrastructure.mapper.MobiShopMapper;
import com.xcz.commons.oss.service.UploadService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 店铺查询服务实现（读模型），提供店铺简要信息与详情聚合查询。
 */
@Service
public class MobiShopServiceImpl extends ServiceImpl<MobiShopMapper, MobiShop> implements MobiShopService {
    @Resource
    private MobiShopMapper mobiShopMapper;
    @Resource
    private MobiShopProductService mobiShopProductService;
    @Resource
    private MobiShopProductCategoryService mobiShopProductCategoryService;
    @Resource
    private UploadService uploadService;

    /**
     * 按店铺 ID 批量查询简要信息（直接读库，不走缓存）。
     *
     * @param shopIds 店铺 ID 列表
     * @return 店铺简要读模型列表
     */
    @Override
    public List<ShopBriefDTO> listBriefByIds(List<Long> shopIds) {
        if (shopIds == null || shopIds.isEmpty()) {
            return List.of();
        }
        return mobiShopMapper.selectByIds(shopIds).stream()
                .map(ShopBriefDTO::of)
                .toList();
    }

    /**
     * 获取店铺详情读模型：由店铺简要缓存与商品目录缓存运行时组装。
     *
     * @param shopId 店铺 ID
     * @return 店铺详情 VO（含商品列表、轮播图、地址等）
     */
    @Override
    public ShopDetailRes getShopDetail(Long shopId) {
        if (shopId == null) {
            throw new ServiceException("店铺ID不能为空");
        }
        ShopBriefDTO brief = ShopCache.getById(shopId, mobiShopMapper);
        if (brief == null) {
            throw new ServiceException("店铺不存在");
        }
        ShopProductCatalogDTO catalog = ProductCache.getCatalog(shopId, () -> mobiShopProductService.listProductsByShopId(shopId));
        return buildShopDetail(brief, catalog);
    }

    /**
     * 获取店铺详情读模型：由店铺简要缓存与商品目录缓存运行时组装。
     *
     * @param shopCode 店铺 ID
     * @return 店铺详情 VO（含商品列表、轮播图、地址等）
     */
    @Override
    public ShopDetailRes getShopDetail(String shopCode) {
        if (shopCode == null) {
            throw new ServiceException("店铺CODE不能为空");
        }
        ShopBriefDTO brief = ShopCache.getByShopCode(shopCode, mobiShopMapper);
        if (brief == null) {
            throw new ServiceException("店铺不存在");
        }
        Long shopId = brief.id();
        ShopProductCatalogDTO catalog = ProductCache.getCatalog(shopId, () -> mobiShopProductService.listProductsByShopId(shopId));
        return buildShopDetail(brief, catalog);
    }

    /**
     * 构建店铺详情响应对象
     *
     * @param brief   店铺简要信息
     * @param catalog 商品目录数据
     * @return 店铺详情响应对象
     */
    private ShopDetailRes buildShopDetail(ShopBriefDTO brief, ShopProductCatalogDTO catalog) {
        List<ShopDetailRes.Product> products = catalog.categories().stream()
                .flatMap(category -> category.products().stream())
                .map(MobiShopServiceImpl::toProductSummary)
                .toList();
        return ShopDetailRes.builder()
                .shopId(brief.id())
                .parentId(brief.parentId())
                .shopName(brief.shopName())
                .shopCode(brief.shopCode())
                .ratio(brief.ratio())
                .picture(resolvePictureUrls(brief.picture()))
                .phone(brief.phone())
                .address(toFullAddress(brief))
                .products(products)
                .build();
    }

    /**
     * 解析图片路径为完整 URL 列表
     *
     * @param picture 图片路径字符串，多个路径用逗号分隔
     * @return 完整的图片 URL 列表
     */
    private List<String> resolvePictureUrls(String picture) {
        if (picture == null || picture.isBlank()) {
            return List.of();
        }
        List<String> urls = new ArrayList<>();
        for (String key : picture.split(",")) {
            if (key == null || key.isBlank()) {
                continue;
            }
            urls.add(uploadService.getUrl(key.trim()));
        }
        return urls;
    }

    /**
     * 拼接完整地址字符串
     *
     * @param brief 店铺简要信息
     * @return 完整地址（省+市+区+详细地址）
     */
    private static String toFullAddress(ShopBriefDTO brief) {
        return String.join("",
                brief.province() == null ? "" : brief.province(),
                brief.city() == null ? "" : brief.city(),
                brief.district() == null ? "" : brief.district(),
                brief.address() == null ? "" : brief.address());
    }

    /**
     * 转换商品简要信息为详情响应中的商品对象
     *
     * @param product 商品简要信息 DTO
     * @return 店铺详情响应中的商品对象
     */
    private static ShopDetailRes.Product toProductSummary(ProductBriefDTO product) {
        return ShopDetailRes.Product.builder()
                .productId(product.productId())
                .productName(product.productName())
                .price(product.price())
                .build();
    }
}
