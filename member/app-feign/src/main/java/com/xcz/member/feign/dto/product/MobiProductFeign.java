package com.xcz.member.feign.dto.product;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MobiProductFeign implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long productId;
    private Long shopId;
    private String shopName;
    private Long categoryId;
    private String productName;
    private String description;
    private String imageUrl;
    private BigDecimal price;
    /** 0-售完 1-出售中 */
    private Integer status;
    private Integer stock;
    private Integer soldCount;
    private LocalDateTime createTime;

    private int pageNum = 1;
    private int pageSize = 10;
}
