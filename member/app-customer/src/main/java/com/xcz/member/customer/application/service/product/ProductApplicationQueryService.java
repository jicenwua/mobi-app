package com.xcz.member.customer.application.service.product;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xcz.member.customer.application.assemblers.ProductAssembler;
import com.xcz.member.customer.application.command.product.QueryProductCommand;
import com.xcz.member.customer.api.dto.response.product.ProductCategoryRes;
import com.xcz.member.customer.api.dto.response.product.ProductRes;
import com.xcz.member.customer.domain.dto.product.ProductBriefDTO;
import com.xcz.member.customer.domain.dto.product.ProductCategoryBriefDTO;
import com.xcz.member.customer.domain.dto.product.ShopProductCatalogDTO;
import com.xcz.member.customer.domain.enums.ProductStatus;
import com.xcz.member.customer.domain.service.MobiShopProductCategoryService;
import com.xcz.member.customer.domain.service.MobiShopProductService;
import com.xcz.member.customer.domain.vo.AdminProductRowVO;
import com.xcz.member.customer.infrastructure.cache.ProductCache;
import com.xcz.member.customer.infrastructure.cache.ProductStockCache;
import com.xcz.member.customer.infrastructure.mapper.MobiShopProductMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * 商品查询应用服务：负责读侧编排与缓存旁路。
 */
@Service
public class ProductApplicationQueryService {

    @Resource
    private MobiShopProductService mobiShopProductService;
    @Resource
    private MobiShopProductCategoryService mobiShopProductCategoryService;
    @Resource
    private MobiShopProductMapper mobiShopProductMapper;

    /**
     * C 端：按店铺一次性获取全部分类及商品（推荐用于菜单页，支持分类锚点跳转）。
     *
     * @param shopId 店铺 ID
     * @return 按分类分组的商品目录
     */
    public List<ProductCategoryRes> getCatalog(Long shopId) {
        ShopProductCatalogDTO catalog = ProductCache.getCatalog(shopId, () -> mobiShopProductService.listProductsByShopId(shopId));
        ShopProductCatalogDTO enriched = enrichRealtimeStock(catalog);
        return ProductAssembler.toCatalogRes(enriched);
    }

    /**
     * 查询店铺商品分类列表（不含商品明细）。
     *
     * @param shopId 店铺 ID
     * @return 分类列表
     */
    public List<ProductCategoryRes> listCategories(Long shopId) {
        return mobiShopProductCategoryService.listByShopId(shopId).stream()
                .map(category -> ProductAssembler.toCategoryRes(
                        ProductCategoryBriefDTO.of(category, List.of())))
                .toList();
    }


    /** 管理后台 Feign：分页查询商品（店铺可选，默认全平台按商品 ID 倒序）。 */
    public Page<ProductRes> pageForFeign(QueryProductCommand command) {
        int pageNum = Math.max(command.pageNum(), 1);
        int pageSize = command.pageSize() < 1 ? 10 : command.pageSize();
        Page<AdminProductRowVO> page = new Page<>(pageNum, pageSize);
        Page<AdminProductRowVO> result = mobiShopProductMapper.selectAdminProductPage(page, command);
        Page<ProductRes> out = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        out.setRecords(result.getRecords().stream()
                .map(ProductAssembler::adminRowToRes)
                .toList());
        return out;
    }

    /**
     * 为目录中所有商品填充 Redis 实时库存与已售数量。
     *
     * @param catalog 商品目录读模型
     * @return 填充实时库存后的目录
     */
    private ShopProductCatalogDTO enrichRealtimeStock(ShopProductCatalogDTO catalog) {
        if (catalog == null || catalog.categories() == null) {
            return catalog;
        }
        List<Long> productIds = catalog.categories().stream()
                .flatMap(category -> category.products() == null ? java.util.stream.Stream.<ProductBriefDTO>empty() : category.products().stream())
                .map(ProductBriefDTO::productId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();
        mobiShopProductService.ensureStockCachedBatch(productIds);
        List<ProductCategoryBriefDTO> categories = catalog.categories().stream()
                .map(category -> {
                    List<ProductBriefDTO> products = category.products() == null
                            ? List.of() : category.products().stream()
                            .map(this::enrichRealtimeStock)
                            .sorted(Comparator.comparing(ProductBriefDTO::createTime, Comparator.nullsLast(Comparator.reverseOrder())))
                            .toList();
                    return new ProductCategoryBriefDTO(
                            category.categoryId(),
                            category.shopId(),
                            category.categoryName(),
                            category.sortOrder(),
                            category.createTime(),
                            products
                    );
                })
                .toList();
        return new ShopProductCatalogDTO(catalog.shopId(), categories);
    }

    /**
     * 为单个商品填充 Redis 实时库存与已售数量；有限库存售罄时覆盖状态。
     */
    private ProductBriefDTO enrichRealtimeStock(ProductBriefDTO brief) {
        if (brief == null) {
            return null;
        }
        long available = ProductStockCache.getAvailableStock(brief.productId());
        long sold = ProductStockCache.getPendingSold(brief.productId());
        int status = brief.status();
        if (available == 0 && status != ProductStatus.OFF_SHELF.getCode()) {
            status = ProductStatus.SOLD_OUT.getCode();
        }
        return new ProductBriefDTO(
                brief.productId(),
                brief.shopId(),
                brief.categoryId(),
                brief.productName(),
                brief.description(),
                brief.imageUrl(),
                brief.price(),
                status,
                available,
                sold,
                brief.createTime()
        );
    }
}
