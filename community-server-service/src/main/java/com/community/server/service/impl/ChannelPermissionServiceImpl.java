package com.community.server.service.impl;

import com.community.common.exception.BusinessErrorEnum;
import com.community.common.exception.BusinessException;
import com.community.common.utils.RequestHolder;
import com.community.server.dao.ChannelDao;
import com.community.server.dao.ChannelPermissionDao;
import com.community.server.dao.MemberDao;
import com.community.server.domain.entity.Channel;
import com.community.server.domain.entity.ChannelPermission;
import com.community.server.domain.entity.Server;
import com.community.server.domain.entity.ServerMember;
import com.community.server.domain.vo.ChannelPermissionVO;
import com.community.server.dao.ServerDao;
import com.community.server.service.ChannelPermissionService;
import com.community.server.service.MembershipValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelPermissionServiceImpl implements ChannelPermissionService {

    private final ChannelPermissionDao channelPermissionDao;
    private final ChannelDao channelDao;
    private final ServerDao serverDao;
    private final MemberDao memberDao;

    @Override
    @Transactional
    public ChannelPermissionVO upsertPermission(Long channelId, Integer targetType, Long targetId,
                                                 Long allowBits, Long denyBits) {
        Channel channel = channelDao.lambdaQuery()
                .eq(Channel::getId, channelId)
                .eq(Channel::getStatus, 1)
                .oneOpt()
                .orElseThrow(() -> new BusinessException(BusinessErrorEnum.CHANNEL_NOT_FOUND));

        MembershipValidator.requireServerOwner(serverDao, channel.getServerId());

        ChannelPermission perm = channelPermissionDao.lambdaQuery()
                .eq(ChannelPermission::getChannelId, channelId)
                .eq(ChannelPermission::getTargetType, targetType)
                .eq(ChannelPermission::getTargetId, targetId)
                .one();

        if (perm == null) {
            perm = new ChannelPermission();
            perm.setChannelId(channelId);
            perm.setTargetType(targetType);
            perm.setTargetId(targetId);
        }
        perm.setAllowBits(allowBits != null ? allowBits : 0L);
        perm.setDenyBits(denyBits != null ? denyBits : 0L);
        channelPermissionDao.saveOrUpdate(perm);

        log.info("ChannelPermission upserted: channelId={}, targetType={}, targetId={}", channelId, targetType, targetId);
        return toVO(perm);
    }

    @Override
    public List<ChannelPermissionVO> getPermissions(Long channelId) {
        Channel channel = channelDao.lambdaQuery()
                .eq(Channel::getId, channelId)
                .eq(Channel::getStatus, 1)
                .oneOpt()
                .orElseThrow(() -> new BusinessException(BusinessErrorEnum.CHANNEL_NOT_FOUND));

        MembershipValidator.requireMember(memberDao, channel.getServerId());

        return channelPermissionDao.lambdaQuery()
                .eq(ChannelPermission::getChannelId, channelId)
                .list()
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    @Transactional
    public void deletePermission(Long channelId, Long permId) {
        ChannelPermission perm = channelPermissionDao.lambdaQuery()
                .eq(ChannelPermission::getId, permId)
                .eq(ChannelPermission::getChannelId, channelId)
                .oneOpt()
                .orElseThrow(() -> new BusinessException(BusinessErrorEnum.NO_PERMISSION));

        Channel channel = channelDao.getById(channelId);
        if (channel == null) {
            throw new BusinessException(BusinessErrorEnum.CHANNEL_NOT_FOUND);
        }
        MembershipValidator.requireServerOwner(serverDao, channel.getServerId());

        channelPermissionDao.removeById(permId);
        log.info("ChannelPermission deleted: id={}, channelId={}", permId, channelId);
    }

    private ChannelPermissionVO toVO(ChannelPermission perm) {
        ChannelPermissionVO vo = new ChannelPermissionVO();
        vo.setId(perm.getId());
        vo.setChannelId(perm.getChannelId());
        vo.setTargetType(perm.getTargetType());
        vo.setTargetId(perm.getTargetId());
        vo.setAllowBits(perm.getAllowBits());
        vo.setDenyBits(perm.getDenyBits());
        vo.setCreateTime(perm.getCreateTime());
        return vo;
    }
}
