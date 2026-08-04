package com.xcz.member.customer.application.command.shop;

import com.xcz.commons.core.exception.ServiceException;
import com.xcz.commons.core.utils.StringUtils;

/**
 * 店铺审核命令
 */
public record AuditShopCommand(
        Long shopId,
        Integer auditStatus,
        String auditReason
) {
    public AuditShopCommand {
        if (shopId == null) {
            throw new ServiceException("店铺 ID 不能为空", 400);
        }
        if (auditStatus == null || (auditStatus != 1 && auditStatus != 2)) {
            throw new ServiceException("审核状态只能为 1（通过）或 2（驳回）", 400);
        }
        if (auditStatus == 2 && StringUtils.isEmpty(auditReason)) {
            throw new ServiceException("驳回时必须填写原因", 400);
        }
    }
}
