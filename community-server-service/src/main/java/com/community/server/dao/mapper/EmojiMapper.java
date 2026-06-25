package com.community.server.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.community.server.domain.entity.Emoji;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface EmojiMapper extends BaseMapper<Emoji> {
}
