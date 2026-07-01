package com.community.message.service.strategy.msg;

import com.community.message.dao.MessageDao;
import com.community.message.domain.dto.SendMsgReq;
import com.community.message.domain.entity.Message;
import com.community.message.domain.enums.MessageStatusEnum;
import com.community.message.domain.enums.MessageTypeEnum;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;

public abstract class AbstractMsgHandler {

    @Autowired
    protected MessageDao messageDao;

    @PostConstruct
    private void init() {
        MsgHandlerFactory.register(getMsgType().getType(), this);
    }

    protected abstract MessageTypeEnum getMsgType();

    protected abstract void checkMsg(SendMsgReq req, Long channelId, Long uid);

    protected abstract void saveMsg(Message message, SendMsgReq req);

    public Long checkAndSaveMsg(SendMsgReq req, Long channelId, Long uid) {
        checkMsg(req, channelId, uid);
        Message message = new Message();
        message.setChannelId(channelId);
        message.setThreadId(req.getThreadId());
        message.setFromUid(uid);
        message.setContent(req.getContent());
        message.setMsgType(getMsgType().getType());
        message.setReplyMsgId(req.getReplyMsgId());
        message.setStatus(MessageStatusEnum.NORMAL.getStatus());
        saveMsg(message, req);
        messageDao.save(message);
        return message.getId();
    }
}
