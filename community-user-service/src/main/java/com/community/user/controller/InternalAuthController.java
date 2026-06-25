package com.community.user.controller;

import com.community.common.domain.vo.UserVO;
import com.community.user.domain.dto.LoginReq;
import com.community.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

// ponytail: 独立 @RestController，不实现 @FeignClient 接口，避免 Spring Cloud 误判为 Feign fallback 导致端点不注册
@RestController
@RequestMapping("/internal/auth")
@RequiredArgsConstructor
public class InternalAuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public UserVO login(@RequestBody LoginReq req) {
        return authService.login(req);
    }

    @PostMapping("/refresh")
    public String refreshToken(@RequestParam("token") String token) {
        return authService.refreshToken(token);
    }

    @GetMapping("/valid-uid")
    public Long getValidUid(@RequestParam("token") String token) {
        return authService.getValidUid(token);
    }

    @GetMapping("/verify")
    public boolean verify(@RequestParam("token") String token) {
        return authService.verify(token);
    }

    @PostMapping("/renewal")
    public void renewalTokenIfNecessary(@RequestParam("token") String token) {
        authService.renewalTokenIfNecessary(token);
    }
}
