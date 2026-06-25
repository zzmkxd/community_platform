package com.community.server.service;

import com.community.server.domain.vo.InviteVO;
import com.community.server.domain.vo.ServerVO;

public interface InviteService {

    InviteVO createInvite(Long serverId, Integer maxUses, Integer expireHours);

    ServerVO joinByInvite(String code);
}
