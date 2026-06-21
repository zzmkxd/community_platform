package com.community.websocket.consumer;

import cn.hutool.json.JSONUtil;
import com.community.common.constant.MQConstant;
import com.community.websocket.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * RocketMQ 消费 → WS 推送消费者
 * 接收 PushMessageDTO，根据 targetType (channel/thread/user) 分发推送
 */
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
    public void onMessage(String message) {
        try {
            // TODO: 解析 PushMessageDTO，调用 webSocketService.pushToChannel/Thread/User
            log.debug("Push message received: {}", message);
        } catch (Exception e) {
            log.error("Push consumer error", e);
        }
    }
}
