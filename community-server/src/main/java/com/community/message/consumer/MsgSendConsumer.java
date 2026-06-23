package com.community.message.consumer;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.community.common.constant.MQConstant;
import com.community.file.dao.FileAttachmentDao;
import com.community.file.domain.entity.FileAttachment;
import com.community.message.dao.MessageDao;
import com.community.message.dao.ThreadDao;
import com.community.message.domain.entity.Message;
import com.community.message.domain.entity.MessageExtra;
import com.community.message.domain.entity.Thread;
import com.community.message.domain.vo.FileVO;
import com.community.message.domain.vo.MessageVO;
import com.community.message.service.PushService;
import com.community.message.service.adapter.MessageAdapter;
import com.community.user.dao.UserDao;
import com.community.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = MQConstant.SEND_MSG_TOPIC,
        consumerGroup = MQConstant.MSG_SEND_CONSUMER_GROUP
)
public class MsgSendConsumer implements RocketMQListener<String> {

    private final MessageDao messageDao;
    private final ThreadDao threadDao;
    private final UserDao userDao;
    private final PushService pushService;
    private final FileAttachmentDao fileAttachmentDao;

    @Override
    public void onMessage(String msg) {
        try {
            JSONObject body = JSONUtil.parseObj(msg);
            Long messageId = body.getLong("messageId");
            Long channelId = body.getLong("channelId");
            Long threadId = body.getLong("threadId");
            Long fromUid = body.getLong("fromUid");

            Message message = messageDao.getById(messageId);
            if (message == null) {
                log.warn("MsgSendConsumer: message not found, msgId={}", messageId);
                return;
            }
            User fromUser = userDao.getById(fromUid);

            Thread thread = null;
            if (threadId != null) {
                thread = threadDao.getById(threadId);
            }

            MessageVO messageVO = MessageAdapter.buildMessageVO(message, fromUser, null, thread);

            // 填充附件列表
            messageVO.setAttachments(MessageAdapter.buildAttachments(message, fileAttachmentDao));

            // 推送到频道订阅者
            pushService.pushToChannel(channelId, fromUid, messageVO);

            // 如果属于 Thread，也推送给 Thread 订阅者
            if (threadId != null) {
                pushService.pushToThread(threadId, messageVO);
            }

            log.info("MsgSendConsumer dispatched: msgId={}, channelId={}, threadId={}", messageId, channelId, threadId);
        } catch (Exception e) {
            log.error("MsgSendConsumer error: {}", msg, e);
        }
    }

}
