package com.community.message.service.strategy.msg;

import cn.hutool.json.JSONUtil;
import com.community.common.exception.BusinessErrorEnum;
import com.community.common.exception.BusinessException;
import com.community.file.dao.FileAttachmentDao;
import com.community.file.domain.entity.FileAttachment;
import com.community.file.domain.enums.FileStatusEnum;
import com.community.message.domain.dto.SendMsgReq;
import com.community.message.domain.entity.Message;
import com.community.message.domain.enums.MessageTypeEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class FileMsgHandler extends AbstractMsgHandler {

    @Autowired
    private FileAttachmentDao fileAttachmentDao;

    @Override
    protected MessageTypeEnum getMsgType() {
        return MessageTypeEnum.FILE;
    }

    @Override
    protected void checkMsg(SendMsgReq req, Long channelId, Long uid) {
        if (req.getFileIds() == null || req.getFileIds().isEmpty()) {
            throw new BusinessException(BusinessErrorEnum.FILE_NOT_FOUND);
        }
        for (Long fileId : req.getFileIds()) {
            FileAttachment file = fileAttachmentDao.lambdaQuery()
                    .eq(FileAttachment::getId, fileId).one();
            if (file == null || !FileStatusEnum.UPLOADED.getStatus().equals(file.getStatus())) {
                throw new BusinessException(BusinessErrorEnum.FILE_UPLOAD_FAILED);
            }
        }
    }

    @Override
    protected void saveMsg(Message message, SendMsgReq req) {
        message.setExtra(JSONUtil.toJsonStr(new ExtraBody(req.getFileIds())));
    }

    static class ExtraBody {
        private List<Long> fileIds;
        ExtraBody(List<Long> fileIds) { this.fileIds = fileIds; }
        public List<Long> getFileIds() { return fileIds; }
        public void setFileIds(List<Long> fileIds) { this.fileIds = fileIds; }
    }
}
