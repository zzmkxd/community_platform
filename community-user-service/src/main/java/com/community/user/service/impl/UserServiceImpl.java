package com.community.user.service.impl;

import com.community.common.exception.BusinessErrorEnum;
import com.community.common.exception.BusinessException;
import com.community.common.utils.RequestHolder;
import com.community.user.dao.UserDao;
import com.community.user.domain.dto.AccountBindReq;
import com.community.user.domain.entity.User;
import com.community.common.domain.vo.UserVO;
import com.community.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// ponytail: @RestController + @RequestMapping 让 UserServiceImpl 既是本地 Bean 也是 /internal/user 的 REST 端点
// Feign 消费者通过 @FeignClient 调用，本服务内注入的 UserService 直接走本地方法，不走 HTTP
@RestController
@RequestMapping("/internal/user")
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserDao userDao;

    @Override
    public UserVO getMe() {
        Long uid = RequestHolder.get().getUid();
        User user = userDao.getById(uid);
        if (user == null) {
            throw new BusinessException(BusinessErrorEnum.USER_NOT_FOUND);
        }
        return toVO(user);
    }

    @Override
    public UserVO updateMe(@RequestParam("nickname") String nickname,
                           @RequestParam("avatar") String avatar,
                           @RequestParam("email") String email) {
        Long uid = RequestHolder.get().getUid();
        User user = userDao.getById(uid);
        if (user == null) {
            throw new BusinessException(BusinessErrorEnum.USER_NOT_FOUND);
        }
        if (nickname != null) {
            user.setNickname(nickname);
        }
        if (avatar != null) {
            user.setAvatar(avatar);
        }
        if (email != null) {
            user.setEmail(email);
        }
        userDao.updateById(user);
        return toVO(user);
    }

    @Override
    public UserVO bindWeChat(@RequestBody AccountBindReq req) {
        Long uid = RequestHolder.get().getUid();
        User user = userDao.getById(uid);
        if (user == null) {
            throw new BusinessException(BusinessErrorEnum.USER_NOT_FOUND);
        }

        String openId = req.getOpenId();
        if (openId == null && req.getCode() != null) {
            throw new BusinessException(BusinessErrorEnum.WECHAT_NOT_CONFIGURED);
        }
        if (openId == null) {
            throw new BusinessException(BusinessErrorEnum.WECHAT_NOT_CONFIGURED);
        }

        boolean bound = userDao.lambdaQuery()
                .eq(User::getOpenId, openId)
                .ne(User::getId, uid)
                .exists();
        if (bound) {
            throw new BusinessException(BusinessErrorEnum.OPEN_ID_ALREADY_BOUND);
        }

        user.setOpenId(openId);
        userDao.updateById(user);
        return toVO(user);
    }

    @Override
    public UserVO getUserById(@PathVariable("id") Long id) {
        User user = userDao.getById(id);
        if (user == null) {
            throw new BusinessException(BusinessErrorEnum.USER_NOT_FOUND);
        }
        return toVO(user);
    }

    @Override
    public List<UserVO> getBatchUsers(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return userDao.lambdaQuery()
                .in(User::getId, ids)
                .list()
                .stream()
                .map(this::toVO)
                .toList();
    }

    private UserVO toVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setEmail(user.getEmail());
        vo.setSex(user.getSex());
        vo.setStatus(user.getStatus());
        vo.setOpenId(user.getOpenId());
        vo.setUnionId(user.getUnionId());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }
}
