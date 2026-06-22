package com.community.message.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.community.message.domain.entity.ChannelReadState;
import com.community.message.dao.mapper.ChannelReadStateMapper;
import org.springframework.stereotype.Repository;

@Repository
public class ChannelReadStateDao extends ServiceImpl<ChannelReadStateMapper, ChannelReadState> {
}
