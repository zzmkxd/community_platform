package com.community.user.service;

import com.community.user.domain.dto.LoginReq;
import com.community.user.domain.dto.RegisterReq;
import com.community.user.domain.vo.UserVO;

public interface AuthService {

    /** 注册：返回 userId + token */
    UserVO register(RegisterReq req);

    /** 登录：返回 userId + token + nickname */
    UserVO login(LoginReq req);

    /** 刷新 token */
    String refreshToken(String token);

    /** Token 校验：返回有效 uid，无效返回 null */
    Long getValidUid(String token);
}
