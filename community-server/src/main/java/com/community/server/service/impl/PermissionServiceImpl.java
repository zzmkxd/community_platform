package com.community.server.service.impl;

import com.community.server.dao.MemberRoleDao;
import com.community.server.dao.RoleDao;
import com.community.server.dao.ChannelPermissionDao;
import com.community.server.domain.enums.PermissionBit;
import com.community.server.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final RoleDao roleDao;
    private final MemberRoleDao memberRoleDao;
    private final ChannelPermissionDao channelPermissionDao;

    @Override
    public boolean checkPermission(Long serverId, Long userId, Long channelId, int permissionBit) {
        // TODO: 实现 RBAC 权限检查：
        // 1. 查用户所有角色
        // 2. 计算基础权限 = OR(角色.permissions)
        // 3. 如果含 ADMINISTRATOR → 直接放行
        // 4. 查 channel_permission 覆盖
        // 5. (effectivePermissions | allow) & ~deny
        // 6. return (effective & permissionBit) != 0;
        throw new UnsupportedOperationException("TODO");
    }
}
