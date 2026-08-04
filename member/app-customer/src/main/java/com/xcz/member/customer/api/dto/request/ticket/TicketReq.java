package com.xcz.member.customer.api.dto.request.ticket;

import lombok.Data;

@Data
public class TicketReq {
    private String title;
    private String description;
    private String content;
    private Integer status;
    private Long userId;
    private Integer pageNum;
    private Integer pageSize;
}
