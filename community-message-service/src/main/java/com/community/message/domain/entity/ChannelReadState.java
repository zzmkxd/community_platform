package com.community.message.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("channel_read_state")
public class ChannelReadState {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long channelId;

    private Long lastReadMsgId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
