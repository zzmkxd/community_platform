package com.community.server.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("channel")
public class Channel {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long serverId;

    private Long categoryId;

    private String name;

    /** TEXT=0, VOICE=1 */
    private Integer type;

    private String topic;

    private Integer sortOrder;

    private Integer status;

    private Long lastMsgId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
