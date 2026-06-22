package com.community.message.service;

public interface ChannelReadStateService {

    void updateReadState(Long channelId, Long lastReadMsgId);

    java.util.Map<Long, Long> getUnreadCounts(Long serverId);
}
