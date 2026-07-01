package com.community.common.transaction.service;

import com.community.common.transaction.annotation.SecureInvoke;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

@RequiredArgsConstructor
public class MQProducer {

    @Autowired
    @Lazy
    private RocketMQTemplate rocketMQTemplate;

    public void sendMsg(String topic, Object body) {
        rocketMQTemplate.syncSend(topic, body);
    }

    /**
     * 发送可靠消息 — 事务提交后保证发送成功，失败自动重试
     */
    @SecureInvoke
    public void sendSecureMsg(String topic, Object body) {
        rocketMQTemplate.syncSend(topic, body);
    }
}
