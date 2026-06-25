package com.community.message.service;

import com.community.common.domain.vo.response.CursorPageBaseResp;
import com.community.message.domain.vo.MessageVO;

public interface SearchService {

    CursorPageBaseResp<MessageVO> search(Long serverId, String q, Long channelId,
                                          String from, String to, Integer page);
}
