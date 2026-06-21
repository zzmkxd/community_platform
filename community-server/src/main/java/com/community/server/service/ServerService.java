package com.community.server.service;

import com.community.server.domain.vo.ServerDetailVO;
import com.community.server.domain.vo.ServerVO;

import java.util.List;

public interface ServerService {

    ServerVO createServer(String name, String description);

    List<ServerVO> getMyServers();

    ServerDetailVO getServerDetail(Long serverId);

    ServerVO updateServer(Long serverId, String name, String description, String icon);

    void deleteServer(Long serverId);
}
