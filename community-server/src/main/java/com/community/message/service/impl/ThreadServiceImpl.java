package com.community.message.service.impl;

import com.community.common.domain.vo.request.CursorPageBaseReq;
import com.community.common.domain.vo.response.CursorPageBaseResp;
import com.community.common.exception.BusinessErrorEnum;
import com.community.common.exception.BusinessException;
import com.community.common.utils.CursorUtils;
import com.community.common.utils.RequestHolder;
import com.community.message.dao.MessageDao;
import com.community.message.dao.ReactionDao;
import com.community.message.dao.ThreadDao;
import com.community.message.domain.entity.Message;
import com.community.message.domain.entity.Reaction;
import com.community.message.domain.entity.Thread;
import com.community.message.domain.vo.MessageVO;
import com.community.message.domain.vo.ReactionVO;
import com.community.message.domain.vo.ThreadVO;
import com.community.message.service.ThreadService;
import com.community.message.service.adapter.MessageAdapter;
import com.community.server.dao.ChannelDao;
import com.community.server.domain.entity.Channel;
import com.community.server.domain.enums.PermissionBit;
import com.community.server.service.PermissionService;
import com.community.user.dao.UserDao;
import com.community.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThreadServiceImpl implements ThreadService {

    private final ThreadDao threadDao;
    private final MessageDao messageDao;
    private final ChannelDao channelDao;
    private final UserDao userDao;
    private final ReactionDao reactionDao;
    private final PermissionService permissionService;

    @Override
    @Transactional
    public ThreadVO createThread(Long channelId, Long rootMsgId, String name) {
        Long uid = RequestHolder.get().getUid();

        Channel channel = channelDao.lambdaQuery()
                .eq(Channel::getId, channelId)
                .eq(Channel::getStatus, 1)
                .oneOpt()
                .orElseThrow(() -> new BusinessException(BusinessErrorEnum.CHANNEL_NOT_FOUND));

        if (!permissionService.checkPermission(channel.getServerId(), uid, channelId,
                PermissionBit.USE_THREADS.getBit())) {
            throw new BusinessException(BusinessErrorEnum.NO_PERMISSION);
        }

        Thread thread = new Thread();
        thread.setChannelId(channelId);
        thread.setRootMsgId(rootMsgId);
        thread.setName(name);
        thread.setCreatorId(uid);
        thread.setStatus("ACTIVE");
        thread.setMessageCount(0);
        thread.setLastActive(LocalDateTime.now());
        threadDao.save(thread);

        User user = userDao.getById(uid);
        log.info("Thread created: id={}, name={}, channelId={}", thread.getId(), name, channelId);
        return toThreadVO(thread, user);
    }

    @Override
    public CursorPageBaseResp<ThreadVO> getThreads(Long channelId, String cursor, Integer pageSize) {
        CursorPageBaseReq req = new CursorPageBaseReq(pageSize != null ? pageSize : 25, cursor);
        CursorPageBaseResp<Thread> page = CursorUtils.getCursorPageByMysql(
                threadDao, req,
                wrapper -> wrapper.eq(Thread::getChannelId, channelId),
                Thread::getLastActive
        );

        if (page.isEmpty()) {
            return CursorPageBaseResp.empty();
        }

        List<Thread> threads = page.getList();
        List<Long> creatorIds = threads.stream().map(Thread::getCreatorId).distinct().toList();
        Map<Long, User> userMap = userDao.lambdaQuery()
                .in(User::getId, creatorIds)
                .list()
                .stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<ThreadVO> voList = threads.stream()
                .map(t -> toThreadVO(t, userMap.get(t.getCreatorId())))
                .toList();
        return CursorPageBaseResp.init(page, voList);
    }

    @Override
    public ThreadVO getThread(Long threadId) {
        Thread thread = threadDao.getById(threadId);
        if (thread == null) {
            throw new BusinessException(BusinessErrorEnum.THREAD_NOT_FOUND);
        }
        User creator = userDao.getById(thread.getCreatorId());
        return toThreadVO(thread, creator);
    }

    @Override
    public CursorPageBaseResp<MessageVO> getThreadMessages(Long threadId, String cursor, Integer pageSize) {
        CursorPageBaseReq req = new CursorPageBaseReq(pageSize != null ? pageSize : 50, cursor);
        CursorPageBaseResp<Message> page = CursorUtils.getCursorPageByMysql(
                messageDao, req,
                wrapper -> wrapper.eq(Message::getThreadId, threadId).ne(Message::getStatus, 1),
                Message::getId
        );

        if (page.isEmpty()) {
            return CursorPageBaseResp.empty();
        }

        List<Long> userIds = page.getList().stream().map(Message::getFromUid).distinct().toList();
        Map<Long, User> userMap = userDao.lambdaQuery()
                .in(User::getId, userIds)
                .list()
                .stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<Long> msgIds = page.getList().stream().map(Message::getId).toList();
        Map<Long, List<ReactionVO>> reactionMap = buildReactionMapForMessages(msgIds);

        List<MessageVO> vos = page.getList().stream()
                .map(msg -> MessageAdapter.buildMessageVO(
                        msg, userMap.get(msg.getFromUid()),
                        reactionMap.getOrDefault(msg.getId(), List.of()), null))
                .toList();
        return CursorPageBaseResp.init(page, vos);
    }

    @Override
    @Transactional
    public ThreadVO updateThread(Long threadId, String name, String status) {
        Thread thread = threadDao.getById(threadId);
        if (thread == null) {
            throw new BusinessException(BusinessErrorEnum.THREAD_NOT_FOUND);
        }

        if (name != null) {
            thread.setName(name);
        }
        if (status != null) {
            thread.setStatus(status);
        }
        threadDao.updateById(thread);

        User creator = userDao.getById(thread.getCreatorId());
        log.info("Thread updated: id={}, name={}, status={}", threadId, name, status);
        return toThreadVO(thread, creator);
    }

    @Scheduled(cron = "0 0 * * * *")
    public void autoArchiveThreads() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        List<Thread> inactive = threadDao.lambdaQuery()
                .eq(Thread::getStatus, "ACTIVE")
                .lt(Thread::getLastActive, cutoff)
                .list();
        for (Thread t : inactive) {
            t.setStatus("ARCHIVED");
            threadDao.updateById(t);
        }
        if (!inactive.isEmpty()) {
            log.info("Auto-archived {} inactive threads", inactive.size());
        }
    }

    private Map<Long, List<ReactionVO>> buildReactionMapForMessages(List<Long> msgIds) {
        if (msgIds.isEmpty()) return Map.of();
        List<Reaction> reactions = reactionDao.lambdaQuery()
                .in(Reaction::getMessageId, msgIds)
                .list();

        Map<Long, Map<String, ReactionVO>> grouped = new java.util.HashMap<>();
        for (Reaction r : reactions) {
            Map<String, ReactionVO> emojiMap = grouped.computeIfAbsent(r.getMessageId(),
                    k -> new java.util.LinkedHashMap<>());
            ReactionVO vo = emojiMap.computeIfAbsent(r.getEmoji(), emoji -> {
                ReactionVO rvo = new ReactionVO();
                rvo.setEmoji(emoji);
                rvo.setCount(0);
                rvo.setUserIds(new java.util.ArrayList<>());
                return rvo;
            });
            vo.setCount(vo.getCount() + 1);
            vo.getUserIds().add(r.getUserId());
        }

        Long currentUid = RequestHolder.get() != null ? RequestHolder.get().getUid() : null;
        Map<Long, List<ReactionVO>> result = new java.util.HashMap<>();
        for (var entry : grouped.entrySet()) {
            List<ReactionVO> list = new java.util.ArrayList<>(entry.getValue().values());
            if (currentUid != null) {
                for (ReactionVO vo : list) {
                    vo.setReacted(vo.getUserIds().contains(currentUid));
                }
            }
            result.put(entry.getKey(), list);
        }
        return result;
    }

    private ThreadVO toThreadVO(Thread thread, User creator) {
        ThreadVO vo = new ThreadVO();
        vo.setId(thread.getId());
        vo.setChannelId(thread.getChannelId());
        vo.setRootMsgId(thread.getRootMsgId());
        vo.setName(thread.getName());
        vo.setStatus(thread.getStatus());
        vo.setMessageCount(thread.getMessageCount());
        vo.setLastActive(thread.getLastActive());
        vo.setCreateTime(thread.getCreateTime());
        if (creator != null) {
            com.community.user.domain.vo.UserVO cv = new com.community.user.domain.vo.UserVO();
            cv.setId(creator.getId());
            cv.setNickname(creator.getNickname());
            cv.setAvatar(creator.getAvatar());
            vo.setCreator(cv);
        }
        return vo;
    }
}
