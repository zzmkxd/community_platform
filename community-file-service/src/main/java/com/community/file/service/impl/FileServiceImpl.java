package com.community.file.service.impl;

import cn.hutool.core.util.IdUtil;
import com.community.common.config.OssProperties;
import com.community.common.exception.BusinessErrorEnum;
import com.community.common.exception.BusinessException;
import com.community.common.utils.RequestHolder;
import com.community.file.dao.FileAttachmentDao;
import com.community.file.domain.entity.FileAttachment;
import com.community.common.enums.FileStatusEnum;
import com.community.file.service.FileService;
import com.community.file.domain.vo.FileVO;

import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

// ponytail: @RestController 让 Feign 接口的 REST 端点生效
// 注意：参数注解不随 @Override 继承，必须显式声明
@Slf4j
@RestController
@RequestMapping("/internal/file")
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private static final int UPLOAD_EXPIRE_MINUTES = 10;
    private static final int DOWNLOAD_EXPIRE_HOURS = 1;

    private final MinioClient minioClient;
    private final OssProperties ossProperties;
    private final FileAttachmentDao fileAttachmentDao;

    @Override
    @PostMapping("/presigned-url")
    public FileVO getPresignedUrl(@RequestParam("fileName") String fileName,
                                  @RequestParam("fileSize") Long fileSize,
                                  @RequestParam("mimeType") String mimeType) {
        Long uid = RequestHolder.get().getUid();
        String objectKey = uid + "/" + IdUtil.fastSimpleUUID() + "_" + fileName;

        FileAttachment file = new FileAttachment();
        file.setUserId(uid);
        file.setObjectKey(objectKey);
        file.setFileName(fileName);
        file.setFileSize(fileSize);
        file.setMimeType(mimeType);
        file.setStatus(FileStatusEnum.PENDING.getStatus());
        fileAttachmentDao.save(file);

        try {
            String uploadUrl = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.PUT)
                            .bucket(ossProperties.getBucketName())
                            .object(objectKey)
                            .expiry(UPLOAD_EXPIRE_MINUTES, TimeUnit.MINUTES)
                            .build()
            );
            FileVO vo = new FileVO();
            vo.setId(file.getId());
            vo.setFileName(fileName);
            vo.setFileSize(fileSize);
            vo.setMimeType(mimeType);
            vo.setDownloadUrl(toPublicUrl(uploadUrl));
            return vo;
        } catch (Exception e) {
            log.error("Failed to generate presigned upload URL for {}", objectKey, e);
            throw new BusinessException(BusinessErrorEnum.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    @PostMapping("/{fileId}/confirm")
    public FileVO confirmUpload(@PathVariable("fileId") Long fileId) {
        FileAttachment file = fileAttachmentDao.lambdaQuery()
                .eq(FileAttachment::getId, fileId).one();
        if (file == null) {
            throw new BusinessException(BusinessErrorEnum.FILE_NOT_FOUND);
        }
        if (FileStatusEnum.UPLOADED.getStatus().equals(file.getStatus())) {
            return buildFileVO(file);
        }

        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(ossProperties.getBucketName())
                    .object(file.getObjectKey())
                    .build());
        } catch (Exception e) {
            log.error("File not found in MinIO: {}", file.getObjectKey(), e);
            throw new BusinessException(BusinessErrorEnum.FILE_NOT_FOUND);
        }

        file.setStatus(FileStatusEnum.UPLOADED.getStatus());
        fileAttachmentDao.updateById(file);

        return buildFileVO(file);
    }

    @Override
    @GetMapping("/{fileId}")
    public FileVO getFile(@PathVariable("fileId") Long fileId) {
        FileAttachment file = fileAttachmentDao.lambdaQuery()
                .eq(FileAttachment::getId, fileId).one();
        if (file == null) {
            throw new BusinessException(BusinessErrorEnum.FILE_NOT_FOUND);
        }
        return buildFileVO(file);
    }

    @Override
    @PostMapping("/batch")
    public List<FileVO> getFiles(@RequestBody List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return List.of();
        }
        return fileAttachmentDao.lambdaQuery()
                .in(FileAttachment::getId, fileIds)
                .list()
                .stream()
                .map(this::buildFileVO)
                .toList();
    }

    @Override
    @GetMapping("/{fileId}/uploaded")
    public boolean isFileUploaded(@PathVariable("fileId") Long fileId) {
        FileAttachment file = fileAttachmentDao.lambdaQuery()
                .eq(FileAttachment::getId, fileId)
                .one();
        return file != null && FileStatusEnum.UPLOADED.getStatus().equals(file.getStatus());
    }

    @Override
    @GetMapping("/{fileId}/download")
    public String getDownloadUrl(@PathVariable("fileId") Long fileId) {
        FileAttachment file = fileAttachmentDao.lambdaQuery()
                .eq(FileAttachment::getId, fileId).one();
        if (file == null || !FileStatusEnum.UPLOADED.getStatus().equals(file.getStatus())) {
            throw new BusinessException(BusinessErrorEnum.FILE_NOT_FOUND);
        }
        String preview = getContentTypePreview(file.getMimeType());
        try {
            String downloadUrl = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(ossProperties.getBucketName())
                            .object(file.getObjectKey())
                            .expiry(DOWNLOAD_EXPIRE_HOURS, TimeUnit.HOURS)
                            .extraQueryParams(java.util.Map.of(
                                    "response-content-disposition",
                                    preview + "; filename=\"" + file.getFileName() + "\""))
                            .build()
            );
            return toPublicUrl(downloadUrl);
        } catch (Exception e) {
            log.error("Failed to generate download URL for {}", file.getObjectKey(), e);
            throw new BusinessException(BusinessErrorEnum.FILE_UPLOAD_FAILED);
        }
    }

    private FileVO buildFileVO(FileAttachment file) {
        FileVO vo = new FileVO();
        vo.setId(file.getId());
        vo.setFileName(file.getFileName());
        vo.setFileSize(file.getFileSize());
        vo.setMimeType(file.getMimeType());
        vo.setWidth(file.getWidth());
        vo.setHeight(file.getHeight());
        vo.setStatus(file.getStatus());
        if (FileStatusEnum.UPLOADED.getStatus().equals(file.getStatus())) {
            vo.setDownloadUrl(generateDownloadUrl(file));
        }
        return vo;
    }

    private String generateDownloadUrl(FileAttachment file) {
        try {
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(ossProperties.getBucketName())
                            .object(file.getObjectKey())
                            .expiry(DOWNLOAD_EXPIRE_HOURS, TimeUnit.HOURS)
                            .extraQueryParams(java.util.Map.of(
                                    "response-content-disposition",
                                    getContentTypePreview(file.getMimeType())
                                            + "; filename=\"" + file.getFileName() + "\""))
                            .build()
            );
            return toPublicUrl(url);
        } catch (Exception e) {
            log.error("Failed to generate download URL for {}", file.getObjectKey(), e);
            throw new BusinessException(BusinessErrorEnum.FILE_UPLOAD_FAILED);
        }
    }

    /**
     * 将 presigned URL 中的内部端点替换为外部可访问端点。
     * 例如 http://minio:9000/bucket/... → http://localhost:9004/bucket/...
     */
    private String toPublicUrl(String presignedUrl) {
        String internal = ossProperties.getEndpoint();
        String external = ossProperties.getEffectivePublicEndpoint();
        if (internal == null || external == null || internal.equals(external)) {
            return presignedUrl;
        }
        return presignedUrl.replace(internal, external);
    }
    private String getContentTypePreview(String mimeType) {
        if (mimeType == null) return "attachment";
        if (mimeType.startsWith("image/") || mimeType.startsWith("video/")
                || mimeType.startsWith("audio/") || mimeType.equals("application/pdf")) {
            return "inline";
        }
        return "attachment";
    }
}
