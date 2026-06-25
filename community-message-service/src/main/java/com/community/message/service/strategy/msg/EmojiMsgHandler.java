package com.community.message.service.strategy.msg;

import com.community.common.exception.BusinessErrorEnum;
import com.community.common.exception.BusinessException;
import com.community.message.domain.dto.SendMsgReq;
import com.community.message.domain.entity.Message;
import com.community.message.domain.enums.MessageTypeEnum;
import org.springframework.stereotype.Component;

@Component
public class EmojiMsgHandler extends AbstractMsgHandler {

    @Override
    protected MessageTypeEnum getMsgType() {
        return MessageTypeEnum.EMOJI;
    }

    @Override
    protected void checkMsg(SendMsgReq req, Long channelId, Long uid) {
        if (req.getContent() == null || req.getContent().isBlank()) {
            throw new BusinessException(BusinessErrorEnum.MESSAGE_NOT_FOUND);
        }
    }

    @Override
    protected void saveMsg(Message message, SendMsgReq req) {
        // emoji/sticker URL stored in content field
    }
}
