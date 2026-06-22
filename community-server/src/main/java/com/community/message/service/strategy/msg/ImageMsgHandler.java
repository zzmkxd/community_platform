package com.community.message.service.strategy.msg;

import cn.hutool.json.JSONUtil;
import com.community.common.exception.BusinessErrorEnum;
import com.community.common.exception.BusinessException;
import com.community.message.domain.dto.SendMsgReq;
import com.community.message.domain.entity.Message;
import com.community.message.domain.enums.MessageTypeEnum;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ImageMsgHandler extends AbstractMsgHandler {

    @Override
    protected MessageTypeEnum getMsgType() {
        return MessageTypeEnum.IMAGE;
    }

    @Override
    protected void checkMsg(SendMsgReq req, Long channelId, Long uid) {
        if (req.getFileIds() == null || req.getFileIds().isEmpty()) {
            throw new BusinessException(BusinessErrorEnum.FILE_NOT_FOUND);
        }
    }

    @Override
    protected void saveMsg(Message message, SendMsgReq req) {
        message.setExtra(JSONUtil.toJsonStr(new ExtraBody(req.getFileIds())));
    }

    record ExtraBody(List<Long> fileIds) {}
}
