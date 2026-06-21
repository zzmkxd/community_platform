package com.community.message.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.community.message.domain.entity.Thread;
import com.community.message.dao.mapper.ThreadMapper;
import org.springframework.stereotype.Repository;

@Repository
public class ThreadDao extends ServiceImpl<ThreadMapper, Thread> {
}
