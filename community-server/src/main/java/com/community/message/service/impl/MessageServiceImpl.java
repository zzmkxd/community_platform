package com.community.message.service.impl;

import cn.hutool.json.JSONUtil;
import com.community.common.constant.MQConstant;
import com.community.common.domain.vo.request.CursorPageBaseReq;
import com.community.common.domain.vo.response.CursorPageBaseResp;
import com.community.common.exception.BusinessErrorEnum;
import com.community.common.exception.BusinessException;
import com.community.common.transaction.service.MQProducer;
import com.community.common.utils.CursorUtils;
import com.community.common.utils.RequestHolder;
import com.community.file.dao.FileAttachmentDao;
import com.community.file.domain.entity.FileAttachment;
import com.community.message.dao.MessageDao;
import com.community.message.dao.ReactionDao;
import com.community.message.dao.ThreadDao;
import com.community.message.domain.dto.SendMsgReq;
import com.community.message.domain.entity.Message;
import com.community.message.domain.entity.MessageExtra;
import com.community.message.domain.entity.Reaction;
import com.community.message.domain.entity.Thread;
import com.community.message.domain.vo.FileVO;
import com.community.message.domain.vo.MessageVO;
import com.community.message.domain.vo.ReactionVO;
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
    private final ReactionDao reactionDao;
    private final PermissionService permissionService;
    private final MQProducer mqProducer;
    private final FileAttachmentDao fileAttachmentDao;

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

        String body = JSONUtil.toJsonStr(JSONUtil.createObj()
                .set("messageId", msgId)
                .set("channelId", channelId)
                .set("threadId", threadId)
                .set("fromUid", uid));
        mqProducer.sendSecureMsg(MQConstant.SEND_MSG_TOPIC, body);

        Message saved = messageDao.getById(msgId);
        User user = userDao.getById(uid);
        MessageVO vo = MessageAdapter.buildMessageVO(saved, user, Collections.emptyList(), thread);
        vo.setAttachments(MessageAdapter.buildAttachments(saved, fileAttachmentDao));
        return vo;
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
        MessageVO vo = MessageAdapter.buildMessageVO(message, user, Collections.emptyList(), null);
        vo.setAttachments(MessageAdapter.buildAttachments(message, fileAttachmentDao));
        return vo;
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
        MessageVO vo = MessageAdapter.buildMessageVO(message, user, Collections.emptyList(), null);
        vo.setAttachments(MessageAdapter.buildAttachments(message, fileAttachmentDao));
        return vo;
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
        if (messages.isEmpty()) {
            return List.of();
        }

        List<Long> userIds = messages.stream().map(Message::getFromUid).distinct().toList();
        Map<Long, User> userMap = userDao.lambdaQuery()
                .in(User::getId, userIds)
                .list()
                .stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<Long> msgIds = messages.stream().map(Message::getId).toList();
        Map<Long, List<ReactionVO>> reactionMap = MessageAdapter.buildReactionMap(msgIds, reactionDao);

        Map<Long, List<FileVO>> attachmentMap = MessageAdapter.buildAttachmentMap(messages, fileAttachmentDao);

        return messages.stream()
                .map(msg -> {
                    MessageVO vo = MessageAdapter.buildMessageVO(
                            msg, userMap.get(msg.getFromUid()),
                            reactionMap.getOrDefault(msg.getId(), List.of()), null);
                    vo.setAttachments(attachmentMap.get(msg.getId()));
                    return vo;
                })
                .toList();
    }
}
