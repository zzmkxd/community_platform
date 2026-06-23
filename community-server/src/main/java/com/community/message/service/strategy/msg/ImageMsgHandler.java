package com.community.message.service.strategy.msg;

import com.community.common.exception.BusinessErrorEnum;
import com.community.common.exception.BusinessException;
import com.community.file.service.FileService;

import com.community.common.enums.FileStatusEnum;
import com.community.message.domain.dto.SendMsgReq;
import com.community.message.domain.entity.Message;
import com.community.message.domain.entity.MessageExtra;
import com.community.message.domain.enums.MessageTypeEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ImageMsgHandler extends AbstractMsgHandler {

    @Autowired
    private FileService fileService;

    @Override
    protected MessageTypeEnum getMsgType() {
        return MessageTypeEnum.IMAGE;
    }

    @Override
    protected void checkMsg(SendMsgReq req, Long channelId, Long uid) {
        if (req.getFileIds() == null || req.getFileIds().isEmpty()) {
            throw new BusinessException(BusinessErrorEnum.FILE_NOT_FOUND);
        }
        for (Long fileId : req.getFileIds()) {
            if (!fileService.isFileUploaded(fileId)) {
                throw new BusinessException(BusinessErrorEnum.FILE_UPLOAD_FAILED);
            }
        }
    }

    @Override
    protected void saveMsg(Message message, SendMsgReq req) {
        message.setExtra(new MessageExtra(req.getFileIds(), null));
    }
}
