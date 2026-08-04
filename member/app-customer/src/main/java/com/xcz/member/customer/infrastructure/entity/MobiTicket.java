package com.xcz.member.customer.infrastructure.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 客服工单 mobi_ticket
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@TableName("mobi_ticket")
public class MobiTicket implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long ticketId;
    private Long userId;
    private String title;
    private String description;
    /** 0-待处理 1-处理中 2-已完成 */
    private Integer status;
    private Long assignedStaffId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
