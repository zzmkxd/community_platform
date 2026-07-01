package com.community.server.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.community.server.domain.entity.ServerMember;
import com.community.server.dao.mapper.MemberMapper;
import org.springframework.stereotype.Repository;

@Repository
public class MemberDao extends ServiceImpl<MemberMapper, ServerMember> {
}
