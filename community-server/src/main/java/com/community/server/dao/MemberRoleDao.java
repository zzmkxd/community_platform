package com.community.server.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.community.server.domain.entity.MemberRole;
import com.community.server.dao.mapper.MemberRoleMapper;
import org.springframework.stereotype.Repository;

@Repository
public class MemberRoleDao extends ServiceImpl<MemberRoleMapper, MemberRole> {
}
