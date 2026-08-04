package com.xcz.member.customer.application.service.product;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xcz.commons.core.exception.ServiceException;
import com.xcz.member.customer.application.assemblers.ProductAssembler;
import com.xcz.member.customer.application.command.category.MutateCategoryCommand;
import com.xcz.member.customer.application.command.product.MutateProductCommand;
import com.xcz.member.customer.api.dto.request.product.ProductReq;
import com.xcz.member.customer.domain.enums.Constants;
import com.xcz.member.customer.domain.enums.ProductStatus;
import com.xcz.member.customer.domain.service.MobiShopProductCategoryService;
import com.xcz.member.customer.domain.service.MobiShopProductService;
import com.xcz.member.customer.domain.service.MobiShopUserService;
import com.xcz.member.customer.infrastructure.entity.MobiShopProduct;
import com.xcz.member.customer.infrastructure.entity.MobiShopProductCategory;
import com.xcz.member.customer.utils.CertFileUtils;
import com.xcz.member.customer.utils.ShopAccessUtils;
import com.xcz.commons.oss.service.UploadService;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商品写侧应用服务。
 */
@Service
public class ProductApplicationService {

    @Resource
    private MobiShopProductService mobiShopProductService;
    @Resource
    private MobiShopProductCategoryService mobiShopProductCategoryService;
    @Resource
    private MobiShopUserService mobiShopUserService;
    @Resource
    private UploadService uploadService;
    @Resource
    @Lazy
    private ProductApplicationService self;



    /**
     * 更新商品（multipart）：meta 须含 productId，image 为可选新图片。
     *
     * @param dto   商品信息
     * @param image 新图片（可选）
     * @return 上传后的图片地址；未更换图片时返回 null
     */
    public String updateProductFromMultipart(ProductReq dto, MultipartFile image) {
        if (dto == null || dto.getProductId() == null) {
            throw new ServiceException("商品ID不能为空", 400);
        }
        ShopAccessUtils.assertShopStaff(mobiShopUserService, dto.getShopId());
        MobiShopProduct existing = loadProduct(dto.getProductId());
        String newImageUrl = null;
        if (image != null && !image.isEmpty()) {
            newImageUrl = uploadProductImageFile(existing.getShopId(), image);
            dto.setImageUrl(newImageUrl);
        }
        if (dto.getCategoryId() != null) {
            List<Long> longs = ensureCategoryBelongsToShop(existing.getShopId(), Collections.singletonList(dto.getCategoryId()));
            if (longs != null) {
                throw new ServiceException("商品分类不存在", 400);
            }
            existing.setCategoryId(dto.getCategoryId());
        }
        if (dto.getProductName() != null) {
            existing.setProductName(dto.getProductName());
        }
        if (dto.getDescription() != null) {
            existing.setDescription(dto.getDescription());
        }
        String oldImageUrl = existing.getImageUrl();
        if (dto.getImageUrl() != null) {
            existing.setImageUrl(dto.getImageUrl());
        }
        if (dto.getPrice() != null) {
            existing.setPrice(dto.getPrice());
        }
        self.persistProductUpdate(existing);
        if (oldImageUrl != null && dto.getImageUrl() != null && !oldImageUrl.equals(dto.getImageUrl())) {
            uploadService.delete(oldImageUrl);
        }
        return newImageUrl;
    }

    @Transactional(rollbackFor = Exception.class)
    public void persistProductUpdate(MobiShopProduct existing) {
        mobiShopProductService.updateById(existing);
    }

    /**
     * 批量新增商品（multipart）：meta 为商品 JSON 列表，images 按 imageIndex 或顺序与 meta 对应。
     *
     * @param meta   商品信息列表
     * @param images 商品图片文件（可选，可少于 meta 条数）
     */
    @Transactional(rollbackFor = Exception.class)
    public List<Long> addProductFromMultipart(List<ProductReq> meta, MultipartFile[] images) {
        if (meta == null || meta.isEmpty()) {
            throw new ServiceException("请至少添加一个商品", 400);
        }
        if(images != null && images.length > 0) {
            bindUploadedImages(meta, images);
        }
        return addProduct(ProductAssembler.toCreateCommand(meta));
    }

