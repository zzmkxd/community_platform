package com.community.user.controller;

import com.community.common.domain.vo.UserVO;
import com.community.user.domain.dto.LoginReq;
import com.community.user.service.AuthService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/auth")
@RequiredArgsConstructor
@Hidden
@Tag(name = "内部鉴权（Feign）")
@Validated
public class InternalAuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public UserVO login(@Valid @RequestBody LoginReq req) {
        return authService.login(req);
    }

    @PostMapping("/refresh")
    public String refreshToken(@NotBlank @RequestParam("token") String token) {
        return authService.refreshToken(token);
    }

    @GetMapping("/valid-uid")
    public Long getValidUid(@NotBlank @RequestParam("token") String token) {
        return authService.getValidUid(token);
    }

    @GetMapping("/verify")
    public boolean verify(@NotBlank @RequestParam("token") String token) {
        return authService.verify(token);
    }

    @PostMapping("/renewal")
    public void renewalTokenIfNecessary(@NotBlank @RequestParam("token") String token) {
        authService.renewalTokenIfNecessary(token);
    }
}
