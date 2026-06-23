package com.community.server.service.impl;

import com.community.common.exception.BusinessErrorEnum;
import com.community.common.exception.BusinessException;
import com.community.common.utils.RequestHolder;
import com.community.server.dao.EmojiDao;
import com.community.server.dao.MemberDao;
import com.community.server.domain.entity.Emoji;
import com.community.server.domain.entity.Server;
import com.community.server.domain.entity.ServerMember;
import com.community.server.domain.vo.EmojiVO;
import com.community.server.dao.ServerDao;
import com.community.server.service.EmojiService;
import com.community.server.service.MembershipValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmojiServiceImpl implements EmojiService {

    private final EmojiDao emojiDao;
    private final ServerDao serverDao;
    private final MemberDao memberDao;

    @Override
    @Transactional
    public EmojiVO uploadEmoji(Long serverId, String name, byte[] imageBytes) {
        Long uid = RequestHolder.get().getUid();

        MembershipValidator.requireMember(memberDao, serverId);

        String objectKey = "emoji/" + serverId + "/" + UUID.randomUUID().toString().substring(0, 8) + "_" + name;

        Emoji emoji = new Emoji();
        emoji.setServerId(serverId);
        emoji.setName(name);
        emoji.setUrl(objectKey);
        emoji.setCreatorId(uid);
        emojiDao.save(emoji);

        log.info("Emoji uploaded: id={}, name={}, serverId={}", emoji.getId(), name, serverId);
        return toVO(emoji);
    }

    @Override
    public List<EmojiVO> getEmojis(Long serverId) {
        MembershipValidator.requireMember(memberDao, serverId);

        return emojiDao.lambdaQuery()
                .eq(Emoji::getServerId, serverId)
                .list()
                .stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    @Transactional
    public void deleteEmoji(Long serverId, Long emojiId) {
        MembershipValidator.requireMember(memberDao, serverId);

        Emoji emoji = emojiDao.lambdaQuery()
                .eq(Emoji::getId, emojiId)
                .eq(Emoji::getServerId, serverId)
                .oneOpt()
                .orElseThrow(() -> new BusinessException(BusinessErrorEnum.EMOJI_NOT_FOUND));

        Long uid = RequestHolder.get().getUid();
        Server server = serverDao.getById(serverId);
        if (server == null || (!server.getOwnerId().equals(uid) && !emoji.getCreatorId().equals(uid))) {
            throw new BusinessException(BusinessErrorEnum.NO_PERMISSION);
        }

        emojiDao.removeById(emojiId);
        log.info("Emoji deleted: id={}, name={}, serverId={}", emojiId, emoji.getName(), serverId);
    }

    private EmojiVO toVO(Emoji emoji) {
        EmojiVO vo = new EmojiVO();
        vo.setId(emoji.getId());
        vo.setServerId(emoji.getServerId());
        vo.setName(emoji.getName());
        vo.setUrl(emoji.getUrl());
        vo.setCreatorId(emoji.getCreatorId());
        return vo;
    }
}
