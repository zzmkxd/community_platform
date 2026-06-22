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
import com.community.server.domain.vo.*;
import com.community.server.service.PermissionService;
import com.community.server.service.ServerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServerServiceImpl implements ServerService {

    private final ServerDao serverDao;
    private final MemberDao memberDao;
    private final RoleDao roleDao;
    private final MemberRoleDao memberRoleDao;
    private final CategoryDao categoryDao;
    private final ChannelDao channelDao;
    private final PermissionService permissionService;

    @Override
    @Transactional
    public ServerVO createServer(String name, String description, String icon) {
        Long uid = RequestHolder.get().getUid();

        Server server = new Server();
        server.setName(name);
        server.setDescription(description);
        server.setIcon(icon);
        server.setOwnerId(uid);
        server.setStatus(1);
        server.setSortOrder(0);
        serverDao.save(server);

        Role everyoneRole = new Role();
        everyoneRole.setServerId(server.getId());
        everyoneRole.setName("@everyone");
        everyoneRole.setPermissions(0L
                | PermissionBit.CREATE_INVITE.getBit()
                | PermissionBit.SEND_MESSAGES.getBit()
                | PermissionBit.ADD_REACTIONS.getBit()
                | PermissionBit.USE_THREADS.getBit()
                | PermissionBit.EMBED_LINKS.getBit()
                | PermissionBit.ATTACH_FILES.getBit());
        everyoneRole.setPosition(0);
        roleDao.save(everyoneRole);

        Role ownerRole = new Role();
        ownerRole.setServerId(server.getId());
        ownerRole.setName("Owner");
        ownerRole.setColor("#FFD700");
        ownerRole.setPermissions((long) PermissionBit.ADMINISTRATOR.getBit());
        ownerRole.setPosition(999);
        roleDao.save(ownerRole);

        ServerMember member = new ServerMember();
        member.setServerId(server.getId());
        member.setUserId(uid);
        member.setStatus(1);
        memberDao.save(member);

        MemberRole mrEveryone = new MemberRole();
        mrEveryone.setMemberId(member.getId());
        mrEveryone.setRoleId(everyoneRole.getId());
        memberRoleDao.save(mrEveryone);

        MemberRole mrOwner = new MemberRole();
        mrOwner.setMemberId(member.getId());
        mrOwner.setRoleId(ownerRole.getId());
        memberRoleDao.save(mrOwner);

        log.info("Server created: id={}, name={}, ownerId={}", server.getId(), name, uid);
        return toServerVO(server, 1);
    }

    @Override
    public List<ServerVO> getMyServers() {
        Long uid = RequestHolder.get().getUid();

        List<ServerMember> members = memberDao.lambdaQuery()
                .eq(ServerMember::getUserId, uid)
                .eq(ServerMember::getStatus, 1)
                .list();

        if (members.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> serverIds = members.stream()
                .map(ServerMember::getServerId)
                .toList();

        List<Server> servers = serverDao.lambdaQuery()
                .in(Server::getId, serverIds)
                .eq(Server::getStatus, 1)
                .list();

        Map<Long, Long> memberCounts = memberCountByServerIds(serverIds);

        return servers.stream()
                .map(s -> toServerVO(s, memberCounts.getOrDefault(s.getId(), 0L).intValue()))
                .toList();
    }

    @Override
    public CursorPageBaseResp<ServerVO> getDiscoverableServers(String cursor, Integer pageSize) {
        CursorPageBaseReq req = new CursorPageBaseReq(
                pageSize != null ? pageSize : 30, cursor);
        CursorPageBaseResp<Server> page = CursorUtils.getCursorPageByMysql(
                serverDao, req,
                wrapper -> wrapper.eq(Server::getStatus, 1),
                Server::getId);

        if (page.isEmpty()) {
            return CursorPageBaseResp.empty();
        }

        List<Server> servers = page.getList();
        List<Long> serverIds = servers.stream().map(Server::getId).toList();
        Map<Long, Long> memberCounts = memberCountByServerIds(serverIds);

        List<ServerVO> vos = servers.stream()
                .map(s -> toServerVO(s, memberCounts.getOrDefault(s.getId(), 0L).intValue()))
                .toList();

        return CursorPageBaseResp.init(page, vos);
    }

    @Override
    public ServerDetailVO getServerDetail(Long serverId) {
        Server server = serverDao.lambdaQuery()
                .eq(Server::getId, serverId)
                .eq(Server::getStatus, 1)
                .oneOpt()
                .orElseThrow(() -> new BusinessException(BusinessErrorEnum.SERVER_NOT_FOUND));

        Long uid = RequestHolder.get().getUid();

        List<Category> categories = categoryDao.lambdaQuery()
                .eq(Category::getServerId, serverId)
                .orderByAsc(Category::getSortOrder)
                .list();

        List<Channel> channels = channelDao.lambdaQuery()
                .eq(Channel::getServerId, serverId)
                .eq(Channel::getStatus, 1)
                .orderByAsc(Channel::getSortOrder)
                .list();

        Map<Long, List<ChannelVO>> groupedChannels = new LinkedHashMap<>();
        for (Channel ch : channels) {
            Long catId = ch.getCategoryId() != null ? ch.getCategoryId() : 0L;
            groupedChannels.computeIfAbsent(catId, k -> new ArrayList<>()).add(toChannelVO(ch));
        }

        List<CategoryVO> categoryVOs = new ArrayList<>();
        for (Category cat : categories) {
            CategoryVO cvo = new CategoryVO();
            cvo.setId(cat.getId());
            cvo.setName(cat.getName());
            cvo.setSortOrder(cat.getSortOrder());
            cvo.setChannels(groupedChannels.getOrDefault(cat.getId(), Collections.emptyList()));
            categoryVOs.add(cvo);
        }

        List<ChannelVO> uncategorized = groupedChannels.getOrDefault(0L, Collections.emptyList());
        if (!uncategorized.isEmpty()) {
            CategoryVO uncatVO = new CategoryVO();
            uncatVO.setId(null);
            uncatVO.setName("未分类");
            uncatVO.setSortOrder(Integer.MAX_VALUE);
            uncatVO.setChannels(uncategorized);
            categoryVOs.add(uncatVO);
        }

        int memberCount = (int) (long) memberDao.lambdaQuery()
                .eq(ServerMember::getServerId, serverId)
                .eq(ServerMember::getStatus, 1)
                .count();

        ServerMember myMember = memberDao.lambdaQuery()
                .eq(ServerMember::getServerId, serverId)
                .eq(ServerMember::getUserId, uid)
                .eq(ServerMember::getStatus, 1)
                .one();

        List<RoleVO> myRoles = Collections.emptyList();
        if (myMember != null) {
            List<MemberRole> memberRoles = memberRoleDao.lambdaQuery()
                    .eq(MemberRole::getMemberId, myMember.getId())
                    .list();
            if (!memberRoles.isEmpty()) {
                List<Long> roleIds = memberRoles.stream().map(MemberRole::getRoleId).toList();
                myRoles = roleDao.lambdaQuery()
                        .in(Role::getId, roleIds)
                        .list()
                        .stream()
                        .map(this::toRoleVO)
                        .toList();
            }
        }

        ServerDetailVO detail = new ServerDetailVO();
        detail.setServer(toServerVO(server, memberCount));
        detail.setCategories(categoryVOs);
        detail.setMyRoles(myRoles);
        return detail;
    }

    @Override
    @Transactional
    public ServerVO updateServer(Long serverId, String name, String description, String icon) {
        Long uid = RequestHolder.get().getUid();

        Server server = serverDao.getById(serverId);
        if (server == null || server.getStatus() == 0) {
            throw new BusinessException(BusinessErrorEnum.SERVER_NOT_FOUND);
        }
        if (!server.getOwnerId().equals(uid)
                && !permissionService.checkPermission(serverId, uid, null, PermissionBit.ADMINISTRATOR.getBit())) {
            throw new BusinessException(BusinessErrorEnum.NO_PERMISSION);
        }

        if (name != null) {
            server.setName(name);
        }
        if (description != null) {
            server.setDescription(description);
        }
        if (icon != null) {
            server.setIcon(icon);
        }
        serverDao.updateById(server);

        int memberCount = (int) (long) memberDao.lambdaQuery()
                .eq(ServerMember::getServerId, serverId)
                .eq(ServerMember::getStatus, 1)
                .count();

        return toServerVO(server, memberCount);
    }

    @Override
    @Transactional
    public void deleteServer(Long serverId) {
        Long uid = RequestHolder.get().getUid();

        Server server = serverDao.getById(serverId);
        if (server == null || server.getStatus() == 0) {
            throw new BusinessException(BusinessErrorEnum.SERVER_NOT_FOUND);
        }
        if (!server.getOwnerId().equals(uid)) {
            throw new BusinessException(BusinessErrorEnum.NO_PERMISSION);
        }

        server.setStatus(0);
        serverDao.updateById(server);
        log.info("Server soft-deleted: id={}, name={}", serverId, server.getName());
    }

    // ---- private helpers ----

    private Map<Long, Long> memberCountByServerIds(List<Long> serverIds) {
        if (serverIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ServerMember> allMembers = memberDao.lambdaQuery()
                .in(ServerMember::getServerId, serverIds)
                .eq(ServerMember::getStatus, 1)
                .list();
        return allMembers.stream()
                .collect(Collectors.groupingBy(ServerMember::getServerId, Collectors.counting()));
    }

    private ServerVO toServerVO(Server server, int memberCount) {
        ServerVO vo = new ServerVO();
        vo.setId(server.getId());
        vo.setName(server.getName());
        vo.setDescription(server.getDescription());
        vo.setIcon(server.getIcon());
        vo.setOwnerId(server.getOwnerId());
        vo.setMemberCount(memberCount);
        vo.setCreateTime(server.getCreateTime());
        return vo;
    }

    private ChannelVO toChannelVO(Channel channel) {
        ChannelVO vo = new ChannelVO();
        vo.setId(channel.getId());
        vo.setName(channel.getName());
        vo.setType(channel.getType());
        vo.setTopic(channel.getTopic());
        vo.setSortOrder(channel.getSortOrder());
        return vo;
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
