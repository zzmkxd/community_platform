package com.community.user.service;

import com.community.user.domain.dto.AccountBindReq;
import com.community.user.domain.vo.UserVO;

public interface UserService {

    UserVO getMe();

    UserVO updateMe(String nickname, String avatar, String email);

    void bindWeChat(AccountBindReq req);

    UserVO getUserById(Long id);
}
