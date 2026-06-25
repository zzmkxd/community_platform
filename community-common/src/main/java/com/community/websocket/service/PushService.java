package com.community.websocket.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * WebSocket 推送路由服务 — 封装 RocketMQ PUSH_TOPIC 发送。
 * <p>
 * 跨服务调用通过 Feign 到达 websocket-service 的 /internal/push/** REST 端点，
 * 然后统一走 RocketMQ → PushConsumer → WebSocket 推送链路。
 */
@FeignClient(name = "community-websocket", path = "/internal/push")
public interface PushService {

    /** 向频道订阅者推送 */
    @PostMapping("/channel/{channelId}")
    void pushToChannel(@PathVariable("channelId") Long channelId,
                       @RequestParam(value = "excludeUid", required = false) Long excludeUid,
                       @RequestBody Object data);

    /** 向话题订阅者推送 */
    @PostMapping("/thread/{threadId}")
    void pushToThread(@PathVariable("threadId") Long threadId, @RequestBody Object data);

    /** 向指定用户推送 */
    @PostMapping("/user/{userId}")
    void pushToUser(@PathVariable("userId") Long userId, @RequestBody Object data);

    /** 向服务器所有在线成员推送（排除指定用户） */
    @PostMapping("/server/{serverId}")
    void pushToServer(@PathVariable("serverId") Long serverId,
                      @RequestParam(value = "excludeUid", required = false) Long excludeUid,
                      @RequestBody Object data);
}
