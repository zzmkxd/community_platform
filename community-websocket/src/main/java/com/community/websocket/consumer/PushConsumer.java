package com.community.websocket.consumer;

import cn.hutool.json.JSONUtil;
import com.community.common.constant.MQConstant;
import com.community.common.domain.dto.PushMessageDTO;
import com.community.websocket.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@RocketMQMessageListener(
        topic = MQConstant.PUSH_TOPIC,
        consumerGroup = MQConstant.PUSH_CONSUMER_GROUP
)
public class PushConsumer implements RocketMQListener<String> {

    private final WebSocketService webSocketService;

    @Override
    public void onMessage(String msg) {
        PushMessageDTO dto;
        try {
            dto = JSONUtil.toBean(msg, PushMessageDTO.class);
        } catch (Exception e) {
            log.error("PushConsumer JSON parse failed, msg discarded: {}", msg, e);
            return;
        }
        switch (dto.getTargetType()) {
            case "channel" -> webSocketService.pushToChannel(dto.getTargetId(), dto.getData());
            case "thread" -> webSocketService.pushToThread(dto.getTargetId(), dto.getData());
            case "user" -> webSocketService.pushToUser(dto.getTargetId(), dto.getData());
            default -> log.warn("Unknown push targetType: {}", dto.getTargetType());
        }
    }
}
