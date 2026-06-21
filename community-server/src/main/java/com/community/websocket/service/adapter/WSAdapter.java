package com.community.websocket.service.adapter;

import com.community.common.websocket.WSRespTypeEnum;
import com.community.common.websocket.dto.WSBaseResp;
import com.community.user.domain.entity.User;
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

    public static WSBaseResp<WSLoginSuccess> buildLoginSuccessResp(User user, String token, boolean hasPower) {
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

    public static WSBaseResp buildTypingStart(Long channelId, Long userId, String nickname) {
        return WSBaseResp.of(WSRespTypeEnum.TYPING_START_PUSH.getType(),
                new Object() {
                    public final Long cid = channelId;
                    public final Long uid = userId;
                    public final String name = nickname;
                });
    }

    public static WSBaseResp buildError(String message) {
        return WSBaseResp.of(WSRespTypeEnum.ERROR.getType(), message);
    }
}
