package com.community.user.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.community.user.domain.entity.User;
import com.community.user.dao.mapper.UserMapper;
import org.springframework.stereotype.Repository;

@Repository
public class UserDao extends ServiceImpl<UserMapper, User> {
}
