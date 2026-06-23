package com.community.websocket.service;

/**
 * WebSocket 推送路由服务 — 封装 RocketMQ PUSH_TOPIC 发送。
 * <p>
 * 注：从 message.service 迁移到 websocket.service，推送能力属于实时通信模块。
 * 微服务拆分后由 websocket-service 暴露 /internal/push/** REST 端点供其他服务 Feign 调用。
 */
public interface PushService {

    /** 向频道订阅者推送 */
    void pushToChannel(Long channelId, Long excludeUid, Object data);

    /** 向话题订阅者推送 */
    void pushToThread(Long threadId, Object data);

    /** 向指定用户推送 */
    void pushToUser(Long userId, Object data);

    /** 向服务器所有在线成员推送（排除指定用户） */
    void pushToServer(Long serverId, Long excludeUid, Object data);
}
