package com.community.websocket.service.adapter;

import com.community.common.websocket.WSRespTypeEnum;
import com.community.common.websocket.dto.WSBaseResp;

/**
 * WebSocket 响应构建适配器
 */
public class WSAdapter {

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
