package com.community.message.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
public class MessageSendListener {

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessageSend(ChannelMessageSendEvent event) {
        log.info("Message sent: msgId={}, channelId={}, threadId={}, fromUid={}",
                event.getMessageId(), event.getChannelId(), event.getThreadId(), event.getFromUid());
        // TODO Phase 4.5+: RocketMQ → MsgSendConsumer → PushService → WebSocket push
    }
}
