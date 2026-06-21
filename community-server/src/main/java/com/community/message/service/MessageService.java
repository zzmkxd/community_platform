package com.community.message.service;

import com.community.common.domain.vo.response.CursorPageBaseResp;
import com.community.message.domain.vo.MessageVO;

import java.util.List;

public interface MessageService {

    MessageVO sendMessage(Long channelId, Long threadId, String content, Integer msgType,
                          Long replyMsgId, List<Long> fileIds);

    CursorPageBaseResp<MessageVO> getMessages(Long channelId, Long threadId, String cursor, Integer pageSize);

    MessageVO getMessage(Long channelId, Long msgId);

    MessageVO editMessage(Long channelId, Long msgId, String content);

    void deleteMessage(Long channelId, Long msgId);
}
