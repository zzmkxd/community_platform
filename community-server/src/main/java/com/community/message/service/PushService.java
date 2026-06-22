package com.community.message.service;

import com.community.common.domain.dto.PushMessageDTO;

/**
 * 推送路由服务 — 封装 RocketMQ PUSH_TOPIC 发送。
 */
public interface PushService {

    /** 向频道订阅者推送 */
    void pushToChannel(Long channelId, Long excludeUid, Object data);

    /** 向话题订阅者推送 */
    void pushToThread(Long threadId, Object data);

    /** 向指定用户推送 */
    void pushToUser(Long userId, Object data);
}
