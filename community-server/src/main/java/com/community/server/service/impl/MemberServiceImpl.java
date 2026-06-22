package com.community.server.service.impl;

import com.community.common.domain.vo.request.CursorPageBaseReq;
import com.community.common.domain.vo.response.CursorPageBaseResp;
import com.community.common.exception.BusinessErrorEnum;
import com.community.common.exception.BusinessException;
import com.community.common.utils.CursorUtils;
import com.community.common.utils.RequestHolder;
import com.community.server.dao.*;
import com.community.server.domain.entity.*;
import com.community.server.domain.enums.PermissionBit;
import com.community.server.domain.vo.MemberVO;
import com.community.server.domain.vo.RoleVO;
import com.community.server.service.MemberService;
import com.community.server.service.PermissionService;
import com.community.user.dao.UserDao;
import com.community.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberDao memberDao;
    private final UserDao userDao;
    private final RoleDao roleDao;
    private final MemberRoleDao memberRoleDao;
    private final ServerDao serverDao;
    private final PermissionService permissionService;

    @Override
    public CursorPageBaseResp<MemberVO> getMembers(Long serverId, String cursor, Integer pageSize) {
        CursorPageBaseReq req = new CursorPageBaseReq(pageSize != null ? pageSize : 10, cursor);
        CursorPageBaseResp<ServerMember> page = CursorUtils.getCursorPageByMysql(
                memberDao, req,
                wrapper -> wrapper.eq(ServerMember::getServerId, serverId)
                        .eq(ServerMember::getStatus, 1),
                ServerMember::getId
        );

        if (page.isEmpty()) {
            return CursorPageBaseResp.empty();
        }

        List<MemberVO> voList = buildMemberVOList(page.getList());
        return CursorPageBaseResp.init(page, voList);
    }

    @Override
    @Transactional
    public MemberVO joinServer(Long serverId) {
        Long uid = RequestHolder.get().getUid();

        Server server = serverDao.lambdaQuery()
                .eq(Server::getId, serverId)
                .eq(Server::getStatus, 1)
                .oneOpt()
                .orElseThrow(() -> new BusinessException(BusinessErrorEnum.SERVER_NOT_FOUND));

        ServerMember existing = memberDao.lambdaQuery()
                .eq(ServerMember::getServerId, serverId)
                .eq(ServerMember::getUserId, uid)
                .one();

        if (existing != null) {
            if (existing.getStatus() == 1) {
                return buildSingleMemberVO(existing);
            }
            // Rejoin: reactivate the member record
            existing.setStatus(1);
            memberDao.updateById(existing);
            log.info("User {} rejoined server {}", uid, serverId);
            return buildSingleMemberVO(existing);
        }

        ServerMember member = new ServerMember();
        member.setServerId(serverId);
        member.setUserId(uid);
        member.setStatus(1);
        memberDao.save(member);

        // Assign @everyone role to new member
        Role everyoneRole = roleDao.lambdaQuery()
                .eq(Role::getServerId, serverId)
                .eq(Role::getName, "@everyone")
                .one();

        if (everyoneRole != null) {
            MemberRole memberRole = new MemberRole();
            memberRole.setMemberId(member.getId());
            memberRole.setRoleId(everyoneRole.getId());
            memberRoleDao.save(memberRole);
        }

        log.info("User {} joined server {}", uid, serverId);
        return buildSingleMemberVO(member);
    }

    @Override
    @Transactional
    public void leaveOrKick(Long serverId, Long userId) {
        Long uid = RequestHolder.get().getUid();

        ServerMember target = memberDao.lambdaQuery()
                .eq(ServerMember::getServerId, serverId)
                .eq(ServerMember::getUserId, userId)
                .eq(ServerMember::getStatus, 1)
                .oneOpt()
                .orElseThrow(() -> new BusinessException(BusinessErrorEnum.NOT_SERVER_MEMBER));

        if (uid.equals(userId)) {
            // Owner cannot leave without transferring first
            Server server = serverDao.getById(serverId);
            if (server != null && server.getOwnerId().equals(uid)) {
                throw new BusinessException(BusinessErrorEnum.NO_PERMISSION);
            }
            target.setStatus(3);
            log.info("User {} left server {}", userId, serverId);
        } else {
            // Requires KICK_MEMBERS permission
            if (!permissionService.checkPermission(serverId, uid, null, PermissionBit.KICK_MEMBERS.getBit())) {
                throw new BusinessException(BusinessErrorEnum.NO_PERMISSION);
            }
            target.setStatus(2);
            log.info("User {} was kicked from server {} by {}", userId, serverId, uid);
        }

        memberDao.updateById(target);
    }

    @Override
    @Transactional
    public MemberVO updateNickname(Long serverId, String nickname) {
        Long uid = RequestHolder.get().getUid();

        ServerMember member = memberDao.lambdaQuery()
                .eq(ServerMember::getServerId, serverId)
                .eq(ServerMember::getUserId, uid)
                .eq(ServerMember::getStatus, 1)
                .oneOpt()
                .orElseThrow(() -> new BusinessException(BusinessErrorEnum.NOT_SERVER_MEMBER));

        member.setNickname(nickname);
        memberDao.updateById(member);

        return buildSingleMemberVO(member);
    }

    @Override
    @Transactional
    public void transferOwnership(Long serverId, Long newOwnerId) {
        Long uid = RequestHolder.get().getUid();

        Server server = serverDao.lambdaQuery()
                .eq(Server::getId, serverId)
                .eq(Server::getStatus, 1)
                .oneOpt()
                .orElseThrow(() -> new BusinessException(BusinessErrorEnum.SERVER_NOT_FOUND));

        if (!server.getOwnerId().equals(uid)) {
            throw new BusinessException(BusinessErrorEnum.NO_PERMISSION);
        }

        boolean isMember = memberDao.lambdaQuery()
                .eq(ServerMember::getServerId, serverId)
                .eq(ServerMember::getUserId, newOwnerId)
                .eq(ServerMember::getStatus, 1)
                .exists();

        if (!isMember) {
            throw new BusinessException(BusinessErrorEnum.NOT_SERVER_MEMBER);
        }

        server.setOwnerId(newOwnerId);
        serverDao.updateById(server);
        log.info("Ownership of server {} transferred from {} to {}", serverId, uid, newOwnerId);
    }

    // ==================== private helpers ====================

    private List<MemberVO> buildMemberVOList(List<ServerMember> members) {
        if (members.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> userIds = members.stream().map(ServerMember::getUserId).distinct().toList();
        Map<Long, User> userMap = userDao.lambdaQuery()
                .in(User::getId, userIds)
                .list()
                .stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<Long> memberIds = members.stream().map(ServerMember::getId).toList();
        Map<Long, List<RoleVO>> roleMap = buildRoleMapByMemberIds(memberIds);

        List<MemberVO> vos = new ArrayList<>();
        for (ServerMember member : members) {
            vos.add(toMemberVO(member, userMap.get(member.getUserId()), roleMap));
        }
        return vos;
    }

    private MemberVO buildSingleMemberVO(ServerMember member) {
        User user = userDao.getById(member.getUserId());
        Map<Long, List<RoleVO>> roleMap = buildRoleMapByMemberIds(List.of(member.getId()));
        return toMemberVO(member, user, roleMap);
    }

    private MemberVO toMemberVO(ServerMember member, User user, Map<Long, List<RoleVO>> roleMap) {
        MemberVO vo = new MemberVO();
        vo.setUserId(member.getUserId());
        vo.setNickname(user != null ? user.getNickname() : null);
        vo.setAvatar(user != null ? user.getAvatar() : null);
        vo.setServerNickname(member.getNickname());
        vo.setStatus(member.getStatus());
        vo.setRoles(roleMap.getOrDefault(member.getId(), Collections.emptyList()));
        return vo;
    }

    private Map<Long, List<RoleVO>> buildRoleMapByMemberIds(List<Long> memberIds) {
        if (memberIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<MemberRole> memberRoles = memberRoleDao.lambdaQuery()
                .in(MemberRole::getMemberId, memberIds)
                .list();

        if (memberRoles.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Long> roleIds = memberRoles.stream().map(MemberRole::getRoleId).distinct().toList();
        Map<Long, Role> roleById = roleDao.lambdaQuery()
                .in(Role::getId, roleIds)
                .list()
                .stream()
                .collect(Collectors.toMap(Role::getId, r -> r));

        Map<Long, List<RoleVO>> result = new HashMap<>();
        for (MemberRole mr : memberRoles) {
            Role role = roleById.get(mr.getRoleId());
            if (role != null) {
                result.computeIfAbsent(mr.getMemberId(), k -> new ArrayList<>())
                        .add(toRoleVO(role));
            }
        }
        return result;
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
