package com.community.user.controller;

import com.community.common.domain.vo.response.ApiResult;
import com.community.user.domain.dto.LoginReq;
import com.community.common.domain.vo.UserVO;
import com.community.user.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "鉴权")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "用户登录，返回 JWT Token 和用户信息")
    @PostMapping("/login")
    public ApiResult<UserVO> login(@Valid @RequestBody LoginReq req) {
        return ApiResult.success(authService.login(req));
    }

    @Operation(summary = "刷新 Token，延长登录有效期")
    @PostMapping("/refresh")
    public ApiResult<String> refresh(@RequestBody Map<String, String> body) {
        return ApiResult.success(authService.refreshToken(body.get("token")));
    }
}
