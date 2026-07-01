package com.community.server.service.impl;

import com.community.common.domain.vo.request.CursorPageBaseReq;
import com.community.common.domain.vo.response.CursorPageBaseResp;
import com.community.common.exception.BusinessErrorEnum;
import com.community.common.exception.BusinessException;
import com.community.common.utils.CursorUtils;
import com.community.common.utils.RequestHolder;
import com.community.server.dao.*;
import com.community.server.domain.entity.*;
import com.community.server.domain.enums.MemberStatusEnum;
import com.community.common.enums.PermissionBit;
import com.community.server.domain.vo.MemberVO;
import com.community.server.domain.vo.RoleVO;
import com.community.websocket.service.PushService;
import com.community.server.service.MemberService;
import com.community.server.service.PermissionService;
import com.community.common.domain.vo.UserVO;
import com.community.user.service.UserService;

import com.community.websocket.service.adapter.WSAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

// ponytail: @RestController 让 Feign 接口的 REST 端点生效
// 注意：参数注解不随 @Override 继承，必须显式声明
@Slf4j
@RestController
@RequestMapping("/internal/member")
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private static final String ROLE_EVERYONE = "@everyone";

    private final MemberDao memberDao;
    private final UserService userService;
    private final RoleDao roleDao;
    private final MemberRoleDao memberRoleDao;
    private final ServerDao serverDao;
    private final PermissionService permissionService;
    private final PushService pushService;

    @Override
    @GetMapping("/server/{serverId}")
    public CursorPageBaseResp<MemberVO> getMembers(@PathVariable("serverId") Long serverId,
                                                    @RequestParam(value = "cursor", required = false) String cursor,
                                                    @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        CursorPageBaseReq req = new CursorPageBaseReq(pageSize != null ? pageSize : 10, cursor);
        CursorPageBaseResp<ServerMember> page = CursorUtils.getCursorPageByMysql(
                memberDao, req,
                wrapper -> wrapper.eq(ServerMember::getServerId, serverId)
                        .eq(ServerMember::getStatus, MemberStatusEnum.ACTIVE.getStatus()),
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
    @PostMapping("/server/{serverId}/join")
    public MemberVO joinServer(@PathVariable("serverId") Long serverId) {
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
            if (existing.getStatus().equals(MemberStatusEnum.ACTIVE.getStatus())) {
                return buildSingleMemberVO(existing);
            }
            existing.setStatus(MemberStatusEnum.ACTIVE.getStatus());
            memberDao.updateById(existing);
            log.info("User {} rejoined server {}", uid, serverId);
            return buildSingleMemberVO(existing);
        }

        ServerMember member = new ServerMember();
        member.setServerId(serverId);
        member.setUserId(uid);
        member.setStatus(MemberStatusEnum.ACTIVE.getStatus());
        memberDao.save(member);

        Role everyoneRole = roleDao.lambdaQuery()
                .eq(Role::getServerId, serverId)
                .eq(Role::getName, ROLE_EVERYONE)
                .one();

        if (everyoneRole != null) {
            MemberRole memberRole = new MemberRole();
            memberRole.setMemberId(member.getId());
            memberRole.setRoleId(everyoneRole.getId());
            memberRoleDao.save(memberRole);
        }

        log.info("User {} joined server {}", uid, serverId);

        pushService.pushToServer(serverId, uid, WSAdapter.buildMemberJoin(serverId, buildSingleMemberVO(member)));

        return buildSingleMemberVO(member);
    }

    @Override
    @Transactional
    @DeleteMapping("/server/{serverId}/member/{userId}")
    public void leaveOrKick(@PathVariable("serverId") Long serverId,
                            @PathVariable("userId") Long userId) {
        Long uid = RequestHolder.get().getUid();

        ServerMember target = memberDao.lambdaQuery()
                .eq(ServerMember::getServerId, serverId)
                .eq(ServerMember::getUserId, userId)
                .eq(ServerMember::getStatus, MemberStatusEnum.ACTIVE.getStatus())
                .oneOpt()
                .orElseThrow(() -> new BusinessException(BusinessErrorEnum.NOT_SERVER_MEMBER));

        if (uid.equals(userId)) {
            Server server = serverDao.getById(serverId);
            if (server != null && server.getOwnerId().equals(uid)) {
                throw new BusinessException(BusinessErrorEnum.NO_PERMISSION);
            }
            target.setStatus(MemberStatusEnum.LEFT.getStatus());
            memberDao.updateById(target);
            log.info("User {} left server {}", userId, serverId);
            pushService.pushToServer(serverId, userId, WSAdapter.buildMemberLeave(serverId, userId));
        } else {
            if (!permissionService.checkPermission(serverId, uid, null, PermissionBit.KICK_MEMBERS.getBit())) {
                throw new BusinessException(BusinessErrorEnum.NO_PERMISSION);
            }
            target.setStatus(MemberStatusEnum.KICKED.getStatus());
            memberDao.updateById(target);
            log.info("User {} was kicked from server {} by {}", userId, serverId, uid);
            pushService.pushToServer(serverId, userId, WSAdapter.buildMemberKick(serverId, userId));
        }
    }

    @Override
    @Transactional
    @PutMapping("/server/{serverId}/nickname")
    public MemberVO updateNickname(@PathVariable("serverId") Long serverId,
                                    @RequestParam("nickname") String nickname) {
        Long uid = RequestHolder.get().getUid();

        ServerMember member = memberDao.lambdaQuery()
                .eq(ServerMember::getServerId, serverId)
                .eq(ServerMember::getUserId, uid)
                .eq(ServerMember::getStatus, MemberStatusEnum.ACTIVE.getStatus())
                .oneOpt()
                .orElseThrow(() -> new BusinessException(BusinessErrorEnum.NOT_SERVER_MEMBER));

        member.setNickname(nickname);
        memberDao.updateById(member);

        return buildSingleMemberVO(member);
    }

    @Override
    @GetMapping("/server/{serverId}/uids")
    public List<Long> getServerMemberUids(@PathVariable("serverId") Long serverId,
                                          @RequestParam(value = "excludeUid", required = false) Long excludeUid) {
        return memberDao.lambdaQuery()
                .select(ServerMember::getUserId)
                .eq(ServerMember::getServerId, serverId)
                .eq(ServerMember::getStatus, MemberStatusEnum.ACTIVE.getStatus())
                .list()
                .stream()
                .map(ServerMember::getUserId)
                .filter(uid -> excludeUid == null || !uid.equals(excludeUid))
                .toList();
    }

    @Override
    @Transactional
    @PostMapping("/server/{serverId}/transfer/{newOwnerId}")
    public void transferOwnership(@PathVariable("serverId") Long serverId,
                                  @PathVariable("newOwnerId") Long newOwnerId) {
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
                .eq(ServerMember::getStatus, MemberStatusEnum.ACTIVE.getStatus())
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
        List<UserVO> users = userService.getBatchUsers(userIds);
        Map<Long, UserVO> userMap = users.stream()
                .collect(Collectors.toMap(UserVO::getId, u -> u));

        List<Long> memberIds = members.stream().map(ServerMember::getId).toList();
        Map<Long, List<RoleVO>> roleMap = buildRoleMapByMemberIds(memberIds);

        List<MemberVO> vos = new ArrayList<>();
        for (ServerMember member : members) {
            vos.add(toMemberVO(member, userMap.get(member.getUserId()), roleMap));
        }
        return vos;
    }

    private MemberVO buildSingleMemberVO(ServerMember member) {
        UserVO user = userService.getUserById(member.getUserId());
        Map<Long, List<RoleVO>> roleMap = buildRoleMapByMemberIds(List.of(member.getId()));
        return toMemberVO(member, user, roleMap);
    }

    private MemberVO toMemberVO(ServerMember member, UserVO user, Map<Long, List<RoleVO>> roleMap) {
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
