package com.community.server.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.community.server.domain.entity.Invite;
import com.community.server.dao.mapper.InviteMapper;
import org.springframework.stereotype.Repository;

@Repository
public class InviteDao extends ServiceImpl<InviteMapper, Invite> {
}
