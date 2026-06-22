package com.community.message.service.impl;

import com.community.common.domain.vo.request.CursorPageBaseReq;
import com.community.common.domain.vo.response.CursorPageBaseResp;
import com.community.common.exception.BusinessErrorEnum;
import com.community.common.exception.BusinessException;
import com.community.common.utils.CursorUtils;
import com.community.common.utils.RequestHolder;
import com.community.message.dao.MessageDao;
import com.community.message.dao.ThreadDao;
import com.community.message.domain.entity.Message;
import com.community.message.domain.entity.Thread;
import com.community.message.domain.vo.MessageVO;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ThreadServiceImpl implements ThreadService {

    private final ThreadDao threadDao;
    private final MessageDao messageDao;
    private final ChannelDao channelDao;
    private final UserDao userDao;
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

        log.info("Thread created: id={}, name={}, channelId={}", thread.getId(), name, channelId);
        return toThreadVO(thread);
    }

    @Override
    public CursorPageBaseResp<ThreadVO> getThreads(Long channelId, String cursor, Integer pageSize) {
        CursorPageBaseReq req = new CursorPageBaseReq(pageSize != null ? pageSize : 25, cursor);
        CursorPageBaseResp<Thread> page = CursorUtils.getCursorPageByMysql(
                threadDao, req,
                wrapper -> wrapper.eq(Thread::getChannelId, channelId),
                Thread::getId
        );

        if (page.isEmpty()) {
            return CursorPageBaseResp.empty();
        }

        List<ThreadVO> voList = page.getList().stream().map(this::toThreadVO).toList();
        return CursorPageBaseResp.init(page, voList);
    }

    @Override
    public ThreadVO getThread(Long threadId) {
        Thread thread = threadDao.getById(threadId);
        if (thread == null) {
            throw new BusinessException(BusinessErrorEnum.THREAD_NOT_FOUND);
        }
        return toThreadVO(thread);
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

        List<MessageVO> vos = page.getList().stream()
                .map(msg -> {
                    User user = userDao.getById(msg.getFromUid());
                    return MessageAdapter.buildMessageVO(msg, user, Collections.emptyList(), null);
                })
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

        log.info("Thread updated: id={}, name={}, status={}", threadId, name, status);
        return toThreadVO(thread);
    }

    private ThreadVO toThreadVO(Thread thread) {
        ThreadVO vo = new ThreadVO();
        vo.setId(thread.getId());
        vo.setChannelId(thread.getChannelId());
        vo.setRootMsgId(thread.getRootMsgId());
        vo.setName(thread.getName());
        vo.setStatus(thread.getStatus());
        vo.setMessageCount(thread.getMessageCount());
        vo.setLastActive(thread.getLastActive());
        vo.setCreateTime(thread.getCreateTime());
        return vo;
    }
}
