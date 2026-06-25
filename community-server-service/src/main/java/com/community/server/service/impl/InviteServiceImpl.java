package com.community.server.service.impl;

import com.community.common.exception.BusinessErrorEnum;
import com.community.common.exception.BusinessException;
import com.community.common.utils.RequestHolder;
import com.community.server.dao.*;
import com.community.server.domain.entity.*;
import com.community.server.domain.vo.InviteVO;
import com.community.server.domain.vo.ServerVO;
import com.community.websocket.service.PushService;
import com.community.server.service.InviteService;
import com.community.websocket.service.adapter.WSAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class InviteServiceImpl implements InviteService {

    private final InviteDao inviteDao;
    private final ServerDao serverDao;
    private final MemberDao memberDao;
    private final RoleDao roleDao;
    private final MemberRoleDao memberRoleDao;
    private final PushService pushService;

    @Override
    @Transactional
    public InviteVO createInvite(Long serverId, Integer maxUses, Integer expireHours) {
        Long uid = RequestHolder.get().getUid();

        Server server = serverDao.lambdaQuery()
                .eq(Server::getId, serverId)
                .eq(Server::getStatus, 1)
                .oneOpt()
                .orElseThrow(() -> new BusinessException(BusinessErrorEnum.SERVER_NOT_FOUND));

        boolean isMember = memberDao.lambdaQuery()
                .eq(ServerMember::getServerId, serverId)
                .eq(ServerMember::getUserId, uid)
                .eq(ServerMember::getStatus, 1)
                .exists();

        if (!isMember) {
            throw new BusinessException(BusinessErrorEnum.NOT_SERVER_MEMBER);
        }

        Invite invite = new Invite();
        invite.setServerId(serverId);
        invite.setInviterId(uid);
        invite.setCode(generateCode());
        invite.setMaxUses(maxUses != null ? maxUses : 0);
        invite.setUsedCount(0);
        invite.setExpireTime(expireHours != null && expireHours > 0
                ? LocalDateTime.now().plusHours(expireHours) : null);
        invite.setStatus(1);
        inviteDao.save(invite);

        log.info("Invite created: code={}, serverId={}, maxUses={}", invite.getCode(), serverId, maxUses);
        return toVO(invite);
    }

    @Override
    @Transactional
    public ServerVO joinByInvite(String code) {
        Long uid = RequestHolder.get().getUid();

        Invite invite = inviteDao.lambdaQuery()
                .eq(Invite::getCode, code)
                .oneOpt()
                .orElseThrow(() -> new BusinessException(BusinessErrorEnum.NO_PERMISSION));

        if (invite.getStatus() != 1) {
            throw new BusinessException(BusinessErrorEnum.NO_PERMISSION);
        }

        if (invite.getExpireTime() != null && LocalDateTime.now().isAfter(invite.getExpireTime())) {
            throw new BusinessException(BusinessErrorEnum.NO_PERMISSION);
        }

        if (invite.getMaxUses() > 0 && invite.getUsedCount() >= invite.getMaxUses()) {
            throw new BusinessException(BusinessErrorEnum.NO_PERMISSION);
        }

        Server server = serverDao.lambdaQuery()
                .eq(Server::getId, invite.getServerId())
                .eq(Server::getStatus, 1)
                .oneOpt()
                .orElseThrow(() -> new BusinessException(BusinessErrorEnum.SERVER_NOT_FOUND));

        // Join as member
        ServerMember existing = memberDao.lambdaQuery()
                .eq(ServerMember::getServerId, invite.getServerId())
                .eq(ServerMember::getUserId, uid)
                .one();

        if (existing == null) {
            ServerMember member = new ServerMember();
            member.setServerId(invite.getServerId());
            member.setUserId(uid);
            member.setStatus(1);
            memberDao.save(member);

            Role everyoneRole = roleDao.lambdaQuery()
                    .eq(Role::getServerId, invite.getServerId())
                    .eq(Role::getName, "@everyone")
                    .one();

            if (everyoneRole != null) {
                MemberRole memberRole = new MemberRole();
                memberRole.setMemberId(member.getId());
                memberRole.setRoleId(everyoneRole.getId());
                memberRoleDao.save(memberRole);
            }
        } else if (existing.getStatus() != 1) {
            existing.setStatus(1);
            memberDao.updateById(existing);
        }

        // Increment usage count
        invite.setUsedCount(invite.getUsedCount() + 1);
        if (invite.getMaxUses() > 0 && invite.getUsedCount() >= invite.getMaxUses()) {
            invite.setStatus(0);
        }
        inviteDao.updateById(invite);

        log.info("User {} joined server {} via invite {}", uid, invite.getServerId(), code);

        int memberCount = (int) (long) memberDao.lambdaQuery()
                .eq(ServerMember::getServerId, invite.getServerId())
                .eq(ServerMember::getStatus, 1)
                .count();

        pushService.pushToServer(invite.getServerId(), uid,
                WSAdapter.buildMemberJoin(invite.getServerId(), null));

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

    private String generateCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    private InviteVO toVO(Invite invite) {
        InviteVO vo = new InviteVO();
        vo.setId(invite.getId());
        vo.setServerId(invite.getServerId());
        vo.setInviterId(invite.getInviterId());
        vo.setCode(invite.getCode());
        vo.setMaxUses(invite.getMaxUses());
        vo.setUsedCount(invite.getUsedCount());
        vo.setExpireTime(invite.getExpireTime());
        vo.setStatus(invite.getStatus());
        vo.setCreateTime(invite.getCreateTime());
        return vo;
    }
}
