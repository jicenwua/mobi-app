package com.xcz.member.customer.application.assemblers;

import com.xcz.commons.core.exception.ServiceException;
import com.xcz.commons.core.utils.StringUtils;
import com.xcz.member.customer.api.dto.request.product.ProductCategoryReq;
import com.xcz.member.customer.api.dto.request.product.ProductReq;
import com.xcz.member.customer.api.dto.response.product.ProductCategoryRes;
import com.xcz.member.customer.api.dto.response.product.ProductRes;
import com.xcz.member.customer.application.command.category.MutateCategoryCommand;
import com.xcz.member.customer.application.command.product.DeductStockCommand;
import com.xcz.member.customer.application.command.product.MutateProductCommand;
import com.xcz.member.customer.application.command.product.QueryProductCommand;
import com.xcz.member.customer.domain.dto.product.ProductBriefDTO;
import com.xcz.member.customer.domain.dto.product.ProductCategoryBriefDTO;
import com.xcz.member.customer.domain.dto.product.ShopProductCatalogDTO;
import com.xcz.member.customer.domain.vo.AdminProductRowVO;
import com.xcz.member.feign.dto.product.MobiProductFeign;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 商品相关对象组装器：Controller Req ↔ Command ↔ 领域读模型 ↔ API Res。
 * 写操作按场景校验字段后组装为 {@link MutateProductCommand} 或 {@link MutateCategoryCommand}。
 */
public final class ProductAssembler {

    private ProductAssembler() {
    }

    /**
     * 将商品查询请求转换为应用层查询命令。
     *
     * @param dto 商品查询参数
     * @return 查询命令
     */
    public static QueryProductCommand toQueryCommand(ProductReq dto) {
        if (dto.getShopId() == null) {
            throw new ServiceException("请选择店铺", 400);
        }
        int pageNum = dto.getPageNum() == null || dto.getPageNum() < 1 ? 1 : dto.getPageNum();
        int pageSize = dto.getPageSize() == null || dto.getPageSize() < 1 ? 10 : dto.getPageSize();
        if (dto.getStatus() != null && dto.getStatus() != 0 && dto.getStatus() != 1 && dto.getStatus() != 2) {
            throw new ServiceException("商品状态无效", 400);
        }
        return new QueryProductCommand(
                dto.getShopId(),
                dto.getCategoryId(),
                dto.getProductName(),
                dto.getStatus(),
                pageNum,
                pageSize
        );
    }

    /**
     * 将商品请求转换为新增商品命令（校验必填字段）。
     *
     * @param dtos 商品信息
     * @return 写操作命令
     */
    public static List<MutateProductCommand> toCreateCommand(List<ProductReq> dtos) {
        List<MutateProductCommand> commands = new ArrayList<>();
        dtos.forEach(dto ->{
            assertShopId(dto.getShopId());
            assertProductName(dto.getProductName());
            assertPrice(dto.getPrice(), true);
            assertStock(dto.getStock());
            assertStatus(dto.getStatus());
            MutateProductCommand mutateProductCommand = new MutateProductCommand(
                    null,
                    dto.getShopId(),
                    dto.getCategoryId(),
                    dto.getProductName(),
                    dto.getDescription(),
                    dto.getImageUrl(),
                    dto.getPrice(),
                    dto.getStatus(),
                    dto.getStock()
            );
            commands.add(mutateProductCommand);
        });
        return commands;

    }

    /**
     * 将商品请求转换为更新商品命令（支持部分字段更新）。
     *
     * @param dto 商品信息（须含 productId）
     * @return 写操作命令
     */
    public static MutateProductCommand toUpdateCommand(ProductReq dto) {
        assertProductId(dto.getProductId());
        if (dto.getStock() != null) {
            throw new ServiceException("不支持通过更新接口修改库存", 400);
        }
        if (dto.getProductName() != null && StringUtils.isEmpty(dto.getProductName())) {
            throw new ServiceException("商品名称不能为空", 400);
        }
        assertPrice(dto.getPrice(), false);
        if (dto.getStatus() != null) {
            throw new ServiceException("请通过状态接口修改售卖状态", 400);
        }
        if (!hasAnyProductUpdate(dto)) {
            throw new ServiceException("请至少提供一个待更新字段", 400);
        }
        return new MutateProductCommand(
                dto.getProductId(),
                null,
                dto.getCategoryId(),
                dto.getProductName(),
                dto.getDescription(),
                dto.getImageUrl(),
                dto.getPrice(),
                null,
                null
        );
    }

