package com.community.user.service;

import com.community.user.domain.dto.AccountBindReq;
import com.community.user.domain.vo.UserVO;

import java.util.List;

public interface UserService {

    UserVO getMe();

    UserVO updateMe(String nickname, String avatar, String email);

    UserVO bindWeChat(AccountBindReq req);

    UserVO getUserById(Long id);

    /**
     * 批量查询用户信息
     * @param ids 用户 ID 列表
     * @return 用户 VO 列表
     */
    List<UserVO> getBatchUsers(List<Long> ids);
}
