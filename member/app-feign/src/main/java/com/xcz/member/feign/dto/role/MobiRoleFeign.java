package com.xcz.member.feign.dto.role;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 小程序权限 mobi_role Feign DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MobiRoleFeign implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String roleKey;
    private String roleDescription;
    private Boolean status;
    private LocalDateTime createTime;
    private Long createBy;
    private LocalDateTime updateTime;
    private Long updateBy;
}
