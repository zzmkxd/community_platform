package com.community.common.websocket;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * WebSocket 客户端 → 服务端 请求类型枚举
 */
@AllArgsConstructor
@Getter
public enum WSReqTypeEnum {

    HEARTBEAT(2, "心跳"),
    SEND_MESSAGE(4, "发送消息"),
    SUBSCRIBE_CHANNEL(5, "订阅频道"),
    UNSUBSCRIBE_CHANNEL(6, "取消订阅频道"),
    SUBSCRIBE_THREAD(7, "订阅话题"),
    UNSUBSCRIBE_THREAD(8, "取消订阅话题"),
    TYPING_START(9, "输入中开始"),
    TYPING_STOP(10, "输入中停止"),
    ;

    private final Integer type;
    private final String desc;

    public static WSReqTypeEnum of(Integer type) {
        for (WSReqTypeEnum value : values()) {
            if (value.getType().equals(type)) {
                return value;
            }
        }
        return null;
    }
}
