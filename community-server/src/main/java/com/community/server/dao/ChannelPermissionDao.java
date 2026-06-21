package com.community.server.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.community.server.domain.entity.ChannelPermission;
import com.community.server.dao.mapper.ChannelPermissionMapper;
import org.springframework.stereotype.Repository;

@Repository
public class ChannelPermissionDao extends ServiceImpl<ChannelPermissionMapper, ChannelPermission> {
}
