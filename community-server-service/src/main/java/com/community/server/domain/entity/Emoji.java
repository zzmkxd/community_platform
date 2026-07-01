package com.community.server.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("emoji")
public class Emoji {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long serverId;

    private String name;

    /** MinIO 上的 objectKey */
    private String url;

    private Long creatorId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
