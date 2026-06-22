package com.community.message.service.impl;

import com.community.common.domain.vo.request.CursorPageBaseReq;
import com.community.common.domain.vo.response.CursorPageBaseResp;
import com.community.common.exception.BusinessErrorEnum;
import com.community.common.exception.BusinessException;
import com.community.common.utils.CursorUtils;
import com.community.common.utils.RequestHolder;
import com.community.message.dao.MessageDao;
import com.community.message.dao.ThreadDao;
import com.community.message.domain.dto.SendMsgReq;
import com.community.message.domain.entity.Message;
import com.community.message.domain.entity.Thread;
import com.community.message.domain.vo.MessageVO;
import com.community.message.event.ChannelMessageSendEvent;
import com.community.message.service.MessageService;
import com.community.message.service.adapter.MessageAdapter;
import com.community.message.service.strategy.msg.AbstractMsgHandler;
import com.community.message.service.strategy.msg.MsgHandlerFactory;
import com.community.server.dao.ChannelDao;
import com.community.server.domain.entity.Channel;
import com.community.server.domain.enums.PermissionBit;
import com.community.server.service.PermissionService;
import com.community.user.dao.UserDao;
import com.community.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageDao messageDao;
    private final ThreadDao threadDao;
    private final ChannelDao channelDao;
    private final UserDao userDao;
    private final PermissionService permissionService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public MessageVO sendMessage(Long channelId, Long threadId, String content, Integer msgType,
                                  Long replyMsgId, List<Long> fileIds) {
        Long uid = RequestHolder.get().getUid();

        Channel channel = channelDao.lambdaQuery()
                .eq(Channel::getId, channelId)
                .eq(Channel::getStatus, 1)
                .oneOpt()
                .orElseThrow(() -> new BusinessException(BusinessErrorEnum.CHANNEL_NOT_FOUND));

        Thread thread = null;
        if (threadId != null) {
            thread = threadDao.lambdaQuery()
                    .eq(Thread::getId, threadId)
                    .oneOpt()
                    .orElseThrow(() -> new BusinessException(BusinessErrorEnum.THREAD_NOT_FOUND));
        }

        if (!permissionService.checkPermission(channel.getServerId(), uid, channelId,
                PermissionBit.SEND_MESSAGES.getBit())) {
            throw new BusinessException(BusinessErrorEnum.NO_PERMISSION);
        }

        SendMsgReq req = new SendMsgReq();
        req.setContent(content);
        req.setMsgType(msgType != null ? msgType : 1);
        req.setThreadId(threadId);
        req.setReplyMsgId(replyMsgId);
        req.setFileIds(fileIds);

        AbstractMsgHandler handler = MsgHandlerFactory.getStrategyNoNull(req.getMsgType());
        Long msgId = handler.checkAndSaveMsg(req, channelId, uid);

        eventPublisher.publishEvent(new ChannelMessageSendEvent(this, msgId, channelId, threadId, uid));

        Message saved = messageDao.getById(msgId);
        User user = userDao.getById(uid);
        return MessageAdapter.buildMessageVO(saved, user, Collections.emptyList(), thread);
    }

    @Override
    public CursorPageBaseResp<MessageVO> getMessages(Long channelId, Long threadId, String cursor, Integer pageSize) {
        CursorPageBaseReq req = new CursorPageBaseReq(pageSize != null ? pageSize : 50, cursor);
        CursorPageBaseResp<Message> page = CursorUtils.getCursorPageByMysql(
                messageDao, req,
                wrapper -> {
                    wrapper.eq(Message::getChannelId, channelId);
                    if (threadId != null) {
                        wrapper.eq(Message::getThreadId, threadId);
                    } else {
                        wrapper.isNull(Message::getThreadId);
                    }
                    wrapper.ne(Message::getStatus, 1);
                },
                Message::getId
        );

        if (page.isEmpty()) {
            return CursorPageBaseResp.empty();
        }

        List<MessageVO> voList = buildMessageVOList(page.getList());
        return CursorPageBaseResp.init(page, voList);
    }

    @Override
    public MessageVO getMessage(Long channelId, Long msgId) {
        Message message = messageDao.lambdaQuery()
                .eq(Message::getId, msgId)
                .eq(Message::getChannelId, channelId)
                .ne(Message::getStatus, 1)
                .oneOpt()
                .orElseThrow(() -> new BusinessException(BusinessErrorEnum.MESSAGE_NOT_FOUND));

        User user = userDao.getById(message.getFromUid());
        return MessageAdapter.buildMessageVO(message, user, Collections.emptyList(), null);
    }

    @Override
    @Transactional
    public MessageVO editMessage(Long channelId, Long msgId, String content) {
        Long uid = RequestHolder.get().getUid();

        Message message = messageDao.lambdaQuery()
                .eq(Message::getId, msgId)
                .eq(Message::getChannelId, channelId)
                .ne(Message::getStatus, 1)
                .oneOpt()
                .orElseThrow(() -> new BusinessException(BusinessErrorEnum.MESSAGE_NOT_FOUND));

        if (!message.getFromUid().equals(uid)) {
            throw new BusinessException(BusinessErrorEnum.NOT_MESSAGE_AUTHOR);
        }

        message.setContent(content);
        message.setStatus(2);
        messageDao.updateById(message);

        log.info("Message edited: msgId={}, channelId={}, uid={}", msgId, channelId, uid);
        User user = userDao.getById(uid);
        return MessageAdapter.buildMessageVO(message, user, Collections.emptyList(), null);
    }

    @Override
    @Transactional
    public void deleteMessage(Long channelId, Long msgId) {
        Long uid = RequestHolder.get().getUid();

        Message message = messageDao.lambdaQuery()
                .eq(Message::getId, msgId)
                .eq(Message::getChannelId, channelId)
                .ne(Message::getStatus, 1)
                .oneOpt()
                .orElseThrow(() -> new BusinessException(BusinessErrorEnum.MESSAGE_NOT_FOUND));

        Channel channel = channelDao.getById(channelId);
        boolean isAdmin = channel != null && permissionService.checkPermission(
                channel.getServerId(), uid, channelId,
                PermissionBit.ADMINISTRATOR.getBit());

        if (!message.getFromUid().equals(uid) && !isAdmin) {
            throw new BusinessException(BusinessErrorEnum.NO_PERMISSION);
        }

        message.setStatus(1);
        messageDao.updateById(message);
        log.info("Message deleted: msgId={}, channelId={}, uid={}", msgId, channelId, uid);
    }

    private List<MessageVO> buildMessageVOList(List<Message> messages) {
        List<Long> userIds = messages.stream().map(Message::getFromUid).distinct().toList();
        Map<Long, User> userMap = userDao.lambdaQuery()
                .in(User::getId, userIds)
                .list()
                .stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        return messages.stream()
                .map(msg -> MessageAdapter.buildMessageVO(
                        msg, userMap.get(msg.getFromUid()), Collections.emptyList(), null))
                .toList();
    }
}
