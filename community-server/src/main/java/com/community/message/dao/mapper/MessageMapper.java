package com.community.message.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.community.message.domain.entity.Message;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MessageMapper extends BaseMapper<Message> {
}
