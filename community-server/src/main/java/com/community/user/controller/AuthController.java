package com.community.user.controller;

import com.community.common.domain.vo.response.ApiResult;
import com.community.user.domain.dto.LoginReq;
import com.community.user.domain.dto.RegisterReq;
import com.community.user.domain.vo.UserVO;
import com.community.user.service.AuthService;
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

    @PostMapping("/register")
    public ApiResult<UserVO> register(@Valid @RequestBody RegisterReq req) {
        return ApiResult.success(authService.register(req));
    }

    @PostMapping("/login")
    public ApiResult<UserVO> login(@Valid @RequestBody LoginReq req) {
        return ApiResult.success(authService.login(req));
    }

    @PostMapping("/refresh")
    public ApiResult<String> refresh(@RequestBody Map<String, String> body) {
        return ApiResult.success(authService.refreshToken(body.get("token")));
    }
}
