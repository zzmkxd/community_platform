package com.community.websocket.service.impl;

import cn.hutool.json.JSONUtil;
import com.community.common.constant.MQConstant;
import com.community.common.domain.dto.PushMessageDTO;
import com.community.server.service.MemberService;
import com.community.websocket.service.PushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushServiceImpl implements PushService {

    private final RocketMQTemplate rocketMQTemplate;
    private final MemberService memberService;

    @Override
    public void pushToChannel(Long channelId, Long excludeUid, Object data) {
        PushMessageDTO dto = PushMessageDTO.builder()
                .targetType("channel")
                .targetId(channelId)
                .data(JSONUtil.toJsonStr(data))
                .build();
        send(dto);
    }

    @Override
    public void pushToThread(Long threadId, Object data) {
        PushMessageDTO dto = PushMessageDTO.builder()
                .targetType("thread")
                .targetId(threadId)
                .data(JSONUtil.toJsonStr(data))
                .build();
        send(dto);
    }

    @Override
    public void pushToUser(Long userId, Object data) {
        PushMessageDTO dto = PushMessageDTO.builder()
                .targetType("user")
                .targetId(userId)
                .data(JSONUtil.toJsonStr(data))
                .build();
        send(dto);
    }

    private void send(PushMessageDTO dto) {
        String body = JSONUtil.toJsonStr(dto);
        rocketMQTemplate.send(MQConstant.PUSH_TOPIC,
                MessageBuilder.withPayload(body).build());
        log.debug("PUSH_TOPIC sent: targetType={}, targetId={}", dto.getTargetType(), dto.getTargetId());
    }

    @Override
    public void pushToServer(Long serverId, Long excludeUid, Object data) {
        List<Long> uids = memberService.getServerMemberUids(serverId, excludeUid);
        for (Long uid : uids) {
            pushToUser(uid, data);
        }
    }
}
