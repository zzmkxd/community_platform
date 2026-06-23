package com.community.message.service.impl;

import com.community.common.domain.vo.response.CursorPageBaseResp;
import com.community.common.utils.RequestHolder;
import com.community.message.dao.MessageDao;
import com.community.message.dao.ReactionDao;
import com.community.message.domain.entity.Message;
import com.community.message.domain.entity.Reaction;
import com.community.message.domain.vo.MessageVO;
import com.community.message.domain.vo.ReactionVO;
import com.community.message.service.SearchService;
import com.community.message.service.adapter.MessageAdapter;
import com.community.user.domain.vo.UserVO;
import com.community.user.service.UserService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final MessageDao messageDao;
    private final UserService userService;
    private final ReactionDao reactionDao;

    @Override
    public CursorPageBaseResp<MessageVO> search(Long serverId, String q, Long channelId,
                                                 String from, String to, Integer page) {
        var query = messageDao.lambdaQuery()
                .like(Message::getContent, q)
                .ne(Message::getStatus, 1);
        if (channelId != null) {
            query.eq(Message::getChannelId, channelId);
        }
        List<Message> messages = query
                .orderByDesc(Message::getCreateTime)
                .last("LIMIT 50")
                .list();

        List<Long> userIds = messages.stream().map(Message::getFromUid).distinct().toList();
        List<UserVO> users = userService.getBatchUsers(userIds);
        Map<Long, UserVO> userMap = users.stream()
                .collect(Collectors.toMap(UserVO::getId, u -> u));

        List<Long> msgIds = messages.stream().map(Message::getId).toList();
        Map<Long, List<ReactionVO>> reactionMap = MessageAdapter.buildReactionMap(msgIds, reactionDao);

        List<MessageVO> vos = messages.stream()
                .map(msg -> MessageAdapter.buildMessageVO(
                        msg, userMap.get(msg.getFromUid()),
                        reactionMap.getOrDefault(msg.getId(), List.of()), null))
                .toList();

        CursorPageBaseResp<MessageVO> resp = new CursorPageBaseResp<>();
        resp.setIsLast(true);
        resp.setList(vos);
        return resp;
    }

}
