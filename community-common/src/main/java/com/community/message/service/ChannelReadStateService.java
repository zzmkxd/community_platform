package com.community.message.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(name = "community-message-service", path = "/internal/read-state")
public interface ChannelReadStateService {

    @PutMapping("/channel/{channelId}")
    void updateReadState(@PathVariable("channelId") Long channelId,
                         @RequestParam("lastReadMsgId") Long lastReadMsgId);

    @GetMapping("/server/{serverId}/unread")
    Map<Long, Long> getUnreadCounts(@PathVariable("serverId") Long serverId);
}
