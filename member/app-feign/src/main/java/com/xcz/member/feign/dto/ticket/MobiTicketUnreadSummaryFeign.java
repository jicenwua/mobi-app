package com.xcz.member.feign.dto.ticket;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MobiTicketUnreadSummaryFeign {
    /** 工单列表未读总数 */
    private Integer listUnread;
    /** 我的工单未读总数 */
    private Integer mineUnread;
}
