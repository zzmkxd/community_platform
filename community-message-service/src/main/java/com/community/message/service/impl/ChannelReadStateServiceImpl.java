package com.community.message.service.impl;

import com.community.common.utils.RequestHolder;
import com.community.message.dao.ChannelReadStateDao;
import com.community.message.domain.entity.ChannelReadState;
import com.community.message.service.ChannelReadStateService;
import com.community.server.domain.vo.ChannelVO;
import com.community.server.service.ChannelService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

// ponytail: @RestController 让 Feign 接口的 REST 端点生效
// 注意：参数注解不随 @Override 继承，必须显式声明
@Slf4j
@RestController
@RequestMapping("/internal/read-state")
@RequiredArgsConstructor
public class ChannelReadStateServiceImpl implements ChannelReadStateService {

    private final ChannelReadStateDao channelReadStateDao;
    private final ChannelService channelService;
    private final com.community.message.dao.MessageDao messageDao;

    @Override
    @Transactional
    @PutMapping("/channel/{channelId}")
    public void updateReadState(@PathVariable("channelId") Long channelId,
                                @RequestParam("lastReadMsgId") Long lastReadMsgId) {
        Long uid = RequestHolder.get().getUid();

        ChannelReadState state = channelReadStateDao.lambdaQuery()
                .eq(ChannelReadState::getUserId, uid)
                .eq(ChannelReadState::getChannelId, channelId)
                .one();

        if (state == null) {
            state = new ChannelReadState();
            state.setUserId(uid);
            state.setChannelId(channelId);
            state.setLastReadMsgId(lastReadMsgId);
            channelReadStateDao.save(state);
        } else if (lastReadMsgId > state.getLastReadMsgId()) {
            state.setLastReadMsgId(lastReadMsgId);
            channelReadStateDao.updateById(state);
        }
    }

    @Override
    @GetMapping("/server/{serverId}/unread")
    public Map<Long, Long> getUnreadCounts(@PathVariable("serverId") Long serverId) {
        Long uid = RequestHolder.get().getUid();

        List<ChannelVO> channels = channelService.listByServerId(serverId);

        Map<Long, Long> unreadCounts = new HashMap<>();
        for (ChannelVO channel : channels) {
            ChannelReadState state = channelReadStateDao.lambdaQuery()
                    .eq(ChannelReadState::getUserId, uid)
                    .eq(ChannelReadState::getChannelId, channel.getId())
                    .one();

            long lastReadId = state != null ? state.getLastReadMsgId() : 0L;
            long unread = messageDao.lambdaQuery()
                    .eq(com.community.message.domain.entity.Message::getChannelId, channel.getId())
                    .gt(com.community.message.domain.entity.Message::getId, lastReadId)
                    .ne(com.community.message.domain.entity.Message::getStatus, 1)
                    .count();

            if (unread > 0) {
                unreadCounts.put(channel.getId(), unread);
            }
        }
        return unreadCounts;
    }
}
