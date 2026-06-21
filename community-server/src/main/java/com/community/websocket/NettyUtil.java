package com.community.websocket;

import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;

public class NettyUtil {

    public static final AttributeKey<String> TOKEN = AttributeKey.valueOf("token");
    public static final AttributeKey<String> IP = AttributeKey.valueOf("ip");
    public static final AttributeKey<Long> UID = AttributeKey.valueOf("uid");
    public static final AttributeKey<WebSocketServerHandshaker> HANDSHAKER_ATTR_KEY =
            AttributeKey.valueOf(WebSocketServerHandshaker.class, "HANDSHAKER");

    public static <T> void setAttr(Channel channel, AttributeKey<T> attributeKey, T data) {
        Attribute<T> attr = channel.attr(attributeKey);
        attr.set(data);
    }

    public static <T> T getAttr(Channel channel, AttributeKey<T> attributeKey) {
        return channel.attr(attributeKey).get();
    }
}