    /**
     * 新增商品：校验店铺权限与分类归属。
     *
     * @param commands 新增商品命令
     */
    @Transactional(rollbackFor = Exception.class)
    public List<Long> addProduct(List<MutateProductCommand> commands) {
        List<Long> list = commands.stream().map(MutateProductCommand::shopId).distinct().toList();
        if(list.size()>1){
            throw new ServiceException("只能操作一个店铺", 400);
        }
        ShopAccessUtils.assertShopStaff(mobiShopUserService, list.getFirst());
        List<Long> categoryIds = commands.stream().map(MutateProductCommand::categoryId).distinct().toList();
        List<Long> missIds = ensureCategoryBelongsToShop(list.getFirst(), categoryIds);
        if(missIds != null ){
             return missIds;
        }
        commands.forEach(command ->{
            MobiShopProduct product = MobiShopProduct.builder()
                    .shopId(command.shopId())
                    .categoryId(command.categoryId())
                    .productName(command.productName())
                    .description(command.description())
                    .imageUrl(command.imageUrl())
                    .price(command.price())
                    .status(command.resolvedStatus())
                    .stock(command.resolvedStock())
                    .soldCount(0L)
                    .createTime(LocalDateTime.now())
                    .build();
            mobiShopProductService.save(product);
        });
        return null;
    }



    /**
     * 删除商品。
     *
     * @param productId 商品 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void removeProduct(Long productId) {
        MobiShopProduct product = loadProduct(productId);
        ShopAccessUtils.assertShopStaff(mobiShopUserService, product.getShopId());
        mobiShopProductService.removeById(productId);
    }

    /**
     * 维护商品库存：追加库存数量；-1 表示设为无限库存。
     *
     * @param productId   商品 ID
     * @param addQuantity 追加数量（正数追加、-1 设为无限）；不可为 0
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateStock(Long productId, Long addQuantity) {
        //检查是否有该商品，是否有更改权限
        MobiShopProduct product = loadProduct(productId);
        ShopAccessUtils.assertShopStaff(mobiShopUserService, product.getShopId());
        mobiShopProductService.ensureStockCached(productId);

        if (addQuantity != null && addQuantity != 0) {
            ProductAssembler.assertStock(addQuantity);
            mobiShopProductService.addStock(product, addQuantity);
        } else {
            throw new ServiceException("请提供 addQuantity", 400);
        }
    }

    /**
     * 用户主动切换商品售卖状态（出售中 / 下架；售完由库存自动判定）。
     *
     * @param productId 商品 ID
     * @param status    1-出售中 2-下架（售完状态 0 仅由库存自动设置）
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long productId, Integer status) {
        ProductAssembler.assertManualProductStatus(status);
        MobiShopProduct product = loadProduct(productId);
        ShopAccessUtils.assertShopStaff(mobiShopUserService, product.getShopId());
        product.setStatus(status);
        mobiShopProductService.updateById(product);
    }

    /**
     * 新增商品分类：未指定排序时自动追加到末尾。
     *
     * @param commands 新增分类命令
     */
    @Transactional(rollbackFor = Exception.class)
    public void addCategory(List<MutateCategoryCommand> commands) {
        List<MobiShopProductCategory> categories = new ArrayList<>();
        commands.forEach(command -> {
            ShopAccessUtils.assertShopStaff(mobiShopUserService, command.shopId());
            MobiShopProductCategory category = MobiShopProductCategory.builder()
                    .shopId(command.shopId())
                    .categoryName(command.categoryName())
                    .sortOrder(command.sortOrder() == null ? nextCategorySortOrder(command.shopId()) : command.sortOrder())
                    .createTime(LocalDateTime.now())
                    .build();
            categories.add(category);
        });
        if(!categories.isEmpty()){
            mobiShopProductCategoryService.saveBatch(categories);
        }
    }

    /**
     * 批量更新商品分类：支持部分字段更新。
     *
     * @param commands 更新分类命令列表
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateCategories(List<MutateCategoryCommand> commands) {
        if (commands == null || commands.isEmpty()) {
            return;
        }
        //获取数据库中的分类
        List<Long> categoryIds = commands.stream()
                .map(MutateCategoryCommand::categoryId)
                .distinct()
                .toList();
        List<MobiShopProductCategory> categories = mobiShopProductCategoryService.listByIds(categoryIds);
        if (categories.size() != categoryIds.size()) {
            throw new ServiceException("分类不存在");
        }
        //分类 ID -> 分类
        Map<Long, MobiShopProductCategory> categoryMap = categories.stream()
                .collect(Collectors.toMap(MobiShopProductCategory::getCategoryId, c -> c));
        Long shopId = categories.getFirst().getShopId();
        if (categories.stream().anyMatch(c -> !shopId.equals(c.getShopId()))) {
            throw new ServiceException("只能修改同店铺下的分类", 400);
        }
        ShopAccessUtils.assertShopStaff(mobiShopUserService, shopId);
        for (MutateCategoryCommand command : commands) {
            MobiShopProductCategory category = categoryMap.get(command.categoryId());
            if (command.categoryName() != null) {
                category.setCategoryName(command.categoryName());
            }
            if (command.sortOrder() != null) {
                category.setSortOrder(command.sortOrder());
            }
        }
        mobiShopProductCategoryService.updateBatchById(categories);
    }

    /**
     * 删除商品分类：分类下仍有商品时拒绝删除。
     *
     * @param categoryId 分类 ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void removeCategory(Long categoryId) {
        MobiShopProductCategory category = loadCategory(categoryId);
        ShopAccessUtils.assertShopStaff(mobiShopUserService, category.getShopId());
        long count = mobiShopProductService.count(new LambdaQueryWrapper<MobiShopProduct>()
                .eq(MobiShopProduct::getCategoryId, categoryId));
        if (count > 0) {
            throw new ServiceException("分类下仍有商品，请先移除或转移商品");
        }
        mobiShopProductCategoryService.removeById(categoryId);
    }


    /**
     * 按 ID 加载商品，不存在时抛出业务异常。
     *
     * @param productId 商品 ID
     * @return 商品实体
     */
    private MobiShopProduct loadProduct(Long productId) {
        MobiShopProduct product = mobiShopProductService.getById(productId);
        if (product == null) {
            throw new ServiceException("商品不存在");
        }
        return product;
    }

