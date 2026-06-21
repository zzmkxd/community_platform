package com.community.server.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.community.server.domain.entity.Emoji;
import com.community.server.dao.mapper.EmojiMapper;
import org.springframework.stereotype.Repository;

@Repository
public class EmojiDao extends ServiceImpl<EmojiMapper, Emoji> {
}
