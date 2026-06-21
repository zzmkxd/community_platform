package com.community.user.service.adapter;

import cn.hutool.core.util.RandomUtil;
import com.community.user.domain.entity.User;
import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.bean.WxOAuth2UserInfo;

@Slf4j
public class UserAdapter {

    public static User buildAuthorizeUser(Long id, WxOAuth2UserInfo userInfo) {
        User user = new User();
        user.setId(id);
        user.setAvatar(userInfo.getHeadImgUrl());
        user.setSex(userInfo.getSex());
        user.setOpenId(userInfo.getOpenid());
        if (userInfo.getNickname().length() > 6) {
            user.setNickname("名字过长" + RandomUtil.randomInt(100000));
        } else {
            user.setNickname(userInfo.getNickname());
        }
        return user;
    }
}
