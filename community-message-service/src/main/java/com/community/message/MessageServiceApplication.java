package com.community.message;

import com.community.file.service.FileService;
import com.community.server.service.ChannelService;
import com.community.server.service.PermissionService;
import com.community.user.service.UserService;
import com.community.websocket.service.PushService;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

// ponytail: 只列出需要远程调用的 Feign 客户端，本地实现的接口不列入
@SpringBootApplication(scanBasePackages = {"com.community.message", "com.community.common"})
@MapperScan({"com.community.message.dao.mapper", "com.community.common.sensitive.dao.mapper"})
@EnableFeignClients(clients = {UserService.class, FileService.class, PushService.class,
                                ChannelService.class, PermissionService.class})
public class MessageServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MessageServiceApplication.class, args);
    }
}
