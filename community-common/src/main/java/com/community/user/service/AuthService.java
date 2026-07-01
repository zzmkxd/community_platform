package com.community.user.service;

import com.community.user.domain.dto.LoginReq;
import com.community.common.domain.vo.UserVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "community-user-service", contextId = "authService", path = "/internal/auth")
public interface AuthService {

    /** 登录（seed data 测试通道）：返回 userId + token + nickname */
    @PostMapping("/login")
    UserVO login(@RequestBody LoginReq req);

    /** 刷新 token */
    @PostMapping("/refresh")
    String refreshToken(@RequestParam("token") String token);

    /** Token 校验：返回有效 uid，无效返回 null */
    @GetMapping("/valid-uid")
    Long getValidUid(@RequestParam("token") String token);

    /** 异步续期：剩余不足 2 天时刷新 Redis TTL 到 5 天 */
    @PostMapping("/renewal")
    void renewalTokenIfNecessary(@RequestParam("token") String token);

    /** 校验 token 是否有效 */
    @GetMapping("/verify")
    boolean verify(@RequestParam("token") String token);
}
