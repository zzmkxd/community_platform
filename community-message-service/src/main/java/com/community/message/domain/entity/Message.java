package com.community.message.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(value = "message", autoResultMap = true)
public class Message {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long channelId;

    /** null = 频道主时间线消息，非 null = Thread 内消息 */
    private Long threadId;

    private Long fromUid;

    private String content;

    /** TEXT=1, IMAGE=2, FILE=3, SYSTEM=4, SOUND=5 */
    private Integer msgType;

    /** JSON 扩展：附件ID列表、@提及列表、URL预览等 */
    @TableField(value = "extra", typeHandler = JacksonTypeHandler.class)
    private MessageExtra extra;

    /** 0=正常, 1=删除, 2=编辑过 */
    private Integer status;

    private Long replyMsgId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
