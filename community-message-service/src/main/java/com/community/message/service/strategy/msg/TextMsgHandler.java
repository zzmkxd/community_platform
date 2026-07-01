package com.community.message.service.strategy.msg;

import com.community.common.algorithm.sensitiveWord.SensitiveWordBs;
import com.community.common.exception.BusinessErrorEnum;
import com.community.common.exception.BusinessException;
import com.community.message.domain.dto.SendMsgReq;
import com.community.message.domain.entity.Message;
import com.community.message.domain.entity.MessageExtra;
import com.community.message.domain.enums.MessageTypeEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TextMsgHandler extends AbstractMsgHandler {

    @Autowired
    private SensitiveWordBs sensitiveWordBs;

    @Override
    protected MessageTypeEnum getMsgType() {
        return MessageTypeEnum.TEXT;
    }

    @Override
    protected void checkMsg(SendMsgReq req, Long channelId, Long uid) {
        // 允许纯文件消息（无文本内容但有 fileIds）
        boolean hasFiles = req.getFileIds() != null && !req.getFileIds().isEmpty();
        if (!hasFiles && (req.getContent() == null || req.getContent().isBlank())) {
            throw new BusinessException(BusinessErrorEnum.MESSAGE_NOT_FOUND);
        }
        if (req.getContent() != null && req.getContent().length() > 4000) {
            throw new BusinessException(BusinessErrorEnum.MESSAGE_NOT_FOUND);
        }
    }

    @Override
    protected void saveMsg(Message message, SendMsgReq req) {
        // 保存文本内容（可能为空字符串，当纯文件消息时）
        String filtered = req.getContent() != null ? sensitiveWordBs.filter(req.getContent()) : "";
        message.setContent(filtered);
        // 保存文件附件 ID 到 extra 字段
        List<Long> fileIds = req.getFileIds();
        if (fileIds != null && !fileIds.isEmpty()) {
            message.setExtra(new MessageExtra(fileIds, null));
        }
    }
}
