package com.community.user.service.impl;

import com.community.common.constant.RedisKey;
import com.community.common.exception.BusinessErrorEnum;
import com.community.common.exception.BusinessException;
import com.community.common.utils.JwtUtils;
import com.community.common.utils.RedisUtils;
import com.community.user.dao.UserDao;
import com.community.user.domain.dto.LoginReq;
import com.community.user.domain.dto.RegisterReq;
import com.community.user.domain.entity.User;
import com.community.user.domain.vo.UserVO;
import com.community.user.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final long TOKEN_TTL_SECONDS = TimeUnit.DAYS.toSeconds(5);

    private final UserDao userDao;
    private final JwtUtils jwtUtils;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    @Transactional
    public UserVO register(RegisterReq req) {
        boolean exists = userDao.lambdaQuery()
                .eq(User::getUsername, req.getUsername())
                .exists();
        if (exists) {
            throw new BusinessException(BusinessErrorEnum.USERNAME_DUPLICATE);
        }

        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setNickname(req.getUsername());
        user.setEmail(req.getEmail());
        userDao.save(user);

        String token = jwtUtils.createToken(user.getId());
        RedisUtils.set(RedisKey.TOKEN_PREFIX + user.getId(), token, TOKEN_TTL_SECONDS);

        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setToken(token);
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        return vo;
    }

    @Override
    public UserVO login(LoginReq req) {
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
        return vo;
    }

    @Override
    public String refreshToken(String token) {
        Long uid = jwtUtils.getUidOrNull(token);
        if (uid == null) {
            throw new BusinessException(BusinessErrorEnum.TOKEN_INVALID);
        }
        RedisUtils.del(RedisKey.TOKEN_PREFIX + uid);
        String newToken = jwtUtils.createToken(uid);
        RedisUtils.set(RedisKey.TOKEN_PREFIX + uid, newToken, TOKEN_TTL_SECONDS);
        return newToken;
    }

    @Override
    public Long getValidUid(String token) {
        Long uid = jwtUtils.getUidOrNull(token);
        if (uid == null) {
            return null;
        }
        String stored = RedisUtils.get(RedisKey.TOKEN_PREFIX + uid);
        return stored != null ? uid : null;
    }
}
