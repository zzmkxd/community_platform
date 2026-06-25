package com.community.message.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.community.message.domain.entity.Message;
import com.community.message.dao.mapper.MessageMapper;
import org.springframework.stereotype.Repository;

@Repository
public class MessageDao extends ServiceImpl<MessageMapper, Message> {
}
