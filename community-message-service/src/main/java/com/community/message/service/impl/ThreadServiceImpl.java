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
import com.community.common.enums.PermissionBit;
import com.community.server.domain.vo.ChannelVO;
import com.community.server.service.ChannelService;
import com.community.server.service.PermissionService;
import com.community.common.domain.vo.UserVO;
import com.community.user.service.UserService;
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
    private final ChannelService channelService;
    private final UserService userService;
    private final ReactionDao reactionDao;
    private final PermissionService permissionService;

    @Override
    @Transactional
    public ThreadVO createThread(Long channelId, Long rootMsgId, String name) {
        if (RequestHolder.get() == null) {
            throw new BusinessException(BusinessErrorEnum.UNAUTHORIZED);
        }
        Long uid = RequestHolder.get().getUid();

        ChannelVO channel = channelService.getById(channelId);
        if (channel == null) {
            throw new BusinessException(BusinessErrorEnum.CHANNEL_NOT_FOUND);
        }

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

        UserVO user = userService.getUserById(uid);
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
        List<UserVO> creators = userService.getBatchUsers(creatorIds);
        Map<Long, UserVO> userMap = creators.stream()
                .collect(Collectors.toMap(UserVO::getId, u -> u));

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
        UserVO creator = userService.getUserById(thread.getCreatorId());
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
        List<UserVO> users = userService.getBatchUsers(userIds);
        Map<Long, UserVO> userMap = users.stream()
                .collect(Collectors.toMap(UserVO::getId, u -> u));

        List<Long> msgIds = page.getList().stream().map(Message::getId).toList();
        Map<Long, List<ReactionVO>> reactionMap = MessageAdapter.buildReactionMap(msgIds, reactionDao);

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

        UserVO creator = userService.getUserById(thread.getCreatorId());
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
        if (!inactive.isEmpty()) {
            List<Long> threadIds = inactive.stream().map(Thread::getId).toList();
            threadDao.lambdaUpdate()
                    .in(Thread::getId, threadIds)
                    .set(Thread::getStatus, "ARCHIVED")
                    .update();
            log.info("Auto-archived {} inactive threads", inactive.size());
        }
    }

    private ThreadVO toThreadVO(Thread thread, UserVO creator) {
        ThreadVO vo = new ThreadVO();
        vo.setId(thread.getId());
        vo.setChannelId(thread.getChannelId());
        vo.setRootMsgId(thread.getRootMsgId());
        vo.setName(thread.getName());
        vo.setStatus(thread.getStatus());
        vo.setMessageCount(thread.getMessageCount());
        vo.setLastActive(thread.getLastActive());
        vo.setCreateTime(thread.getCreateTime());
        vo.setCreator(creator);
        return vo;
    }
}
