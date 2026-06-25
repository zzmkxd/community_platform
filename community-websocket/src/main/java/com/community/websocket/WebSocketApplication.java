package com.community.websocket;

import com.community.server.service.MemberService;
import com.community.user.service.UserService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.cloud.openfeign.EnableFeignClients;

// ponytail: websocket 纯推送服务，无 DB，排除 DataSource 及 MyBatis 相关自动配置
@SpringBootApplication(
    scanBasePackages = {"com.community.websocket", "com.community.common"},
    exclude = {DataSourceAutoConfiguration.class}
)
@EnableFeignClients(clients = {UserService.class, MemberService.class})
public class WebSocketApplication {
    public static void main(String[] args) {
        SpringApplication.run(WebSocketApplication.class, args);
    }
}
