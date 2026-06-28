package com.community.server;

import com.community.message.service.ChannelReadStateService;
import com.community.user.service.UserService;
import com.community.websocket.service.PushService;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

// ponytail: 只列出需要远程调用的 Feign 客户端，本地实现的接口不列入
@SpringBootApplication(scanBasePackages = {"com.community.server", "com.community.common"})
@MapperScan({"com.community.server.dao.mapper", "com.community.common.sensitive.dao.mapper"})
@EnableFeignClients(clients = {UserService.class, PushService.class, ChannelReadStateService.class})
public class ServerServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ServerServiceApplication.class, args);
    }
}
