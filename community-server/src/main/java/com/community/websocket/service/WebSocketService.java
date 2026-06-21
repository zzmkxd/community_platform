package com.community.websocket.service;

import io.netty.channel.Channel;

public interface WebSocketService {

    /** WebSocket 连接建立 */
    void connect(Channel channel);

    /** JWT 认证 */
    void authorize(Channel channel, String token);

    /** 处理登录请求 */
    void handleLogin(Channel channel);

    /** 连接断开 */
    void removed(Channel channel);

    /** 订阅频道 */
    void subscribeChannel(Channel channel, String data);

    /** 取消订阅频道 */
    void unsubscribeChannel(Channel channel, String data);

    /** 订阅话题 */
    void subscribeThread(Channel channel, String data);

    /** 取消订阅话题 */
    void unsubscribeThread(Channel channel, String data);

    /** 向指定频道订阅者推送消息 */
    void pushToChannel(Long channelId, Object message);

    /** 向指定话题订阅者推送消息 */
    void pushToThread(Long threadId, Object message);

    /** 向指定用户推送消息 */
    void pushToUser(Long userId, Object message);
}
