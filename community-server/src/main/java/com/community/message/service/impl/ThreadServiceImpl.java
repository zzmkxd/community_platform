package com.community.message.service.impl;

import com.community.common.domain.vo.response.CursorPageBaseResp;
import com.community.message.domain.vo.MessageVO;
import com.community.message.domain.vo.ThreadVO;
import com.community.message.service.ThreadService;
import org.springframework.stereotype.Service;

@Service
public class ThreadServiceImpl implements ThreadService {

    @Override
    public ThreadVO createThread(Long channelId, Long rootMsgId, String name) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public CursorPageBaseResp<ThreadVO> getThreads(Long channelId, String cursor, Integer pageSize) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public ThreadVO getThread(Long threadId) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public CursorPageBaseResp<MessageVO> getThreadMessages(Long threadId, String cursor, Integer pageSize) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public ThreadVO updateThread(Long threadId, String name, String status) {
        throw new UnsupportedOperationException("TODO");
    }
}
