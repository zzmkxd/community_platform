package com.community.message.service.strategy.msg;

import com.community.common.exception.BusinessErrorEnum;
import com.community.common.exception.BusinessException;
import com.community.message.domain.dto.SendMsgReq;
import com.community.message.domain.entity.Message;
import com.community.message.domain.entity.MessageExtra;
import com.community.message.domain.enums.MessageTypeEnum;
import org.springframework.stereotype.Component;

@Component
public class SoundMsgHandler extends AbstractMsgHandler {

    @Override
    protected MessageTypeEnum getMsgType() {
        return MessageTypeEnum.SOUND;
    }

    @Override
    protected void checkMsg(SendMsgReq req, Long channelId, Long uid) {
        if (req.getSoundMsg() == null
                || req.getSoundMsg().getAudioUrl() == null
                || req.getSoundMsg().getSecond() == null
                || req.getSoundMsg().getSecond() <= 0) {
            throw new BusinessException(BusinessErrorEnum.FILE_NOT_FOUND);
        }
    }

    @Override
    protected void saveMsg(Message message, SendMsgReq req) {
        message.setExtra(new MessageExtra(null, req.getSoundMsg()));
    }
}
