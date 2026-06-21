package com.community.server.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.community.server.domain.entity.Role;
import com.community.server.dao.mapper.RoleMapper;
import org.springframework.stereotype.Repository;

@Repository
public class RoleDao extends ServiceImpl<RoleMapper, Role> {
}
