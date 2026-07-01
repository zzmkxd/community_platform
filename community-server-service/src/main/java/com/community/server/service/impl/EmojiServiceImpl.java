package com.community.server.service.impl;

import com.community.common.config.OssProperties;
import com.community.common.exception.BusinessErrorEnum;
import com.community.common.exception.BusinessException;
import com.community.common.exception.CommonErrorEnum;
import com.community.common.utils.RequestHolder;
import com.community.server.dao.EmojiDao;
import com.community.server.dao.MemberDao;
import com.community.server.domain.entity.Emoji;
import com.community.server.domain.entity.Server;
import com.community.server.domain.vo.EmojiVO;
import com.community.server.dao.ServerDao;
import com.community.server.service.EmojiService;
import com.community.server.service.MembershipValidator;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class EmojiServiceImpl implements EmojiService {

    private final EmojiDao emojiDao;
    private final ServerDao serverDao;
    private final MemberDao memberDao;
    private final MinioClient minioClient;
    private final MinioClient minioPresignedClient;
    private final OssProperties ossProperties;

    public EmojiServiceImpl(EmojiDao emojiDao, ServerDao serverDao, MemberDao memberDao,
                            MinioClient minioClient,
                            @Qualifier("minioPresignedClient") MinioClient minioPresignedClient,
                            OssProperties ossProperties) {
        this.emojiDao = emojiDao;
        this.serverDao = serverDao;
        this.memberDao = memberDao;
        this.minioClient = minioClient;
        this.minioPresignedClient = minioPresignedClient;
        this.ossProperties = ossProperties;
    }

    @Override
    @Transactional
    public EmojiVO uploadEmoji(Long serverId, String name, byte[] imageBytes) {
        Long uid = RequestHolder.get().getUid();

        MembershipValidator.requireMember(memberDao, serverId);

        // 仅存储 objectKey，对外 URL 由 toVO() 动态生成预签名 URL
        String objectKey = "emoji/" + serverId + "/" + UUID.randomUUID().toString().substring(0, 8) + "_" + name;

        try {
            String contentType = detectContentType(imageBytes);
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(ossProperties.getBucketName())
                    .object(objectKey)
                    .stream(new ByteArrayInputStream(imageBytes), imageBytes.length, -1)
                    .contentType(contentType)
                    .build());
            log.info("Emoji image uploaded to MinIO: bucket={}, object={}, size={}",
                    ossProperties.getBucketName(), objectKey, imageBytes.length);
        } catch (Exception e) {
            log.error("Failed to upload emoji image to MinIO: object={}", objectKey, e);
            throw new BusinessException(CommonErrorEnum.SYSTEM_ERROR);
        }

        Emoji emoji = new Emoji();
        emoji.setServerId(serverId);
        emoji.setName(name);
        emoji.setUrl(objectKey); // 存 objectKey，不是完整 URL
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
        vo.setUrl(toPublicUrl(emoji.getUrl()));
        vo.setCreatorId(emoji.getCreatorId());
        return vo;
    }

    /**
     * 将 objectKey 转换为浏览器可访问的 URL。
     * 优先使用预签名 URL（支持私有 bucket），失败时回退到 publicEndpoint 直链。
     * 预签名 URL 通过外部 endpoint 的 client 直接生成，无需端点替换。
     */
    private String toPublicUrl(String objectKey) {
        if (objectKey == null) return null;
        // 已经是完整 HTTP URL 则直接返回（兼容旧数据）
        if (objectKey.startsWith("http://") || objectKey.startsWith("https://")) {
            return objectKey;
        }
        try {
            return minioPresignedClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(ossProperties.getBucketName())
                            .object(objectKey)
                            .expiry(7, TimeUnit.DAYS)
                            .build());
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for object: {}", objectKey, e);
            return ossProperties.getEffectivePublicEndpoint()
                    + "/" + ossProperties.getBucketName() + "/" + objectKey;
        }
    }

    private String detectContentType(byte[] bytes) {
        if (bytes.length < 4) return "application/octet-stream";
        int b0 = bytes[0] & 0xFF;
        int b1 = bytes[1] & 0xFF;
        int b2 = bytes[2] & 0xFF;
        int b3 = bytes[3] & 0xFF;
        if (b0 == 0xFF && b1 == 0xD8) return "image/jpeg";
        if (b0 == 0x89 && b1 == 0x50 && b2 == 0x4E && b3 == 0x47) return "image/png";
        if (b0 == 0x47 && b1 == 0x49 && b2 == 0x46) return "image/gif";
        if (b0 == 0x52 && b1 == 0x49 && b2 == 0x46 && b3 == 0x46) return "image/webp";
        return "application/octet-stream";
    }
}
