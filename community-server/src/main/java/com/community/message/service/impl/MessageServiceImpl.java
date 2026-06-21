package com.community.message.service.impl;

import com.community.common.domain.vo.response.CursorPageBaseResp;
import com.community.message.domain.vo.MessageVO;
import com.community.message.service.MessageService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageServiceImpl implements MessageService {

    @Override
    public MessageVO sendMessage(Long channelId, Long threadId, String content, Integer msgType,
                                  Long replyMsgId, List<Long> fileIds) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public CursorPageBaseResp<MessageVO> getMessages(Long channelId, Long threadId, String cursor, Integer pageSize) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public MessageVO getMessage(Long channelId, Long msgId) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public MessageVO editMessage(Long channelId, Long msgId, String content) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void deleteMessage(Long channelId, Long msgId) {
        throw new UnsupportedOperationException("TODO");
    }
}
