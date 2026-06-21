package com.community.message.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.community.message.domain.entity.Reaction;
import com.community.message.dao.mapper.ReactionMapper;
import org.springframework.stereotype.Repository;

@Repository
public class ReactionDao extends ServiceImpl<ReactionMapper, Reaction> {
}
