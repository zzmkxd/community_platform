package com.community.server.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.community.server.domain.entity.Channel;
import com.community.server.dao.mapper.ChannelMapper;
import org.springframework.stereotype.Repository;

@Repository
public class ChannelDao extends ServiceImpl<ChannelMapper, Channel> {
}
