package com.community.server.service;

import com.community.server.domain.vo.RoleVO;

import java.util.List;

public interface RoleService {

    RoleVO createRole(Long serverId, String name, String color, Long permissions, Integer position);

    List<RoleVO> getRoles(Long serverId);

    RoleVO updateRole(Long serverId, Long roleId, String name, String color, Long permissions);

    void deleteRole(Long serverId, Long roleId);

    List<RoleVO> assignRoles(Long serverId, Long userId, List<Long> roleIds);

    void removeRole(Long serverId, Long userId, Long roleId);
}
