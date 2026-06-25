package com.community.common.constant;

/**
 * Redis Key 常量
 */
public interface RedisKey {

    String TOKEN_PREFIX = "token:";
    String USER_INFO = "user:info:";

    /** 频道订阅者集合 key: ws:sub:channel:{channelId} */
    String WS_SUB_CHANNEL = "ws:sub:channel:";

    /** 话题订阅者集合 key: ws:sub:thread:{threadId} */
    String WS_SUB_THREAD = "ws:sub:thread:";

    /** 在线用户 Set */
    String ONLINE_USERS = "online:users";

    /** 用户WebSocket连接 key: ws:user:{userId} */
    String WS_USER = "ws:user:";
}
