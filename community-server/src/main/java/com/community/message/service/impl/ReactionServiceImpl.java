package com.community.message.service.impl;

import com.community.common.exception.BusinessErrorEnum;
import com.community.common.exception.BusinessException;
import com.community.common.utils.RequestHolder;
import com.community.message.dao.ReactionDao;
import com.community.message.domain.entity.Reaction;
import com.community.message.domain.vo.ReactionVO;
import com.community.message.service.ReactionService;
import com.community.common.enums.PermissionBit;
import com.community.server.domain.vo.ChannelVO;
import com.community.server.service.ChannelService;
import com.community.server.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReactionServiceImpl implements ReactionService {

    private final ReactionDao reactionDao;
    private final PermissionService permissionService;
    private final com.community.message.dao.MessageDao messageDao;
    private final ChannelService channelService;

    @Override
    @Transactional
    public List<ReactionVO> addReaction(Long msgId, String emoji) {
        Long uid = RequestHolder.get().getUid();

        com.community.message.domain.entity.Message message = messageDao.getById(msgId);
        if (message == null || message.getStatus() == 1) {
            throw new BusinessException(BusinessErrorEnum.MESSAGE_NOT_FOUND);
        }

        ChannelVO channel = channelService.getById(message.getChannelId());
        if (channel != null && !permissionService.checkPermission(
                channel.getServerId(), uid, message.getChannelId(),
                PermissionBit.ADD_REACTIONS.getBit())) {
            throw new BusinessException(BusinessErrorEnum.NO_PERMISSION);
        }

        // Toggle: same user + same message + same emoji → remove
        Reaction existing = reactionDao.lambdaQuery()
                .eq(Reaction::getMessageId, msgId)
                .eq(Reaction::getUserId, uid)
                .eq(Reaction::getEmoji, emoji)
                .one();

        if (existing != null) {
            reactionDao.removeById(existing.getId());
            log.info("Reaction removed (toggle): msgId={}, userId={}, emoji={}", msgId, uid, emoji);
        } else {
            Reaction reaction = new Reaction();
            reaction.setMessageId(msgId);
            reaction.setUserId(uid);
            reaction.setEmoji(emoji);
            reactionDao.save(reaction);
            log.info("Reaction added: msgId={}, userId={}, emoji={}", msgId, uid, emoji);
        }

        return getReactions(msgId);
    }

    @Override
    @Transactional
    public List<ReactionVO> removeReaction(Long msgId, String emoji) {
        Long uid = RequestHolder.get().getUid();

        reactionDao.lambdaUpdate()
                .eq(Reaction::getMessageId, msgId)
                .eq(Reaction::getUserId, uid)
                .eq(Reaction::getEmoji, emoji)
                .remove();

        return getReactions(msgId);
    }

    @Override
    public List<ReactionVO> getReactions(Long msgId) {
        List<Reaction> reactions = reactionDao.lambdaQuery()
                .eq(Reaction::getMessageId, msgId)
                .list();

        // Group by emoji, count + collect userIds
        Map<String, List<Reaction>> grouped = reactions.stream()
                .collect(Collectors.groupingBy(Reaction::getEmoji));

        Long uid = RequestHolder.get() != null ? RequestHolder.get().getUid() : null;

        List<ReactionVO> vos = new ArrayList<>();
        for (Map.Entry<String, List<Reaction>> entry : grouped.entrySet()) {
            ReactionVO vo = new ReactionVO();
            vo.setEmoji(entry.getKey());
            vo.setCount(entry.getValue().size());
            vo.setUserIds(entry.getValue().stream().map(Reaction::getUserId).limit(10).toList());
            vo.setReacted(uid != null && entry.getValue().stream().anyMatch(r -> r.getUserId().equals(uid)));
            vos.add(vo);
        }
        return vos;
    }
}
