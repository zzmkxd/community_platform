package com.community.message.service.impl;

import com.community.common.domain.vo.response.CursorPageBaseResp;
import com.community.message.domain.vo.MessageVO;
import com.community.message.service.SearchService;
import org.springframework.stereotype.Service;

@Service
public class SearchServiceImpl implements SearchService {

    @Override
    public CursorPageBaseResp<MessageVO> search(Long serverId, String q, Long channelId,
                                                  String from, String to, Integer page) {
        throw new UnsupportedOperationException("TODO");
    }
}
