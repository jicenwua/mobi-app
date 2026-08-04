package com.xcz.member.customer.controller;

import com.xcz.commons.core.domain.ResponseEntity;
import com.xcz.commons.core.utils.response.ResponseEntityUtils;
import com.xcz.member.customer.api.dto.request.product.ProductCategoryReq;
import com.xcz.member.customer.api.dto.request.product.ProductReq;
import com.xcz.member.customer.api.dto.response.product.ProductCategoryRes;
import com.xcz.member.customer.application.assemblers.ProductAssembler;
import com.xcz.member.customer.application.service.product.ProductApplicationQueryService;
import com.xcz.member.customer.application.service.product.ProductApplicationService;
import jakarta.annotation.Resource;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 小程序端商品接口。
 */
@RestController
@RequestMapping("/product")
public class MobiProductController {

    @Resource
    private ProductApplicationService productApplicationService;
    @Resource
    private ProductApplicationQueryService productApplicationQueryService;

    /**
     * C 端：按店铺获取全量商品目录（按分类分组，适合分类锚点跳转）。
     *
     * @param shopId 店铺 ID
     * @return 分类及商品列表
     */
    @GetMapping("/catalog")
    public ResponseEntity<List<ProductCategoryRes>> getCatalog(@RequestParam Long shopId) {
        return ResponseEntityUtils.ok(productApplicationQueryService.getCatalog(shopId), "查询成功");
    }

    /**
     * 查询店铺商品分类列表。
     *
     * @param shopId 店铺 ID
     * @return 分类列表
     */
    @GetMapping("/category")
    public ResponseEntity<List<ProductCategoryRes>> listCategories(@RequestParam Long shopId) {
        return ResponseEntityUtils.ok(productApplicationQueryService.listCategories(shopId), "查询成功");
    }

    /**
     * 批量新增商品：meta 为商品 JSON 数组，images 为图片文件（可选，与 meta 按 imageIndex 或顺序对应）。
     *
     * @param meta   商品信息 JSON 列表
     * @param images 商品图片（可选，可一次上传多个）
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<Long>> add(
            @RequestPart("meta") List<ProductReq> meta,
            @RequestPart(value = "images", required = false) MultipartFile[] images) {
        List<Long> longs = productApplicationService.addProductFromMultipart(meta, images);
        if(longs != null){
            return ResponseEntityUtils.fail(longs, "部分商品新增失败，所属分类不存在");
        }
        return ResponseEntityUtils.ok(null, "新增成功");
    }

    /**
     * 更新商品：meta 须含 productId，image 为可选新图片（不能修改库存）。
     *
     * @param dto   商品信息
     * @param image 新图片（可选）
     */
    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> update(
            @RequestPart("meta") ProductReq dto,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        String imageUrl = productApplicationService.updateProductFromMultipart(dto, image);
        return ResponseEntityUtils.ok(imageUrl, "更新成功");
    }

    /**
     * 维护商品库存：追加库存数量；-1 表示设为无限库存。
     *
     * @param dto       库存参数
     */
    @PutMapping("/stock")
    public ResponseEntity<Void> updateStock( @RequestBody ProductReq dto) {
        productApplicationService.updateStock(dto.getProductId(), dto.getStock());
        return ResponseEntityUtils.ok(null, "库存更新成功");
    }

    /**
     * 删除商品。
     *
     * @param productId 商品 ID
     */
    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> remove(@PathVariable Long productId) {
        productApplicationService.removeProduct(productId);
        return ResponseEntityUtils.ok(null, "删除成功");
    }

    /**
     * 切换商品售卖状态。
     *
     * @param productId 商品 ID
     * @param status    1-出售中 2-下架（售完状态 0 仅由库存自动设置）
     */
    @PutMapping("/{productId}/status")
    public ResponseEntity<Void> updateStatus(@PathVariable Long productId, @RequestParam Integer status) {
        productApplicationService.updateStatus(productId, status);
        return ResponseEntityUtils.ok(null, "状态更新成功");
    }

    /**
     * 新增商品分类。
     *
     * @param dto 分类信息
     * @return 新分类 ID
     */
    @PostMapping("/category")
    public ResponseEntity<Long> addCategory(@RequestBody List<ProductCategoryReq> dto) {
         productApplicationService.addCategory(ProductAssembler.toCreateCategoryCommand(dto));
        return ResponseEntityUtils.ok(null, "分类新增成功");
    }

    /**
     * 批量更新商品分类。
     *
     * @param dto 分类信息列表（每项须含 categoryId）
     */
    @PutMapping("/category")
    public ResponseEntity<Void> updateCategories(@RequestBody List<ProductCategoryReq> dto) {
        productApplicationService.updateCategories(ProductAssembler.toUpdateCategoryCommands(dto));
        return ResponseEntityUtils.ok(null, "分类更新成功");
    }

    /**
     * 删除商品分类。
     *
     * @param categoryId 分类 ID
     */
    @DeleteMapping("/category/{categoryId}")
    public ResponseEntity<Void> removeCategory(@PathVariable Long categoryId) {
        productApplicationService.removeCategory(categoryId);
        return ResponseEntityUtils.ok(null, "分类删除成功");
    }
}
