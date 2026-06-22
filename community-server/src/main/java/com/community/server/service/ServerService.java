package com.community.server.service;

import com.community.server.domain.vo.ServerDetailVO;
import com.community.server.domain.vo.ServerVO;

import java.util.List;

public interface ServerService {

    ServerVO createServer(String name, String description, String icon);

    List<ServerVO> getMyServers();

    List<ServerVO> getDiscoverableServers();

    ServerDetailVO getServerDetail(Long serverId);

    ServerVO updateServer(Long serverId, String name, String description, String icon);

    void deleteServer(Long serverId);
}