    /**
     * 按 ID 加载分类，不存在时抛出业务异常。
     *
     * @param categoryId 分类 ID
     * @return 分类实体
     */
    private MobiShopProductCategory loadCategory(Long categoryId) {
        MobiShopProductCategory category = mobiShopProductCategoryService.getById(categoryId);
        if (category == null) {
            throw new ServiceException("分类不存在");
        }
        return category;
    }

    /**
     * 校验分类是否属于指定店铺。
     *
     * @param shopId     店铺 ID
     * @param categoryIds 分类 ID（为 null 时跳过校验）
     */
    private List<Long> ensureCategoryBelongsToShop(Long shopId, List<Long> categoryIds) {
        if (categoryIds.isEmpty()) {
            return null;
        }
        List<MobiShopProductCategory> list = mobiShopProductCategoryService.list(
                new LambdaQueryWrapper<MobiShopProductCategory>()
                        .eq(MobiShopProductCategory::getShopId, shopId)
                        .in(MobiShopProductCategory::getCategoryId, categoryIds)
        );
        if (list.size() != categoryIds.size()) {
            return list.stream()
                    .map(MobiShopProductCategory::getCategoryId)
                    .filter(categoryId -> !categoryIds.contains(categoryId))
                    .toList();
        }
        return null;
    }

    /**
     * 计算新分类的默认排序值（当前分类数量）。
     *
     * @param shopId 店铺 ID
     * @return 排序值
     */
    private int nextCategorySortOrder(Long shopId) {
        return mobiShopProductCategoryService.listByShopId(shopId).size();
    }

    /**
     * 将 multipart 图片上传并写入 meta 的 imageUrl。
     */
    private void bindUploadedImages(List<ProductReq> meta, MultipartFile[] images) {
        if (images == null || images.length == 0) {
            return;
        }
        List<Long> shopIds = meta.stream().map(ProductReq::getShopId).distinct().toList();
        if (shopIds.size() != 1) {
            throw new ServiceException("只能操作一个店铺", 400);
        }
        Long shopId = shopIds.getFirst();
        ShopAccessUtils.assertShopStaff(mobiShopUserService, shopId);
        int metaSize = meta.size();
        for (int i = 0; i < metaSize; i++) {
            MultipartFile file = pickImageFile(meta.get(i), images, i, metaSize);
            if (file == null || file.isEmpty()) {
                continue;
            }
            meta.get(i).setImageUrl(uploadProductImageFile(shopId, file));
        }
    }

    private MultipartFile pickImageFile(ProductReq req, MultipartFile[] images, int metaIndex, int metaSize) {
        if (req.getImageIndex() != null) {
            int idx = req.getImageIndex();
            return idx >= 0 && idx < images.length ? images[idx] : null;
        }
        if (images.length == metaSize) {
            return images[metaIndex];
        }
        if (metaSize == 1) {
            return images[0];
        }
        return null;
    }

    /**
     * 上传商品图片并返回图片 URL。
     */
    private String uploadProductImageFile(Long shopId, MultipartFile file) {
        String baseDir = Constants.PRODUCT_IMAGE_DIRECTORY + shopId;
        try {
            String objectKey = CertFileUtils.uploadToDir(file, baseDir, uploadService);
            return uploadService.getEnteralUrl(objectKey);
        } catch (IOException e) {
            throw new ServiceException("图片上传失败：" + e.getMessage(), 400);
        }
    }
}
