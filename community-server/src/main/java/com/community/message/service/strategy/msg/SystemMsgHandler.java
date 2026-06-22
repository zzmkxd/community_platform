package com.community.message.service.strategy.msg;

import com.community.message.domain.dto.SendMsgReq;
import com.community.message.domain.entity.Message;
import com.community.message.domain.enums.MessageTypeEnum;
import org.springframework.stereotype.Component;

@Component
public class SystemMsgHandler extends AbstractMsgHandler {

    @Override
    protected MessageTypeEnum getMsgType() {
        return MessageTypeEnum.SYSTEM;
    }

    @Override
    protected void checkMsg(SendMsgReq req, Long channelId, Long uid) {
        if (req.getContent() == null || req.getContent().isBlank()) {
            req.setContent("");
        }
    }

    @Override
    protected void saveMsg(Message message, SendMsgReq req) {
        // system messages: content contains the system event description
    }
}
