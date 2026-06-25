package com.community.common.websocket;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * WebSocket 服务端 → 客户端 推送类型枚举
 */
@AllArgsConstructor
@Getter
public enum WSRespTypeEnum {

    LOGIN_SUCCESS(3, "用户登录成功返回用户信息"),
    INVALIDATE_TOKEN(6, "使前端的token失效"),
    MESSAGE_CREATE(20, "新消息推送"),
    MESSAGE_UPDATE(21, "消息编辑推送"),
    MESSAGE_DELETE(22, "消息删除推送"),
    REACTION_ADD(23, "Reaction 添加推送"),
    REACTION_REMOVE(24, "Reaction 移除推送"),
    TYPING_START_PUSH(25, "输入中开始推送"),
    TYPING_STOP_PUSH(26, "输入中停止推送"),
    THREAD_CREATE(34, "话题创建推送"),
    MEMBER_JOIN(30, "成员加入推送"),
    MEMBER_LEAVE(31, "成员离开推送"),
    MEMBER_KICK(32, "成员被踢推送"),
    USER_ONLINE(40, "用户上线推送"),
    USER_OFFLINE(41, "用户离线推送"),
    CHANNEL_CREATE(50, "频道创建推送"),
    CHANNEL_UPDATE(51, "频道更新推送"),
    CHANNEL_DELETE(52, "频道删除推送"),
    SERVER_UPDATE(53, "服务器更新推送"),
    ERROR(99, "错误推送"),
    ;

    private final Integer type;
    private final String desc;

    public static WSRespTypeEnum of(Integer type) {
        for (WSRespTypeEnum value : values()) {
            if (value.getType().equals(type)) {
                return value;
            }
        }
        return null;
    }
}
