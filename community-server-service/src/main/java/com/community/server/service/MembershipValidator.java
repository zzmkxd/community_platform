package com.community.server.service;

import com.community.common.exception.BusinessErrorEnum;
import com.community.common.exception.BusinessException;
import com.community.common.utils.RequestHolder;
import com.community.server.dao.MemberDao;
import com.community.server.dao.ServerDao;
import com.community.server.domain.entity.Server;
import com.community.server.domain.entity.ServerMember;
import com.community.server.domain.enums.MemberStatusEnum;
import com.community.server.domain.enums.ServerStatusEnum;

/**
 * 成员身份校验工具类 — 统一的 requireMember / requireServerOwner 校验逻辑
 */
public class MembershipValidator {

    public static ServerMember requireMember(MemberDao memberDao, Long serverId) {
        Long uid = RequestHolder.get().getUid();
        ServerMember member = memberDao.lambdaQuery()
                .eq(ServerMember::getServerId, serverId)
                .eq(ServerMember::getUserId, uid)
                .eq(ServerMember::getStatus, MemberStatusEnum.ACTIVE.getStatus())
                .one();
        if (member == null) {
            throw new BusinessException(BusinessErrorEnum.NOT_SERVER_MEMBER);
        }
        return member;
    }

    public static Server requireServerOwner(ServerDao serverDao, Long serverId) {
        Long uid = RequestHolder.get().getUid();
        Server server = serverDao.lambdaQuery()
                .eq(Server::getId, serverId)
                .eq(Server::getStatus, ServerStatusEnum.NORMAL.getStatus())
                .oneOpt()
                .orElseThrow(() -> new BusinessException(BusinessErrorEnum.SERVER_NOT_FOUND));
        if (!server.getOwnerId().equals(uid)) {
            throw new BusinessException(BusinessErrorEnum.NO_PERMISSION);
        }
        return server;
    }
}
