package com.community.websocket.service.adapter;

import com.community.common.websocket.WSRespTypeEnum;
import com.community.common.websocket.dto.WSBaseResp;
import com.community.user.domain.vo.UserVO;
import com.community.user.domain.vo.response.ws.WSLoginSuccess;
import com.community.user.domain.vo.response.ws.WSLoginUrl;
import me.chanjar.weixin.mp.bean.result.WxMpQrCodeTicket;

/**
 * WebSocket 响应构建适配器
 */
public class WSAdapter {

    public static WSBaseResp<WSLoginUrl> buildLoginResp(WxMpQrCodeTicket wxMpQrCodeTicket) {
        WSBaseResp<WSLoginUrl> wsBaseResp = new WSBaseResp<>();
        wsBaseResp.setType(WSRespTypeEnum.LOGIN_URL.getType());
        wsBaseResp.setData(WSLoginUrl.builder().loginUrl(wxMpQrCodeTicket.getUrl()).build());
        return wsBaseResp;
    }

    public static WSBaseResp<WSLoginSuccess> buildLoginSuccessResp(UserVO user, String token, boolean hasPower) {
        WSBaseResp<WSLoginSuccess> wsBaseResp = new WSBaseResp<>();
        wsBaseResp.setType(WSRespTypeEnum.LOGIN_SUCCESS.getType());
        WSLoginSuccess wsLoginSuccess = WSLoginSuccess.builder()
                .avatar(user.getAvatar())
                .name(user.getNickname())
                .token(token)
                .uid(user.getId())
                .power(hasPower ? 1 : 0)
                .build();
        wsBaseResp.setData(wsLoginSuccess);
        return wsBaseResp;
    }

    public static WSBaseResp<?> buildScanSuccessResp() {
        WSBaseResp<?> wsBaseResp = new WSBaseResp<>();
        wsBaseResp.setType(WSRespTypeEnum.LOGIN_SCAN_SUCCESS.getType());
        return wsBaseResp;
    }

    public static WSBaseResp<?> buildInvalidateTokenResp() {
        WSBaseResp<?> wsBaseResp = new WSBaseResp<>();
        wsBaseResp.setType(WSRespTypeEnum.INVALIDATE_TOKEN.getType());
        return wsBaseResp;
    }

    public static WSBaseResp buildMessagePush(Object messageVO) {
        return WSBaseResp.of(WSRespTypeEnum.MESSAGE_CREATE.getType(), messageVO);
    }

    public static WSBaseResp buildReactionAdd(Long msgId, String emoji, Long userId, int totalCount) {
        return WSBaseResp.of(WSRespTypeEnum.REACTION_ADD.getType(),
                new Object() {
                    public final Long messageId = msgId;
                    public final String e = emoji;
                    public final Long uid = userId;
                    public final int count = totalCount;
                });
    }

    public static WSBaseResp buildTypingStart(Long channelId, Long threadId, Long userId) {
        return WSBaseResp.of(WSRespTypeEnum.TYPING_START_PUSH.getType(),
                new Object() {
                    public final Long cid = channelId;
                    public final Long tid = threadId;
                    public final Long uid = userId;
                });
    }

    public static WSBaseResp buildTypingStop(Long channelId, Long threadId, Long userId) {
        return WSBaseResp.of(WSRespTypeEnum.TYPING_STOP_PUSH.getType(),
                new Object() {
                    public final Long cid = channelId;
                    public final Long tid = threadId;
                    public final Long uid = userId;
                });
    }

    public static WSBaseResp buildUserOnline(Long userId) {
        return WSBaseResp.of(WSRespTypeEnum.USER_ONLINE.getType(),
                new Object() { public final Long uid = userId; });
    }

    public static WSBaseResp buildUserOffline(Long userId) {
        return WSBaseResp.of(WSRespTypeEnum.USER_OFFLINE.getType(),
                new Object() { public final Long uid = userId; });
    }

    public static WSBaseResp buildMessageDelete(Long msgId, Long channelId) {
        final Long mid = msgId;
        final Long cid = channelId;
        return WSBaseResp.of(WSRespTypeEnum.MESSAGE_DELETE.getType(),
                new Object() { public final Long msgId = mid; public final Long channelId = cid; });
    }

    public static WSBaseResp buildMessageUpdate(Object messageVO) {
        return WSBaseResp.of(WSRespTypeEnum.MESSAGE_UPDATE.getType(), messageVO);
    }

    public static WSBaseResp buildReactionRemove(Long msgId, String emoji, Long userId, int totalCount) {
        return WSBaseResp.of(WSRespTypeEnum.REACTION_REMOVE.getType(),
                new Object() {
                    public final Long messageId = msgId;
                    public final String e = emoji;
                    public final Long uid = userId;
                    public final int count = totalCount;
                });
    }

    public static WSBaseResp buildThreadCreate(Object threadVO) {
        return WSBaseResp.of(WSRespTypeEnum.THREAD_CREATE.getType(), threadVO);
    }

    public static WSBaseResp buildMemberJoin(Long serverId, Object memberVO) {
        return WSBaseResp.of(WSRespTypeEnum.MEMBER_JOIN.getType(), memberVO);
    }

    public static WSBaseResp buildMemberLeave(Long serverId, Long userId) {
        return WSBaseResp.of(WSRespTypeEnum.MEMBER_LEAVE.getType(),
                new Object() { public final Long sid = serverId; public final Long uid = userId; });
    }

    public static WSBaseResp buildMemberKick(Long serverId, Long userId) {
        return WSBaseResp.of(WSRespTypeEnum.MEMBER_KICK.getType(),
                new Object() { public final Long sid = serverId; public final Long uid = userId; });
    }

    public static WSBaseResp buildChannelCreate(Object channelVO) {
        return WSBaseResp.of(WSRespTypeEnum.CHANNEL_CREATE.getType(), channelVO);
    }

    public static WSBaseResp buildChannelUpdate(Object channelVO) {
        return WSBaseResp.of(WSRespTypeEnum.CHANNEL_UPDATE.getType(), channelVO);
    }

    public static WSBaseResp buildChannelDelete(Long channelId) {
        return WSBaseResp.of(WSRespTypeEnum.CHANNEL_DELETE.getType(),
                new Object() { public final Long cid = channelId; });
    }

    public static WSBaseResp buildServerUpdate(Object serverVO) {
        return WSBaseResp.of(WSRespTypeEnum.SERVER_UPDATE.getType(), serverVO);
    }

    public static WSBaseResp buildError(String message) {
        return WSBaseResp.of(WSRespTypeEnum.ERROR.getType(), message);
    }

    public static WSBaseResp<String> buildInvalidSendMsgResp() {
        return WSBaseResp.of(WSRespTypeEnum.ERROR.getType(),
                "Messages must be sent via REST API: POST /api/v1/channels/{channelId}/messages");
    }
}
