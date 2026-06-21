package com.community.user.service.impl;

import com.community.common.utils.JwtUtils;
import com.community.user.domain.dto.LoginReq;
import com.community.user.domain.dto.RegisterReq;
import com.community.user.domain.vo.UserVO;
import com.community.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final JwtUtils jwtUtils;

    @Override
    public UserVO register(RegisterReq req) {
        // TODO: 实现注册逻辑
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public UserVO login(LoginReq req) {
        // TODO: 实现登录逻辑
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public String refreshToken(String token) {
        // TODO: 实现 token 刷新
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public Long getValidUid(String token) {
        return jwtUtils.getUidOrNull(token);
    }
}
