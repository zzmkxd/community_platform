package com.community.server.service.impl;

import com.community.common.exception.BusinessErrorEnum;
import com.community.common.exception.BusinessException;
import com.community.common.utils.RequestHolder;
import com.community.server.dao.*;
import com.community.server.domain.entity.*;
import com.community.server.domain.vo.RoleVO;
import com.community.server.service.MembershipValidator;
import com.community.server.service.RoleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleDao roleDao;
    private final MemberDao memberDao;
    private final MemberRoleDao memberRoleDao;
    private final ServerDao serverDao;

    @Override
    @Transactional
    public RoleVO createRole(Long serverId, String name, String color, Long permissions, Integer position) {
        MembershipValidator.requireServerOwner(serverDao, serverId);

        Role role = new Role();
        role.setServerId(serverId);
        role.setName(name);
        role.setColor(color);
        role.setPermissions(permissions != null ? permissions : 0L);
        role.setPosition(position != null ? position : 0);
        roleDao.save(role);

        log.info("Role created: id={}, name={}, serverId={}", role.getId(), name, serverId);
        return toRoleVO(role);
    }

    @Override
    public List<RoleVO> getRoles(Long serverId) {
        return roleDao.lambdaQuery()
                .eq(Role::getServerId, serverId)
                .orderByDesc(Role::getPosition)
                .list()
                .stream()
                .map(this::toRoleVO)
                .toList();
    }

    @Override
    @Transactional
    public RoleVO updateRole(Long serverId, Long roleId, String name, String color, Long permissions) {
        MembershipValidator.requireServerOwner(serverDao, serverId);

        Role role = roleDao.lambdaQuery()
                .eq(Role::getId, roleId)
                .eq(Role::getServerId, serverId)
                .oneOpt()
                .orElseThrow(() -> new BusinessException(BusinessErrorEnum.ROLE_NOT_FOUND));

        if (name != null) {
            role.setName(name);
        }
        if (color != null) {
            role.setColor(color);
        }
        if (permissions != null) {
            role.setPermissions(permissions);
        }
        roleDao.updateById(role);

        return toRoleVO(role);
    }

    @Override
    @Transactional
    public void deleteRole(Long serverId, Long roleId) {
        MembershipValidator.requireServerOwner(serverDao, serverId);

        Role role = roleDao.lambdaQuery()
                .eq(Role::getId, roleId)
                .eq(Role::getServerId, serverId)
                .oneOpt()
                .orElseThrow(() -> new BusinessException(BusinessErrorEnum.ROLE_NOT_FOUND));

        // Remove all member-role associations for this role
        memberRoleDao.lambdaUpdate()
                .eq(MemberRole::getRoleId, roleId)
                .remove();

        roleDao.removeById(roleId);
        log.info("Role deleted: id={}, name={}, serverId={}", roleId, role.getName(), serverId);
    }

    @Override
    @Transactional
    public List<RoleVO> assignRoles(Long serverId, Long userId, List<Long> roleIds) {
        MembershipValidator.requireServerOwner(serverDao, serverId);

        ServerMember member = memberDao.lambdaQuery()
                .eq(ServerMember::getServerId, serverId)
                .eq(ServerMember::getUserId, userId)
                .eq(ServerMember::getStatus, 1)
                .oneOpt()
                .orElseThrow(() -> new BusinessException(BusinessErrorEnum.NOT_SERVER_MEMBER));

        // Remove existing role assignments
        memberRoleDao.lambdaUpdate()
                .eq(MemberRole::getMemberId, member.getId())
                .remove();

        // Batch validate all roles belong to the server
        List<Role> roles = roleDao.lambdaQuery()
                .in(Role::getId, roleIds)
                .eq(Role::getServerId, serverId)
                .list();
        if (roles.size() != (int) roleIds.stream().distinct().count()) {
            throw new BusinessException(BusinessErrorEnum.ROLE_NOT_FOUND);
        }

        // Assign new roles
        for (Role role : roles) {
            MemberRole mr = new MemberRole();
            mr.setMemberId(member.getId());
            mr.setRoleId(role.getId());
            memberRoleDao.save(mr);
        }

        log.info("Roles assigned: userId={}, serverId={}, roleIds={}", userId, serverId, roleIds);
        return roleDao.lambdaQuery()
                .in(Role::getId, roleIds)
                .list()
                .stream()
                .map(this::toRoleVO)
                .toList();
    }

    @Override
    @Transactional
    public void removeRole(Long serverId, Long userId, Long roleId) {
        MembershipValidator.requireServerOwner(serverDao, serverId);

        ServerMember member = memberDao.lambdaQuery()
                .eq(ServerMember::getServerId, serverId)
                .eq(ServerMember::getUserId, userId)
                .eq(ServerMember::getStatus, 1)
                .oneOpt()
                .orElseThrow(() -> new BusinessException(BusinessErrorEnum.NOT_SERVER_MEMBER));

        memberRoleDao.lambdaUpdate()
                .eq(MemberRole::getMemberId, member.getId())
                .eq(MemberRole::getRoleId, roleId)
                .remove();

        log.info("Role removed: userId={}, serverId={}, roleId={}", userId, serverId, roleId);
    }

    private RoleVO toRoleVO(Role role) {
        RoleVO vo = new RoleVO();
        vo.setId(role.getId());
        vo.setName(role.getName());
        vo.setColor(role.getColor());
        vo.setPermissions(role.getPermissions());
        vo.setPosition(role.getPosition());
        return vo;
    }
}
