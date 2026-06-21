package com.community.user.controller;

import com.community.common.domain.vo.response.ApiResult;
import com.community.user.domain.dto.AccountBindReq;
import com.community.user.domain.vo.UserVO;
import com.community.user.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "用户")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResult<UserVO> getMe() {
        return ApiResult.success(userService.getMe());
    }

    @PutMapping("/me")
    public ApiResult<UserVO> updateMe(@RequestBody UserVO body) {
        return ApiResult.success(userService.updateMe(
                body.getNickname(), body.getAvatar(), body.getEmail()));
    }

    @PostMapping("/me/bind-wx")
    public ApiResult<Void> bindWeChat(@RequestBody AccountBindReq req) {
        userService.bindWeChat(req);
        return ApiResult.success(null);
    }

    @GetMapping("/{id}")
    public ApiResult<UserVO> getUser(@PathVariable Long id) {
        return ApiResult.success(userService.getUserById(id));
    }
}
