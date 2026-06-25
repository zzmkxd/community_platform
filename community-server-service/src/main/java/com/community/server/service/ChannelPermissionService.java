package com.community.server.service;

import com.community.server.domain.vo.ChannelPermissionVO;

import java.util.List;

public interface ChannelPermissionService {

    ChannelPermissionVO upsertPermission(Long channelId, Integer targetType, Long targetId, Long allowBits, Long denyBits);

    List<ChannelPermissionVO> getPermissions(Long channelId);

    void deletePermission(Long channelId, Long permId);
}
