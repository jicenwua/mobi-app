package com.xcz.member.feign.service;

import com.xcz.commons.core.domain.ResponseEntity;
import com.xcz.member.feign.dto.points.PointsAccountFeign;
import com.xcz.member.feign.dto.points.PointsAdjustFeign;
import com.xcz.member.feign.dto.points.PointsLogBriefFeign;
import com.xcz.member.feign.dto.points.PointsShopFeignQuery;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.openfeign.SpringQueryMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 积分服务 Feign 客户端
 */
@FeignClient(
        value = "app-miniApp",
        path = "/points/sys",
        contextId = "MobiPointsFeignService"
)
public interface MobiPointsFeignService {

    @GetMapping("/account/list")
    ResponseEntity<List<PointsAccountFeign>> accountList(@SpringQueryMap PointsShopFeignQuery query);

    @PostMapping("/account/adjust")
    ResponseEntity<Void> adjustAccount(@RequestBody PointsAdjustFeign body);

    @GetMapping("/log/list")
    ResponseEntity<List<PointsLogBriefFeign>> logList(@SpringQueryMap PointsShopFeignQuery query);

    @GetMapping("/log/{logId}")
    ResponseEntity<PointsLogBriefFeign> logDetail(
            @PathVariable("logId") Long logId,
            @RequestParam(required = false) Long shopId);
}
