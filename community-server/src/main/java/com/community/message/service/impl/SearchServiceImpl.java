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
import com.community.user.dao.UserDao;
import com.community.user.domain.entity.User;
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
    private final UserDao userDao;
    private final ReactionDao reactionDao;

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

        List<Long> userIds = messages.stream().map(Message::getFromUid).distinct().toList();
        Map<Long, User> userMap = userDao.lambdaQuery()
                .in(User::getId, userIds)
                .list()
                .stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<Long> msgIds = messages.stream().map(Message::getId).toList();
        Map<Long, List<ReactionVO>> reactionMap = buildReactionMap(msgIds);

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

    private Map<Long, List<ReactionVO>> buildReactionMap(List<Long> msgIds) {
        if (msgIds.isEmpty()) return Map.of();
        List<Reaction> reactions = reactionDao.lambdaQuery()
                .in(Reaction::getMessageId, msgIds)
                .list();

        Map<Long, Map<String, ReactionVO>> grouped = new HashMap<>();
        for (Reaction r : reactions) {
            Map<String, ReactionVO> emojiMap = grouped.computeIfAbsent(r.getMessageId(),
                    k -> new LinkedHashMap<>());
            ReactionVO vo = emojiMap.computeIfAbsent(r.getEmoji(), emoji -> {
                ReactionVO rvo = new ReactionVO();
                rvo.setEmoji(emoji);
                rvo.setCount(0);
                rvo.setUserIds(new ArrayList<>());
                return rvo;
            });
            vo.setCount(vo.getCount() + 1);
            vo.getUserIds().add(r.getUserId());
        }

        Long currentUid = RequestHolder.get() != null ? RequestHolder.get().getUid() : null;
        Map<Long, List<ReactionVO>> result = new HashMap<>();
        for (var entry : grouped.entrySet()) {
            List<ReactionVO> list = new ArrayList<>(entry.getValue().values());
            if (currentUid != null) {
                for (ReactionVO vo : list) {
                    vo.setReacted(vo.getUserIds().contains(currentUid));
                }
            }
            result.put(entry.getKey(), list);
        }
        return result;
    }
}
