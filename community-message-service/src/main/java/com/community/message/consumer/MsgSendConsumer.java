package com.community.message.consumer;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.community.common.constant.MQConstant;
import com.community.file.service.FileService;
import com.community.server.domain.vo.ChannelVO;
import com.community.server.service.ChannelService;

import com.community.message.dao.MessageDao;
import com.community.message.dao.ThreadDao;
import com.community.message.domain.document.MessageDocument;
import com.community.message.domain.entity.Message;
import com.community.message.domain.entity.MessageExtra;
import com.community.message.domain.entity.Thread;
import com.community.file.domain.vo.FileVO;
import com.community.message.domain.vo.MessageVO;
import com.community.websocket.service.PushService;
import com.community.message.service.adapter.MessageAdapter;
import com.community.common.domain.vo.UserVO;
import com.community.user.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final UserService userService;
    private final PushService pushService;
    private final FileService fileService;
    private final ChannelService channelService;
    private final ElasticsearchOperations esOps;

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
            UserVO fromUser = userService.getUserById(fromUid);

            Thread thread = null;
            if (threadId != null) {
                thread = threadDao.getById(threadId);
            }

            MessageVO messageVO = MessageAdapter.buildMessageVO(message, fromUser, null, thread);

            // 填充附件列表
            messageVO.setAttachments(buildAttachments(message));

            // 推送到频道订阅者
            pushService.pushToChannel(channelId, fromUid, messageVO);

            // 如果属于 Thread，也推送给 Thread 订阅者
            if (threadId != null) {
                pushService.pushToThread(threadId, messageVO);
            }

            log.info("MsgSendConsumer dispatched: msgId={}, channelId={}, threadId={}", messageId, channelId, threadId);

            // ponytail: async ES index, failure logged but doesn't block push
            indexMessage(message, channelId);
        } catch (Exception e) {
            log.error("MsgSendConsumer error: {}", msg, e);
        }
    }

    private List<FileVO> buildAttachments(Message message) {
        MessageExtra extra = message.getExtra();
        if (extra == null || extra.getFileIds() == null || extra.getFileIds().isEmpty()) {
            return null;
        }
        List<FileVO> files = fileService.getFiles(extra.getFileIds());
        return files.isEmpty() ? null : files;
    }

    // ponytail: ES index failure logged, not re-thrown — push already succeeded
    private void indexMessage(Message message, Long channelId) {
        try {
            ChannelVO channel = channelService.getById(channelId);
            MessageDocument doc = MessageDocument.from(message, channel.getServerId());
            esOps.save(doc);
            log.debug("ES indexed: msgId={}, serverId={}", message.getId(), channel.getServerId());
        } catch (Exception e) {
            log.warn("ES index failed for msgId={}: {}", message.getId(), e.getMessage());
        }
    }
}
