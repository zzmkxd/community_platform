package com.community.websocket.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.community.common.constant.RedisKey;
import com.community.common.domain.dto.WSChannelExtraDTO;
import com.community.common.utils.RedisUtils;
import com.community.common.websocket.WSRespTypeEnum;
import com.community.common.websocket.dto.WSBaseResp;
import com.community.user.dao.UserDao;
import com.community.user.domain.entity.User;
import com.community.user.service.AuthService;
import com.community.websocket.NettyUtil;
import com.community.websocket.service.WebSocketService;
import com.community.websocket.service.adapter.WSAdapter;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.result.WxMpQrCodeTicket;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketServiceImpl implements WebSocketService {

    private static final Duration EXPIRE_TIME = Duration.ofHours(1);
    private static final Long MAX_MUM_SIZE = 10000L;
    private static final String LOGIN_CODE = "loginCode";

    /** 所有请求登录的code与channel关系 */
    private static final Cache<Integer, Channel> WAIT_LOGIN_MAP = Caffeine.newBuilder()
            .expireAfterWrite(EXPIRE_TIME)
            .maximumSize(MAX_MUM_SIZE)
            .build();

    /** 所有已连接的websocket连接列表和一些额外参数 */
    private static final ConcurrentHashMap<Channel, WSChannelExtraDTO> ONLINE_WS_MAP = new ConcurrentHashMap<>();

    /** 所有在线的用户和对应的socket */
    private static final ConcurrentHashMap<Long, CopyOnWriteArrayList<Channel>> ONLINE_UID_MAP = new ConcurrentHashMap<>();

    private final WxMpService wxMpService;
    private final AuthService authService;
    private final UserDao userDao;

    @Override
    public void connect(Channel channel) {
        ONLINE_WS_MAP.put(channel, new WSChannelExtraDTO());
        log.info("WS connected: {}", channel.id());
    }

    @Override
    public void authorize(Channel channel, String token) {
        boolean verifySuccess = authService.verify(token);
        if (verifySuccess) {
            Long uid = authService.getValidUid(token);
            User user = userDao.getById(uid);
            loginSuccess(channel, user, token);
        } else {
            sendMsg(channel, WSAdapter.buildInvalidateTokenResp());
        }
    }

    @Override
    public void handleLogin(Channel channel) {
        try {
            Integer code = generateLoginCode(channel);
            WxMpQrCodeTicket wxMpQrCodeTicket = wxMpService.getQrcodeService()
                    .qrCodeCreateTmpTicket(code, (int) EXPIRE_TIME.getSeconds());
            sendMsg(channel, WSAdapter.buildLoginResp(wxMpQrCodeTicket));
        } catch (Exception e) {
            log.error("handleLogin error", e);
        }
    }

    @Override
    public void removed(Channel channel) {
        WSChannelExtraDTO wsChannelExtraDTO = ONLINE_WS_MAP.remove(channel);
        Optional<Long> uidOptional = Optional.ofNullable(wsChannelExtraDTO)
                .map(WSChannelExtraDTO::getUid);
        offline(channel, uidOptional);

        Long uid = NettyUtil.getAttr(channel, NettyUtil.UID);
        if (uid != null) {
            broadcastOffline(uid);
            RedisUtils.del(RedisKey.WS_USER + uid);
        }
        log.info("WS removed: uid={}", uid);
    }

    @Override
    public void subscribeChannel(Channel channel, String data) {
        Long uid = NettyUtil.getAttr(channel, NettyUtil.UID);
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
    public void handleSendMessage(Channel channel, String data) {
        log.warn("WS SEND_MESSAGE rejected — messages must be sent via REST API, data: {}", data);
        sendMsg(channel, WSAdapter.buildInvalidSendMsgResp());
    }

    @Override
    public void handleTypingStart(Channel channel, String data) {
        Long uid = NettyUtil.getAttr(channel, NettyUtil.UID);
        Long channelId = parseId(data, "channelId");
        Long threadId = parseId(data, "threadId");
        if (uid == null || channelId == null) return;
        Object payload = WSAdapter.buildTypingStart(channelId, threadId, uid);
        pushToChannel(channelId, payload);
        if (threadId != null) {
            pushToThread(threadId, payload);
        }
    }

    @Override
    public void handleTypingStop(Channel channel, String data) {
        Long uid = NettyUtil.getAttr(channel, NettyUtil.UID);
        Long channelId = parseId(data, "channelId");
        Long threadId = parseId(data, "threadId");
        if (uid == null || channelId == null) return;
        Object payload = WSAdapter.buildTypingStop(channelId, threadId, uid);
        pushToChannel(channelId, payload);
        if (threadId != null) {
            pushToThread(threadId, payload);
        }
    }

    @Override
    public void broadcastOnline(Long uid) {
        WSBaseResp<?> resp = WSAdapter.buildUserOnline(uid);
        sendToAllOnline(resp, uid);
    }

    @Override
    public void broadcastOffline(Long uid) {
        WSBaseResp<?> resp = WSAdapter.buildUserOffline(uid);
        sendToAllOnline(resp, uid);
    }

    @Override
    public Boolean scanLoginSuccess(Integer loginCode, Long uid) {
        Channel channel = WAIT_LOGIN_MAP.getIfPresent(loginCode);
        if (Objects.isNull(channel)) {
            return Boolean.FALSE;
        }
        User user = userDao.getById(uid);
        WAIT_LOGIN_MAP.invalidate(loginCode);
        String token = authService.login(uid);
        loginSuccess(channel, user, token);
        return Boolean.TRUE;
    }

    @Override
    public Boolean scanSuccess(Integer loginCode) {
        Channel channel = WAIT_LOGIN_MAP.getIfPresent(loginCode);
        if (Objects.nonNull(channel)) {
            sendMsg(channel, WSAdapter.buildScanSuccessResp());
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    @Override
    public void pushToChannel(Long channelId, Object message) {
        String key = RedisKey.WS_SUB_CHANNEL + channelId;
        java.util.Set<String> uidStrs = RedisUtils.sMembers(key);
        for (String uidStr : uidStrs) {
            try {
                pushToUser(Long.parseLong(uidStr), message);
            } catch (NumberFormatException e) {
                log.warn("Invalid uid in channel subscribers: {}", uidStr);
            }
        }
    }

    @Override
    public void pushToThread(Long threadId, Object message) {
        String key = RedisKey.WS_SUB_THREAD + threadId;
        java.util.Set<String> uidStrs = RedisUtils.sMembers(key);
        for (String uidStr : uidStrs) {
            try {
                pushToUser(Long.parseLong(uidStr), message);
            } catch (NumberFormatException e) {
                log.warn("Invalid uid in thread subscribers: {}", uidStr);
            }
        }
    }

    @Override
    public void pushToUser(Long userId, Object message) {
        WSBaseResp<?> resp;
        if (message instanceof WSBaseResp<?>) {
            resp = (WSBaseResp<?>) message;
        } else {
            resp = WSBaseResp.of(WSRespTypeEnum.MESSAGE_CREATE.getType(), message);
        }
        sendToUid(resp, userId);
    }

    @Override
    public void sendToAllOnline(WSBaseResp<?> wsBaseResp, Long skipUid) {
        ONLINE_WS_MAP.forEach((channel, ext) -> {
            if (Objects.nonNull(skipUid) && Objects.equals(ext.getUid(), skipUid)) {
                return;
            }
            sendMsg(channel, wsBaseResp);
        });
    }

    @Override
    public void sendToUid(WSBaseResp<?> wsBaseResp, Long uid) {
        CopyOnWriteArrayList<Channel> channels = ONLINE_UID_MAP.get(uid);
        if (CollectionUtil.isEmpty(channels)) {
            log.info("用户: {} 不在线", uid);
            return;
        }
        channels.forEach(channel -> sendMsg(channel, wsBaseResp));
    }

    // ---- private helpers ----

    private Integer generateLoginCode(Channel channel) {
        int inc;
        do {
            inc = RedisUtils.inc(RedisKey.LOGIN_CODE, 1).intValue();
        } while (WAIT_LOGIN_MAP.asMap().containsKey(inc));
        WAIT_LOGIN_MAP.put(inc, channel);
        return inc;
    }

    private void loginSuccess(Channel channel, User user, String token) {
        online(channel, user.getId());
        boolean hasPower = user.getId() != null && user.getId() == 1L;
        sendMsg(channel, WSAdapter.buildLoginSuccessResp(user, token, hasPower));
        broadcastOnline(user.getId());
    }

    private void online(Channel channel, Long uid) {
        getOrInitChannelExt(channel).setUid(uid);
        ONLINE_UID_MAP.putIfAbsent(uid, new CopyOnWriteArrayList<>());
        ONLINE_UID_MAP.get(uid).add(channel);
        NettyUtil.setAttr(channel, NettyUtil.UID, uid);
    }

    private void offline(Channel channel, Optional<Long> uidOptional) {
        if (uidOptional.isPresent()) {
            CopyOnWriteArrayList<Channel> channels = ONLINE_UID_MAP.get(uidOptional.get());
            if (CollectionUtil.isNotEmpty(channels)) {
                channels.removeIf(ch -> Objects.equals(ch, channel));
            }
            if (CollectionUtil.isEmpty(channels)) {
                ONLINE_UID_MAP.remove(uidOptional.get());
            }
        }
    }

    private WSChannelExtraDTO getOrInitChannelExt(Channel channel) {
        WSChannelExtraDTO wsChannelExtraDTO =
                ONLINE_WS_MAP.getOrDefault(channel, new WSChannelExtraDTO());
        WSChannelExtraDTO old = ONLINE_WS_MAP.putIfAbsent(channel, wsChannelExtraDTO);
        return ObjectUtil.isNull(old) ? wsChannelExtraDTO : old;
    }

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

    private void sendMsg(Channel channel, WSBaseResp<?> wsBaseResp) {
        channel.writeAndFlush(new TextWebSocketFrame(JSONUtil.toJsonStr(wsBaseResp)));
    }
}
