package com.community.user.service;

import com.community.user.domain.vo.UserVO;

public interface UserService {

    UserVO getMe();

    UserVO updateMe(String nickname, String avatar, String email);

    UserVO getUserById(Long id);
}
