package com.community.websocket.service;

import com.community.common.websocket.dto.WSBaseResp;
import io.netty.channel.Channel;

public interface WebSocketService {

    /** WebSocket 连接建立 */
    void connect(Channel channel);

    /** JWT 认证 */
    void authorize(Channel channel, String token);

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

    /** 处理客户端发送消息请求（Phase 3 实现完整链路） */
    void handleSendMessage(Channel channel, String data);

    /** 处理输入中开始 */
    void handleTypingStart(Channel channel, String data);

    /** 处理输入中停止 */
    void handleTypingStop(Channel channel, String data);

    /** 广播用户上线给同服务器成员 */
    void broadcastOnline(Long uid);

    /** 广播用户离线给同服务器成员 */
    void broadcastOffline(Long uid);

    /** 向指定频道订阅者推送消息 */
    void pushToChannel(Long channelId, Object message);

    /** 向指定话题订阅者推送消息 */
    void pushToThread(Long threadId, Object message);

    /** 向指定用户推送消息 */
    void pushToUser(Long userId, Object message);

    /** 向所有在线用户推送（可跳过指定用户） */
    void sendToAllOnline(WSBaseResp<?> wsBaseResp, Long skipUid);

    /** 向指定用户推送 */
    void sendToUid(WSBaseResp<?> wsBaseResp, Long uid);
}
