package com.community.message.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class ChannelMessageSendEvent extends ApplicationEvent {

    private final Long messageId;
    private final Long channelId;
    private final Long threadId;
    private final Long fromUid;

    public ChannelMessageSendEvent(Object source, Long messageId, Long channelId, Long threadId, Long fromUid) {
        super(source);
        this.messageId = messageId;
        this.channelId = channelId;
        this.threadId = threadId;
        this.fromUid = fromUid;
    }
}
