package com.community.user.service.impl;

import com.community.common.constant.RedisKey;
import com.community.common.exception.BusinessErrorEnum;
import com.community.common.exception.BusinessException;
import com.community.common.utils.JwtUtils;
import com.community.common.utils.RedisUtils;
import com.community.user.dao.UserDao;
import com.community.user.domain.dto.LoginReq;
import com.community.user.domain.entity.User;
import com.community.common.domain.vo.UserVO;
import com.community.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.TimeUnit;

// ponytail: @RestController 让 Feign 接口的 REST 端点生效
// 注意：参数注解（@RequestBody/@RequestParam）不随 @Override 继承，必须显式声明
@Slf4j
@RestController
@RequestMapping("/internal/auth")
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final long TOKEN_TTL_SECONDS = TimeUnit.DAYS.toSeconds(5);

    private final UserDao userDao;
    private final JwtUtils jwtUtils;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    @PostMapping("/login")
    public UserVO login(@RequestBody LoginReq req) {
        User user = userDao.lambdaQuery()
                .eq(User::getUsername, req.getUsername())
                .oneOpt()
                .orElseThrow(() -> new BusinessException(BusinessErrorEnum.USER_NOT_FOUND));

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BusinessException(BusinessErrorEnum.PASSWORD_ERROR);
        }

        String token = jwtUtils.createToken(user.getId());
        RedisUtils.set(RedisKey.TOKEN_PREFIX + user.getId(), token, TOKEN_TTL_SECONDS);

        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setToken(token);
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setEmail(user.getEmail());
        return vo;
    }

    @Override
    @PostMapping("/refresh")
    public String refreshToken(@RequestParam("token") String token) {
        Long uid = jwtUtils.getUidOrNull(token);
        if (uid == null) {
            throw new BusinessException(BusinessErrorEnum.TOKEN_INVALID);
        }
        String newToken = jwtUtils.createToken(uid);
        RedisUtils.set(RedisKey.TOKEN_PREFIX + uid, newToken, TOKEN_TTL_SECONDS);
        return newToken;
    }

    @Override
    @GetMapping("/valid-uid")
    public Long getValidUid(@RequestParam("token") String token) {
        Long uid = jwtUtils.getUidOrNull(token);
        if (uid == null) {
            return null;
        }
        String stored = RedisUtils.get(RedisKey.TOKEN_PREFIX + uid);
        return token.equals(stored) ? uid : null;
    }

    @Override
    @GetMapping("/verify")
    public boolean verify(@RequestParam("token") String token) {
        Long uid = jwtUtils.getUidOrNull(token);
        if (uid == null) {
            return false;
        }
        String stored = RedisUtils.get(RedisKey.TOKEN_PREFIX + uid);
        return token.equals(stored);
    }

    @Async
    @Override
    @PostMapping("/renewal")
    public void renewalTokenIfNecessary(@RequestParam("token") String token) {
        Long uid = jwtUtils.getUidOrNull(token);
        if (uid == null) {
            return;
        }
        String key = RedisKey.TOKEN_PREFIX + uid;
        long expireDays = RedisUtils.getExpire(key, TimeUnit.DAYS);
        if (expireDays == -2) {
            return;
        }
        if (expireDays < 2) {
            RedisUtils.expire(key, TimeUnit.DAYS.toSeconds(5));
        }
    }
}
