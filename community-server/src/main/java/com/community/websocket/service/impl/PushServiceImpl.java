package com.community.websocket.service.impl;

import cn.hutool.json.JSONUtil;
import com.community.common.constant.MQConstant;
import com.community.common.domain.dto.PushMessageDTO;
import com.community.server.service.MemberService;
import com.community.websocket.service.PushService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class PushServiceImpl implements PushService {

    private final RocketMQTemplate rocketMQTemplate;
    private final MemberService memberService;

    // ponytail: @Lazy 必须在构造参数上才能打破循环依赖，Lombok @RequiredArgsConstructor 不会传递字段上的 @Lazy
    // 微服务拆分后 PushService → Feign → MemberService，循环自然消除
    public PushServiceImpl(RocketMQTemplate rocketMQTemplate,
                           @Lazy MemberService memberService) {
        this.rocketMQTemplate = rocketMQTemplate;
        this.memberService = memberService;
    }

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
