package com.community.server.service.impl;

import com.community.server.dao.ServerDao;
import com.community.server.domain.vo.ServerDetailVO;
import com.community.server.domain.vo.ServerVO;
import com.community.server.service.ServerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ServerServiceImpl implements ServerService {

    private final ServerDao serverDao;

    @Override
    public ServerVO createServer(String name, String description) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public List<ServerVO> getMyServers() {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public ServerDetailVO getServerDetail(Long serverId) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public ServerVO updateServer(Long serverId, String name, String description, String icon) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void deleteServer(Long serverId) {
        throw new UnsupportedOperationException("TODO");
    }
}
