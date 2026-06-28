package com.community.server.dao;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.community.server.domain.entity.Server;
import com.community.server.dao.mapper.ServerMapper;
import org.springframework.stereotype.Repository;

@Repository
public class ServerDao extends ServiceImpl<ServerMapper, Server> {
}
