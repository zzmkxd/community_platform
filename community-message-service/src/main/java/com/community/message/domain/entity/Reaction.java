package com.community.message.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("reaction")
public class Reaction {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long messageId;

    private Long userId;

    /** emoji 字符，如 👍 */
    private String emoji;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
