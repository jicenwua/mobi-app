package com.xcz.member.customer.infrastructure.cache;

import com.xcz.member.customer.domain.dto.product.ProductBriefDTO;
import com.xcz.member.customer.domain.dto.product.ProductCategoryBriefDTO;
import com.xcz.member.customer.domain.dto.product.ShopProductCatalogDTO;
import com.xcz.member.customer.domain.enums.Constants;
import com.xcz.member.customer.infrastructure.cache.dto.ProductCatalogCacheDTO;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * 店铺商品目录 Redis 缓存（分类 + 商品静态信息，不含实时库存）。
 * <p>
 * 写侧失效由 {@link com.xcz.member.customer.infrastructure.service.MobiShopProductServiceImpl}、
 * {@link com.xcz.member.customer.infrastructure.service.MobiShopProductCategoryServiceImpl} 自动维护。
 */
@Slf4j
@UtilityClass
public class ProductCache {

    private static final RedissonClient REDISSON = Constants.CACHE;
    private static final String CATALOG_KEY = "product:catalog:";
    private static final Duration CATALOG_TTL = Duration.ofHours(1);
    /** 空目录短 TTL，防止恶意 shopId 穿透数据库 */
    private static final Duration EMPTY_CATALOG_TTL = Duration.ofMinutes(5);

    /**
     * 读取店铺商品目录，未命中时通过 loader 回源并回填。
     *
     * @param shopId 店铺 ID
     * @param loader 回源加载器（返回分类列表）
     * @return 商品目录读模型
     */
    public ShopProductCatalogDTO getCatalog(Long shopId, Supplier<List<ProductCategoryBriefDTO>> loader) {
        if (shopId == null) {
            return new ShopProductCatalogDTO(null, List.of());
        }
        RBucket<ProductCatalogCacheDTO> bucket = REDISSON.getBucket(CATALOG_KEY + shopId);
        ProductCatalogCacheDTO cached = readCatalogCache(bucket, shopId);
        //如果缓存中有数据则直接返回
        if (cached != null && cached.getCategories() != null) {
            return new ShopProductCatalogDTO(shopId, ProductCategoryBriefDTO.toDtoList(cached.getCategories()));
        }

        //否则从数据库中获取数据并缓存
        List<ProductCategoryBriefDTO> loaded = loader.get();
        List<ProductCategoryBriefDTO> categories = loaded == null ? List.of() : loaded;
        // 非空目录缓存 1h；空目录缓存 5min 防穿透
        Duration ttl = categories.isEmpty() ? EMPTY_CATALOG_TTL : CATALOG_TTL;
        bucket.set(ProductCatalogCacheDTO.from(shopId, categories), ttl);
        //如果有商品，则缓存商品的库存
        if (!categories.isEmpty()) {
            initProductStock(categories);
        }
        return new ShopProductCatalogDTO(shopId, categories);
    }

    /**
     * 删除店铺商品目录缓存。
     *
     * @param shopId 店铺 ID
     */
    public void evict(Long shopId) {
        if (shopId == null) {
            return;
        }
        REDISSON.getBucket(CATALOG_KEY + shopId).delete();
    }

    /**
     * 读取缓存反序列化失败时删除脏 key。
     */
    private static ProductCatalogCacheDTO readCatalogCache(RBucket<ProductCatalogCacheDTO> bucket, Long shopId) {
        try {
            return bucket.get();
        } catch (Exception ex) {
            log.warn("商品目录缓存反序列化失败，将删除并回源加载。shopId={}, key={}{}", shopId, CATALOG_KEY, shopId, ex);
            bucket.delete();
            return null;
        }
    }

    /**
     * 为目录中的商品初始化 Redis 库存（仅当 stock key 不存在时写入，不覆盖热数据）。
     */
    private static void initProductStock(List<ProductCategoryBriefDTO> categories) {
        List<ProductBriefDTO> products = categories.stream()
                .map(ProductCategoryBriefDTO::products)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .toList();
        products.forEach(it -> ProductStockCache.init(it.productId(), it.stock(), it.soldCount()));
    }
}
