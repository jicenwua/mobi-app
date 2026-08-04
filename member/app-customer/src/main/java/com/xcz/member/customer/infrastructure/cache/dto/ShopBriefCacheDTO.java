package com.xcz.member.customer.infrastructure.cache.dto;

import com.xcz.member.customer.domain.dto.shop.ShopBriefDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

/**
 * 店铺简要信息 Redis 缓存模型。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShopBriefCacheDTO implements Serializable {

    private Long id;
    private Long parentId;
    private String shopName;
    private String shopCode;
    private Integer ratio;
    private String picture;
    private String phone;
    private Integer categoryId;
    private String province;
    private String city;
    private String district;
    private String address;

    public static ShopBriefCacheDTO from(ShopBriefDTO dto) {
        if (dto == null) {
            return null;
        }
        return ShopBriefCacheDTO.builder()
                .id(dto.id())
                .parentId(dto.parentId())
                .shopName(dto.shopName())
                .shopCode(dto.shopCode())
                .ratio(dto.ratio())
                .picture(dto.picture())
                .phone(dto.phone())
                .categoryId(dto.categoryId())
                .province(dto.province())
                .city(dto.city())
                .district(dto.district())
                .address(dto.address())
                .build();
    }



    public static List<ShopBriefCacheDTO> fromList(Collection<ShopBriefDTO> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return List.of();
        }
        return dtos.stream().map(ShopBriefCacheDTO::from).toList();
    }
}
