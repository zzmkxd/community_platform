package com.community.user.service.impl;

import com.community.user.domain.vo.UserVO;
import com.community.user.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    @Override
    public UserVO getMe() {
        // TODO: 从 RequestHolder 获取当前用户
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public UserVO updateMe(String nickname, String avatar, String email) {
        // TODO: 更新用户资料
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public UserVO getUserById(Long id) {
        // TODO: 查询用户公开信息
        throw new UnsupportedOperationException("TODO");
    }
}
