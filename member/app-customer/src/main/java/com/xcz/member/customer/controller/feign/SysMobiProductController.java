package com.xcz.member.customer.controller.feign;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.xcz.commons.core.domain.ResponseEntity;
import com.xcz.commons.core.exception.ServiceException;
import com.xcz.commons.core.utils.response.ResponseEntityUtils;
import com.xcz.member.customer.api.dto.response.product.ProductRes;
import com.xcz.member.customer.application.assemblers.ProductAssembler;
import com.xcz.member.customer.application.service.product.ProductApplicationQueryService;
import com.xcz.member.customer.application.service.product.ProductApplicationService;
import com.xcz.member.feign.dto.product.MobiProductFeign;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理后台商品查询（Feign 入口）
 */
@RestController
@RequestMapping("/product")
public class SysMobiProductController {

    @Resource
    private ProductApplicationQueryService productApplicationQueryService;
    @Resource
    private ProductApplicationService productApplicationService;

    @GetMapping("/list")
    public ResponseEntity<List<MobiProductFeign>> list(MobiProductFeign query) {
        Page<ProductRes> page = productApplicationQueryService.pageForFeign(ProductAssembler.toQueryCommand(query));
        List<MobiProductFeign> rows = page.getRecords().stream()
                .map(ProductAssembler::toFeign)
                .toList();
        ResponseEntity<List<MobiProductFeign>> res = ResponseEntityUtils.ok(rows, "查询成功");
        res.setTotal(page.getTotal());
        return res;
    }

    @PutMapping("/status")
    public ResponseEntity<Void> updateStatus(@RequestBody MobiProductFeign body) {
        if (body == null || body.getProductId() == null || body.getStatus() == null) {
            throw new ServiceException("商品ID与状态不能为空", 400);
        }
        productApplicationService.updateStatus(body.getProductId(), body.getStatus());
        return ResponseEntityUtils.ok(null, "更新成功");
    }
}
