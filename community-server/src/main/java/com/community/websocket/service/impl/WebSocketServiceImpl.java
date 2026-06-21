package com.community.websocket.service.impl;

import cn.hutool.json.JSONUtil;
import com.community.common.constant.RedisKey;
import com.community.common.utils.JwtUtils;
import com.community.common.utils.RedisUtils;
import com.community.common.websocket.WSRespTypeEnum;
import com.community.common.websocket.dto.WSBaseResp;
import com.community.websocket.NettyUtil;
import com.community.websocket.service.WebSocketService;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketServiceImpl implements WebSocketService {

    private final JwtUtils jwtUtils;

    /** userId → Channel */
    private static final ConcurrentHashMap<Long, Channel> USER_CHANNEL_MAP = new ConcurrentHashMap<>();

    @Override
    public void connect(Channel channel) {
        log.info("WS connected: {}", channel.id());
    }

    @Override
    public void authorize(Channel channel, String token) {
        Long uid = jwtUtils.getUidOrNull(token);
        if (uid == null) {
            sendToChannel(channel, WSRespTypeEnum.ERROR, "Token invalid");
            channel.close();
            return;
        }
        NettyUtil.setAttr(channel, NettyUtil.UID, uid);
        USER_CHANNEL_MAP.put(uid, channel);
        sendToChannel(channel, WSRespTypeEnum.LOGIN_SUCCESS, "authenticated");
        log.info("WS authorized: uid={}", uid);
    }

    @Override
    public void handleLogin(Channel channel) {
        // 兼容 MallChat 的 LOGIN 请求类型
        Long uid = NettyUtil.getAttr(channel, NettyUtil.UID);
        if (uid != null) {
            sendToChannel(channel, WSRespTypeEnum.LOGIN_SUCCESS, "ok");
        }
    }

    @Override
    public void removed(Channel channel) {
        Long uid = NettyUtil.getAttr(channel, NettyUtil.UID);
        if (uid != null) {
            USER_CHANNEL_MAP.remove(uid);
            // 清理订阅关系
            RedisUtils.del(RedisKey.WS_USER + uid);
            log.info("WS removed: uid={}", uid);
        }
    }

    @Override
    public void subscribeChannel(Channel channel, String data) {
        Long uid = NettyUtil.getAttr(channel, NettyUtil.UID);
        // data 格式: {"channelId": 1}  或直接是 channelId
        Long channelId = parseId(data, "channelId");
        if (uid != null && channelId != null) {
            RedisUtils.sAdd(RedisKey.WS_SUB_CHANNEL + channelId, String.valueOf(uid));
            log.info("User {} subscribed channel {}", uid, channelId);
        }
    }

    @Override
    public void unsubscribeChannel(Channel channel, String data) {
        Long uid = NettyUtil.getAttr(channel, NettyUtil.UID);
        Long channelId = parseId(data, "channelId");
        if (uid != null && channelId != null) {
            RedisUtils.sRemove(RedisKey.WS_SUB_CHANNEL + channelId, String.valueOf(uid));
        }
    }

    @Override
    public void subscribeThread(Channel channel, String data) {
        Long uid = NettyUtil.getAttr(channel, NettyUtil.UID);
        Long threadId = parseId(data, "threadId");
        if (uid != null && threadId != null) {
            RedisUtils.sAdd(RedisKey.WS_SUB_THREAD + threadId, String.valueOf(uid));
        }
    }

    @Override
    public void unsubscribeThread(Channel channel, String data) {
        Long uid = NettyUtil.getAttr(channel, NettyUtil.UID);
        Long threadId = parseId(data, "threadId");
        if (uid != null && threadId != null) {
            RedisUtils.sRemove(RedisKey.WS_SUB_THREAD + threadId, String.valueOf(uid));
        }
    }

    @Override
    public void pushToChannel(Long channelId, Object message) {
        // TODO: 从 Redis Set 读取订阅者，逐个 pushToUser
        // RedisUtils.sMembers(RedisKey.WS_SUB_CHANNEL + channelId)
    }

    @Override
    public void pushToThread(Long threadId, Object message) {
        // TODO: 从 Redis Set 读取订阅者，逐个 pushToUser
    }

    @Override
    public void pushToUser(Long userId, Object message) {
        Channel channel = USER_CHANNEL_MAP.get(userId);
        if (channel != null && channel.isActive()) {
            WSBaseResp resp = WSBaseResp.of(WSRespTypeEnum.MESSAGE_CREATE.getType(), message);
            channel.writeAndFlush(new TextWebSocketFrame(JSONUtil.toJsonStr(resp)));
        }
    }

    // ---- private helpers ----

    private Long parseId(String data, String key) {
        try {
            if (data.matches("\\d+")) {
                return Long.parseLong(data);
            }
            var json = JSONUtil.parseObj(data);
            return json.getLong(key);
        } catch (Exception e) {
            log.warn("Failed to parse {} from: {}", key, data);
            return null;
        }
    }

    private void sendToChannel(Channel channel, WSRespTypeEnum type, Object data) {
        WSBaseResp resp = WSBaseResp.of(type.getType(), data);
        channel.writeAndFlush(new TextWebSocketFrame(JSONUtil.toJsonStr(resp)));
    }
}
