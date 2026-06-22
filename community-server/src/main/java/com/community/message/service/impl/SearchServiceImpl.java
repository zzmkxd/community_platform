package com.community.message.service.impl;

import com.community.common.domain.vo.response.CursorPageBaseResp;
import com.community.message.dao.MessageDao;
import com.community.message.domain.entity.Message;
import com.community.message.domain.vo.MessageVO;
import com.community.message.service.SearchService;
import com.community.message.service.adapter.MessageAdapter;
import com.community.user.dao.UserDao;
import com.community.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final MessageDao messageDao;
    private final UserDao userDao;

    @Override
    public CursorPageBaseResp<MessageVO> search(Long serverId, String q, Long channelId,
                                                 String from, String to, Integer page) {
        List<Message> messages = messageDao.lambdaQuery()
                .like(Message::getContent, q)
                .ne(Message::getStatus, 1)
                .last(channelId != null ? "AND channel_id = " + channelId : "")
                .orderByDesc(Message::getCreateTime)
                .last("LIMIT 50")
                .list();

        List<MessageVO> vos = messages.stream()
                .map(msg -> {
                    User user = userDao.getById(msg.getFromUid());
                    return MessageAdapter.buildMessageVO(msg, user, Collections.emptyList(), null);
                })
                .toList();

        CursorPageBaseResp<MessageVO> resp = new CursorPageBaseResp<>();
        resp.setIsLast(true);
        resp.setList(vos);
        return resp;
    }
}
