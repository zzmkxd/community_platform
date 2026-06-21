package com.community.message.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("thread")
public class Thread {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long channelId;

    /** 触发生成 thread 的那条消息 */
    private Long rootMsgId;

    private String name;

    private Long creatorId;

    /** ACTIVE / ARCHIVED */
    private String status;

    private Integer messageCount;

    private Long lastMsgId;

    private LocalDateTime lastActive;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
