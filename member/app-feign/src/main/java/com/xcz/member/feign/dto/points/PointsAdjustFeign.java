package com.xcz.member.feign.dto.points;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 管理后台手动调整用户积分
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointsAdjustFeign implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long userId;
    private Long shopId;
    /** 1-基础积分 2-赠送积分 */
    private Integer pointType;
    /** 1-增加 2-减少 */
    private Integer direction;
    private BigDecimal amount;
    /** 调整备注 */
    private String remark;
}
