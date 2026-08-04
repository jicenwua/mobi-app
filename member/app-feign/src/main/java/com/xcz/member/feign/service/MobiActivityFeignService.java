package com.xcz.member.feign.service;

import com.xcz.commons.core.domain.ResponseEntity;
import com.xcz.member.feign.dto.activity.MobiActivityDetailVO;
import com.xcz.member.feign.dto.activity.MobiActivityFeign;
import com.xcz.member.feign.dto.activity.MobiActivitySaveDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 店铺活动管理 Feign（管理后台）
 */
@FeignClient(
        value = "app-miniApp",
        path = "/shop/sys/activity",
        contextId = "MobiActivityFeignService"
)
public interface MobiActivityFeignService {

    @GetMapping("/list")
    ResponseEntity<List<MobiActivityFeign>> list(
            @RequestParam(required = false) Long shopId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer activityType);

    @GetMapping("/{id}")
    ResponseEntity<MobiActivityDetailVO> get(@PathVariable("id") Long id);

    @PostMapping
    ResponseEntity<Long> save(@RequestBody MobiActivitySaveDTO body);

    @DeleteMapping("/{id}")
    ResponseEntity<Void> remove(@PathVariable("id") Long id);

    @PutMapping("/{id}/stop")
    ResponseEntity<Void> stop(@PathVariable("id") Long id);

    @PutMapping("/{id}/enable")
    ResponseEntity<Void> enable(@PathVariable("id") Long id);
}
