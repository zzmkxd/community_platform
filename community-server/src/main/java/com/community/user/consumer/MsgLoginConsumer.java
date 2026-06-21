package com.community.user.consumer;

import com.community.common.constant.MQConstant;
import com.community.common.domain.dto.LoginMessageDTO;
import com.community.websocket.service.WebSocketService;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.spring.annotation.MessageModel;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@RocketMQMessageListener(
        consumerGroup = MQConstant.LOGIN_MSG_GROUP,
        topic = MQConstant.LOGIN_MSG_TOPIC,
        messageModel = MessageModel.BROADCASTING)
@Component
@RequiredArgsConstructor
public class MsgLoginConsumer implements RocketMQListener<LoginMessageDTO> {

    private final WebSocketService webSocketService;

    @Override
    public void onMessage(LoginMessageDTO loginMessageDTO) {
        webSocketService.scanLoginSuccess(loginMessageDTO.getCode(), loginMessageDTO.getUid());
    }
}
