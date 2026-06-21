package com.community.websocket;

import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.json.JSONUtil;
import com.community.common.websocket.WSReqTypeEnum;
import com.community.common.websocket.dto.WSBaseReq;
import com.community.websocket.service.WebSocketService;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Sharable
public class NettyWebSocketServerHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

    private WebSocketService webSocketService;

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        this.webSocketService = SpringUtil.getBean(WebSocketService.class);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        userOffLine(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.warn("channelInactive [{}]", ctx.channel().id());
        userOffLine(ctx);
    }

    private void userOffLine(ChannelHandlerContext ctx) {
        this.webSocketService.removed(ctx.channel());
        ctx.channel().close();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent idleStateEvent) {
            if (idleStateEvent.state() == IdleState.READER_IDLE) {
                userOffLine(ctx);
            }
        } else if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
            this.webSocketService.connect(ctx.channel());
            String token = NettyUtil.getAttr(ctx.channel(), NettyUtil.TOKEN);
            if (cn.hutool.core.util.StrUtil.isNotBlank(token)) {
                this.webSocketService.authorize(ctx.channel(), token);
            }
        }
        super.userEventTriggered(ctx, evt);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.warn("Exception: {}", cause.getMessage());
        ctx.channel().close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) {
        WSBaseReq wsBaseReq = JSONUtil.toBean(msg.text(), WSBaseReq.class);
        WSReqTypeEnum wsReqTypeEnum = WSReqTypeEnum.of(wsBaseReq.getType());
        if (wsReqTypeEnum == null) {
            log.info("Unknown WS type: {}", wsBaseReq.getType());
            return;
        }
        switch (wsReqTypeEnum) {
            case LOGIN -> webSocketService.handleLogin(ctx.channel());
            case HEARTBEAT -> { /* no-op */ }
            case SUBSCRIBE_CHANNEL ->
                    webSocketService.subscribeChannel(ctx.channel(), wsBaseReq.getData());
            case UNSUBSCRIBE_CHANNEL ->
                    webSocketService.unsubscribeChannel(ctx.channel(), wsBaseReq.getData());
            case SUBSCRIBE_THREAD ->
                    webSocketService.subscribeThread(ctx.channel(), wsBaseReq.getData());
            case UNSUBSCRIBE_THREAD ->
                    webSocketService.unsubscribeThread(ctx.channel(), wsBaseReq.getData());
            default -> log.info("Unhandled WS type: {}", wsReqTypeEnum);
        }
    }
}
