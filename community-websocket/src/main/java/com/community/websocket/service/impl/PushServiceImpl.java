package com.community.websocket.service.impl;

import cn.hutool.json.JSONUtil;
import com.community.common.constant.MQConstant;
import com.community.common.domain.dto.PushMessageDTO;
import com.community.server.service.MemberService;
import com.community.websocket.service.PushService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// ponytail: @RestController 让 Feign 接口的 REST 端点生效
// 注意：参数注解不随 @Override 继承，必须显式声明
// 拆分后 MemberService 通过 Feign 调用，@Lazy 不再需要
@Slf4j
@RestController
@RequestMapping("/internal/push")
public class PushServiceImpl implements PushService {

    private final RocketMQTemplate rocketMQTemplate;
    private final MemberService memberService;

    public PushServiceImpl(RocketMQTemplate rocketMQTemplate, MemberService memberService) {
        this.rocketMQTemplate = rocketMQTemplate;
        this.memberService = memberService;
    }

    @Override
    @PostMapping("/channel/{channelId}")
    public void pushToChannel(@PathVariable("channelId") Long channelId,
                              @RequestParam(value = "excludeUid", required = false) Long excludeUid,
                              @RequestBody Object data) {
        PushMessageDTO dto = PushMessageDTO.builder()
                .targetType("channel")
                .targetId(channelId)
                .data(JSONUtil.toJsonStr(data))
                .build();
        send(dto);
    }

    @Override
    @PostMapping("/thread/{threadId}")
    public void pushToThread(@PathVariable("threadId") Long threadId, @RequestBody Object data) {
        PushMessageDTO dto = PushMessageDTO.builder()
                .targetType("thread")
                .targetId(threadId)
                .data(JSONUtil.toJsonStr(data))
                .build();
        send(dto);
    }

    @Override
    @PostMapping("/user/{userId}")
    public void pushToUser(@PathVariable("userId") Long userId, @RequestBody Object data) {
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
    @PostMapping("/server/{serverId}")
    public void pushToServer(@PathVariable("serverId") Long serverId,
                             @RequestParam(value = "excludeUid", required = false) Long excludeUid,
                             @RequestBody Object data) {
        List<Long> uids = memberService.getServerMemberUids(serverId, excludeUid);
        for (Long uid : uids) {
            pushToUser(uid, data);
        }
    }
}
