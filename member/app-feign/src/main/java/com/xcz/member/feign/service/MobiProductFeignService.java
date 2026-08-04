package com.xcz.member.feign.service;

import com.xcz.commons.core.domain.ResponseEntity;
import com.xcz.member.feign.dto.product.MobiProductFeign;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

/**
 * 店铺商品 Feign 客户端
 */
@FeignClient(
        value = "app-miniApp",
        path = "/product",
        contextId = "MobiProductFeignService"
)
public interface MobiProductFeignService {

    @GetMapping("/list")
    ResponseEntity<List<MobiProductFeign>> list(@SpringQueryMap MobiProductFeign query);

    @PutMapping("/status")
    ResponseEntity<Void> updateStatus(@RequestBody MobiProductFeign body);
}
