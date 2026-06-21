package com.community.user.service;

import com.community.user.domain.dto.LoginReq;
import com.community.user.domain.vo.UserVO;

public interface AuthService {

    /** 登录（seed data 测试通道）：返回 userId + token + nickname */
    UserVO login(LoginReq req);

    /** 刷新 token */
    String refreshToken(String token);

    /** Token 校验：返回有效 uid，无效返回 null */
    Long getValidUid(String token);

    /** 直接按 uid 签发 token（微信扫码登录用） */
    String login(Long uid);

    /** 异步续期：剩余不足 2 天时刷新 Redis TTL 到 5 天 */
    void renewalTokenIfNecessary(String token);

    /** 校验 token 是否有效 */
    boolean verify(String token);
}
