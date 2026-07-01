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
        JSONObject body;
        try {
            body = JSONUtil.parseObj(msg);
        } catch (Exception e) {
            log.error("MsgSendConsumer JSON parse failed, msg discarded: {}", msg, e);
            return;
        }

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
        if (fromUser == null) {
            log.warn("MsgSendConsumer: user not found, uid={}, msgId={}", fromUid, messageId);
            return;
        }

        Thread thread = null;
        if (threadId != null) {
            thread = threadDao.getById(threadId);
        }

        MessageVO messageVO = MessageAdapter.buildMessageVO(message, fromUser, null, thread);
        messageVO.setAttachments(buildAttachments(message));

        pushService.pushToChannel(channelId, fromUid, messageVO);
        if (threadId != null) {
            pushService.pushToThread(threadId, messageVO);
        }

        log.info("MsgSendConsumer dispatched: msgId={}, channelId={}, threadId={}", messageId, channelId, threadId);

        indexMessage(message, channelId);
    }

    private List<FileVO> buildAttachments(Message message) {
        MessageExtra extra = message.getExtra();
        if (extra == null || extra.getFileIds() == null || extra.getFileIds().isEmpty()) {
            return null;
        }
        List<FileVO> files = fileService.getFiles(extra.getFileIds());
        return files.isEmpty() ? null : files;
    }

    private void indexMessage(Message message, Long channelId) {
        try {
            ChannelVO channel = channelService.getById(channelId);
            if (channel == null) {
                log.error("ES index SKIPPED — channel not found! channelId={}, msgId={}", channelId, message.getId());
                return;
            }
            MessageDocument doc = MessageDocument.from(message, channel.getServerId());
            esOps.save(doc);
            log.debug("ES indexed: msgId={}, serverId={}", message.getId(), channel.getServerId());
        } catch (Exception e) {
            log.error("ES index FAILED (async consumer) — search will miss this message! msgId={}, channelId={}, error={}",
                    message.getId(), channelId, e.toString());
        }
    }
}
