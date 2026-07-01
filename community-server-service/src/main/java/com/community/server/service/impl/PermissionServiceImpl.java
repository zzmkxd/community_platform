package com.community.server.service.impl;

import com.community.server.dao.ChannelPermissionDao;
import com.community.server.dao.MemberDao;
import com.community.server.dao.MemberRoleDao;
import com.community.server.dao.RoleDao;
import com.community.server.domain.entity.*;
import com.community.common.enums.PermissionBit;
import com.community.server.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// ponytail: @RestController 让 Feign 接口的 REST 端点生效
// 注意：参数注解不随 @Override 继承，必须显式声明
@Slf4j
@RestController
@RequestMapping("/internal/permission")
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final RoleDao roleDao;
    private final MemberRoleDao memberRoleDao;
    private final MemberDao memberDao;
    private final ChannelPermissionDao channelPermissionDao;

    @Override
    @GetMapping("/check")
    public boolean checkPermission(@RequestParam("serverId") Long serverId,
                                   @RequestParam("userId") Long userId,
                                   @RequestParam("channelId") Long channelId,
                                   @RequestParam("permissionBit") int permissionBit) {
        // 1. Get the member record
        ServerMember member = memberDao.lambdaQuery()
                .eq(ServerMember::getServerId, serverId)
                .eq(ServerMember::getUserId, userId)
                .eq(ServerMember::getStatus, 1)
                .one();
        if (member == null) {
            return false;
        }

        // 2. Compute base permissions = OR of all role permissions
        List<MemberRole> memberRoles = memberRoleDao.lambdaQuery()
                .eq(MemberRole::getMemberId, member.getId())
                .list();

        if (memberRoles.isEmpty()) {
            return false;
        }

        List<Long> roleIds = memberRoles.stream().map(MemberRole::getRoleId).toList();
        List<Role> roles = roleDao.lambdaQuery()
                .in(Role::getId, roleIds)
                .list();

        long basePermissions = 0L;
        for (Role role : roles) {
            basePermissions |= role.getPermissions();
        }

        // 3. ADMINISTRATOR shortcut → full access
        if ((basePermissions & PermissionBit.ADMINISTRATOR.getBit()) != 0) {
            return true;
        }

        // 4. Apply channel permission overrides
        long effectivePermissions = basePermissions;

        List<ChannelPermission> overrides = channelPermissionDao.lambdaQuery()
                .eq(ChannelPermission::getChannelId, channelId)
                .list();

        // User overrides take priority over role overrides
        for (ChannelPermission override : overrides) {
            boolean matches;
            if (override.getTargetType() == 1) {
                // User override
                matches = override.getTargetId().equals(userId);
            } else {
                // Role override
                matches = roleIds.contains(override.getTargetId());
            }
            if (matches) {
                effectivePermissions |= override.getAllowBits();
                effectivePermissions &= ~override.getDenyBits();
            }
        }

        // 5. Check the requested permission bit
        return (effectivePermissions & permissionBit) != 0;
    }
}