    /**
     * 将分类请求转换为新增分类命令。
     *
     * @param dtos 分类信息
     * @return 写操作命令
     */
    public static List<MutateCategoryCommand> toCreateCategoryCommand(List<ProductCategoryReq> dtos) {
        List<MutateCategoryCommand> commands = new ArrayList<>();
        long count = dtos.stream().map(ProductCategoryReq::getShopId).distinct().count();
        if(count != 1){
            throw new ServiceException("只能修改同店铺下的分类", 400);
        }
        dtos.forEach(dto -> {
            assertShopId(dto.getShopId());
            assertCategoryName(dto.getCategoryName());
            MutateCategoryCommand mutateCategoryCommand = new MutateCategoryCommand(null, dto.getShopId(), dto.getCategoryName(), dto.getSortOrder());
            commands.add(mutateCategoryCommand);
        });
        return  commands;
    }

    /**
     * 将分类请求列表转换为批量更新分类命令。
     *
     * @param dtos 分类信息（每项须含 categoryId）
     * @return 写操作命令列表
     */
    public static List<MutateCategoryCommand> toUpdateCategoryCommands(List<ProductCategoryReq> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return List.of();
        }
        List<MutateCategoryCommand> commands = new ArrayList<>();
        for (ProductCategoryReq dto : dtos) {
            assertCategoryId(dto.getCategoryId());
            if (dto.getCategoryName() != null && StringUtils.isEmpty(dto.getCategoryName())) {
                throw new ServiceException("分类名称不能为空", 400);
            }
            if (dto.getCategoryName() == null && dto.getSortOrder() == null) {
                throw new ServiceException("请至少提供一个待更新字段", 400);
            }
            commands.add(new MutateCategoryCommand(
                    dto.getCategoryId(), null, dto.getCategoryName(), dto.getSortOrder()));
        }
        return commands;
    }

    /**
     * 构建扣减库存命令（内部下单场景）。
     *
     * @param productId 商品 ID
     * @param quantity  扣减数量
     * @return 扣减库存命令
     */
    public static DeductStockCommand toDeductStockCommand(Long productId, int quantity) {
        if (productId == null) {
            throw new ServiceException("商品ID不能为空", 400);
        }
        if (quantity <= 0) {
            throw new ServiceException("扣减数量无效", 400);
        }
        return new DeductStockCommand(productId, quantity);
    }

    /**
     * 将商品简要读模型转换为响应对象。
     *
     * @param dto 商品简要读模型
     * @return 商品响应；读模型为 null 时返回 null
     */
    public static ProductRes toRes(ProductBriefDTO dto) {
        if (dto == null) {
            return null;
        }
        return ProductRes.builder()
                .productId(dto.productId())
                .shopId(dto.shopId())
                .categoryId(dto.categoryId())
                .productName(dto.productName())
                .description(dto.description())
                .imageUrl(dto.imageUrl())
                .price(dto.price())
                .status(dto.status())
                .stock(dto.stock())
                .soldCount(dto.soldCount())
                .createTime(dto.createTime())
                .build();
    }

    /** 管理后台联表查询行 → 商品响应（使用库内库存/销量，避免列表逐条读 Redis）。 */
    public static ProductRes adminRowToRes(AdminProductRowVO row) {
        if (row == null) {
            return null;
        }
        return ProductRes.builder()
                .productId(row.getProductId())
                .shopId(row.getShopId())
                .shopName(row.getShopName())
                .categoryId(row.getCategoryId())
                .productName(row.getProductName())
                .description(row.getDescription())
                .imageUrl(row.getImageUrl())
                .price(row.getPrice())
                .status(row.getStatus())
                .stock(row.getStock())
                .soldCount(row.getSoldCount())
                .createTime(row.getCreateTime())
                .build();
    }

    /**
     * 将分类简要读模型转换为响应对象（含下属商品列表）。
     *
     * @param dto 分类简要读模型
     * @return 分类响应；读模型为 null 时返回 null
     */
    public static ProductCategoryRes toCategoryRes(ProductCategoryBriefDTO dto) {
        if (dto == null) {
            return null;
        }
        List<ProductRes> products = dto.products() == null
                ? List.of()
                : dto.products().stream().map(ProductAssembler::toRes).toList();
        return ProductCategoryRes.builder()
                .categoryId(dto.categoryId())
                .shopId(dto.shopId())
                .categoryName(dto.categoryName())
                .sortOrder(dto.sortOrder())
                .createTime(dto.createTime())
                .products(products)
                .build();
    }

    /**
     * 将店铺商品目录读模型转换为分类响应列表。
     *
     * @param dto 商品目录读模型
     * @return 分类及商品列表；读模型为 null 时返回空列表
     */
    public static List<ProductCategoryRes> toCatalogRes(ShopProductCatalogDTO dto) {
        if (dto == null || dto.categories() == null) {
            return List.of();
        }
        return dto.categories().stream().map(ProductAssembler::toCategoryRes).toList();
    }

    /**
     * 商品响应 → Feign DTO。
     */
    public static MobiProductFeign toFeign(ProductRes res) {
        if (res == null) {
            return null;
        }
        return MobiProductFeign.builder()
                .productId(res.getProductId())
                .shopId(res.getShopId())
                .shopName(res.getShopName())
                .categoryId(res.getCategoryId())
                .productName(res.getProductName())
                .description(res.getDescription())
                .imageUrl(res.getImageUrl())
                .price(res.getPrice())
                .status(res.getStatus())
                .stock(res.getStock() == null ? null : res.getStock().intValue())
                .soldCount(res.getSoldCount() == null ? null : res.getSoldCount().intValue())
                .createTime(res.getCreateTime())
                .build();
    }

    /**
     * Feign 查询 DTO → 商品查询命令（管理端）。
     */
    public static QueryProductCommand toQueryCommand(MobiProductFeign query) {
        if (query == null) {
            throw new ServiceException("查询参数无效", 400);
        }
        int pageNum = query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() < 1 ? 10 : query.getPageSize();
        return new QueryProductCommand(
                query.getShopId(),
                null,
                query.getProductName(),
                query.getStatus(),
                pageNum,
                pageSize
        );
    }

    private static boolean hasAnyProductUpdate(ProductReq dto) {
        return dto.getCategoryId() != null
                || dto.getProductName() != null
                || dto.getDescription() != null
                || dto.getImageUrl() != null
                || dto.getPrice() != null;
    }

    private static void assertShopId(Long shopId) {
        if (shopId == null) {
            throw new ServiceException("请选择店铺", 400);
        }
    }

    private static void assertProductId(Long productId) {
        if (productId == null) {
            throw new ServiceException("商品ID不能为空", 400);
        }
    }

    private static void assertCategoryId(Long categoryId) {
        if (categoryId == null) {
            throw new ServiceException("分类ID不能为空", 400);
        }
    }

    private static void assertProductName(String productName) {
        if (StringUtils.isEmpty(productName)) {
            throw new ServiceException("商品名称不能为空", 400);
        }
    }

    private static void assertCategoryName(String categoryName) {
        if (StringUtils.isEmpty(categoryName)) {
            throw new ServiceException("分类名称不能为空", 400);
        }
    }

    private static void assertPrice(BigDecimal price, boolean required) {
        if (price == null) {
            if (required) {
                throw new ServiceException("商品价格无效", 400);
            }
            return;
        }
        if (price.compareTo(BigDecimal.ZERO) < 0) {
            throw new ServiceException("商品价格无效", 400);
        }
    }

    /**
     * 校验库存值是否合法。
     *
     * @param stock 库存；-1 表示无限
     */
    public static void assertStock(Long stock) {
        if (stock != null && stock < -1) {
            throw new ServiceException("库存无效，-1 表示无限库存", 400);
        }
    }


    /**
     * 校验商品售卖状态编码是否合法（含系统自动售完状态）。
     */
    public static void assertProductStatus(Integer status) {
        if (status != null && status != 0 && status != 1 && status != 2) {
            throw new ServiceException("商品状态无效", 400);
        }
    }

    /**
     * 校验用户主动修改的售卖状态：仅允许出售中或下架，售完由库存自动判定。
     */
    public static void assertManualProductStatus(Integer status) {
        if (status == null) {
            throw new ServiceException("商品状态不能为空", 400);
        }
        if (status != 1 && status != 2) {
            throw new ServiceException("仅支持设置为出售中或下架", 400);
        }
    }

    private static void assertStatus(Integer status) {
        if (status == null) {
            return;
        }
        assertManualProductStatus(status);
    }
}

