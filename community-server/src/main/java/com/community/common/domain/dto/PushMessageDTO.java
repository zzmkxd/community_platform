package com.community.common.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RocketMQ PUSH_TOPIC 消息体。由 MsgSendConsumer 构建，PushConsumer 消费后分派推送。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PushMessageDTO {

    /** "channel" | "thread" | "user" */
    private String targetType;

    /** channelId / threadId / userId */
    private Long targetId;

    /** WS 响应 JSON 字符串 */
    private String data;
}
