package com.community.user.service;

import com.community.user.domain.dto.AccountBindReq;
import com.community.common.domain.vo.UserVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "community-user-service", contextId = "userService", path = "/internal/user")
public interface UserService {

    @GetMapping("/me")
    UserVO getMe();

    @PutMapping("/me")
    UserVO updateMe(@RequestParam("nickname") String nickname,
                    @RequestParam("avatar") String avatar,
                    @RequestParam("email") String email);

    @PostMapping("/bind-wechat")
    UserVO bindWeChat(@RequestBody AccountBindReq req);

    @GetMapping("/{id}")
    UserVO getUserById(@PathVariable("id") Long id);

    /**
     * 批量查询用户信息
     * @param ids 用户 ID 列表
     * @return 用户 VO 列表
     */
    @PostMapping("/batch")
    List<UserVO> getBatchUsers(@RequestBody List<Long> ids);
}
