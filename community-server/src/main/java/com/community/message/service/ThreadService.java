package com.community.message.service;

import com.community.common.domain.vo.response.CursorPageBaseResp;
import com.community.message.domain.vo.MessageVO;
import com.community.message.domain.vo.ThreadVO;

public interface ThreadService {

    ThreadVO createThread(Long channelId, Long rootMsgId, String name);

    CursorPageBaseResp<ThreadVO> getThreads(Long channelId, String cursor, Integer pageSize);

    ThreadVO getThread(Long threadId);

    CursorPageBaseResp<MessageVO> getThreadMessages(Long threadId, String cursor, Integer pageSize);

    ThreadVO updateThread(Long threadId, String name, String status);
}
