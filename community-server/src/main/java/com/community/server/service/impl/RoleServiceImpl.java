package com.community.server.service.impl;

import com.community.server.domain.vo.RoleVO;
import com.community.server.service.RoleService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoleServiceImpl implements RoleService {

    @Override
    public RoleVO createRole(Long serverId, String name, String color, Long permissions, Integer position) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public List<RoleVO> getRoles(Long serverId) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public RoleVO updateRole(Long serverId, Long roleId, String name, String color, Long permissions) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void deleteRole(Long serverId, Long roleId) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public List<RoleVO> assignRoles(Long serverId, Long userId, List<Long> roleIds) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void removeRole(Long serverId, Long userId, Long roleId) {
        throw new UnsupportedOperationException("TODO");
    }
}
