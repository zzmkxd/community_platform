package com.community.message.event;

import cn.hutool.json.JSONUtil;
import com.community.common.constant.MQConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class MessageSendListener {

    private final RocketMQTemplate rocketMQTemplate;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageSend(ChannelMessageSendEvent event) {
        log.info("Message sent: msgId={}, channelId={}, threadId={}, fromUid={}",
                event.getMessageId(), event.getChannelId(), event.getThreadId(), event.getFromUid());
        String body = JSONUtil.toJsonStr(JSONUtil.createObj()
                .set("messageId", event.getMessageId())
                .set("channelId", event.getChannelId())
                .set("threadId", event.getThreadId())
                .set("fromUid", event.getFromUid()));
        rocketMQTemplate.send(MQConstant.SEND_MSG_TOPIC,
                MessageBuilder.withPayload(body).build());
    }
}
