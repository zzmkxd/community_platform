package com.community.user.consumer;

import com.community.common.constant.MQConstant;
import com.community.common.domain.dto.ScanSuccessMessageDTO;
import com.community.websocket.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@RocketMQMessageListener(
        consumerGroup = MQConstant.SCAN_MSG_GROUP,
        topic = MQConstant.SCAN_MSG_TOPIC,
        messageModel = MessageModel.BROADCASTING)
@Component
@RequiredArgsConstructor
public class ScanSuccessConsumer implements RocketMQListener<ScanSuccessMessageDTO> {

    private final WebSocketService webSocketService;

    @Override
    public void onMessage(ScanSuccessMessageDTO scanSuccessMessageDTO) {
        webSocketService.scanSuccess(scanSuccessMessageDTO.getCode());
    }
}
