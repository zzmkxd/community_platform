package com.community.message.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.community.message.domain.entity.Message;
import com.community.message.dao.mapper.MessageMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class MessageDao extends ServiceImpl<MessageMapper, Message> {

    public List<Map<String, Object>> countUnreadByChannels(List<Long> channelIds, Long uid) {
        return baseMapper.countUnreadByChannels(channelIds, uid);
    }
}
