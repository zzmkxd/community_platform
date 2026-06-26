package com.community.user.controller;

import com.community.common.domain.vo.response.ApiResult;
import com.community.user.domain.dto.AccountBindReq;
import com.community.common.domain.vo.UserVO;
import com.community.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "用户")
public class UserController {

    private final UserService userService;

    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/me")
    public ApiResult<UserVO> getMe() {
        return ApiResult.success(userService.getMe());
    }

    @Operation(summary = "修改当前用户资料（昵称、头像、邮箱）")
    @PutMapping("/me")
    public ApiResult<UserVO> updateMe(@RequestBody UserVO body) {
        return ApiResult.success(userService.updateMe(
                body.getNickname(), body.getAvatar(), body.getEmail()));
    }

    @Operation(summary = "绑定微信账号")
    @PostMapping("/me/bind-wx")
    public ApiResult<UserVO> bindWeChat(@RequestBody AccountBindReq req) {
        return ApiResult.success(userService.bindWeChat(req));
    }

    @Operation(summary = "根据用户 ID 获取用户公开信息")
    @GetMapping("/{id}")
    public ApiResult<UserVO> getUser(@PathVariable Long id) {
        return ApiResult.success(userService.getUserById(id));
    }
}
