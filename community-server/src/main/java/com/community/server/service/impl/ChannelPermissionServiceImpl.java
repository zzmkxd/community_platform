package com.community.server.service.impl;

import com.community.server.domain.vo.ChannelPermissionVO;
import com.community.server.service.ChannelPermissionService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChannelPermissionServiceImpl implements ChannelPermissionService {

    @Override
    public ChannelPermissionVO upsertPermission(Long channelId, Integer targetType, Long targetId, Long allowBits, Long denyBits) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public List<ChannelPermissionVO> getPermissions(Long channelId) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void deletePermission(Long channelId, Long permId) {
        throw new UnsupportedOperationException("TODO");
    }
}
