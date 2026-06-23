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
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    private static final int UPLOAD_EXPIRE_MINUTES = 10;
    private static final int DOWNLOAD_EXPIRE_HOURS = 1;

    private final MinioClient minioClient;
    private final OssProperties ossProperties;
    private final FileAttachmentDao fileAttachmentDao;

    @Override
    public FileVO getPresignedUrl(String fileName, Long fileSize, String mimeType) {
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
            vo.setDownloadUrl(uploadUrl);
            return vo;
        } catch (Exception e) {
            log.error("Failed to generate presigned upload URL for {}", objectKey, e);
            throw new BusinessException(BusinessErrorEnum.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    public FileVO confirmUpload(Long fileId) {
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
    public FileVO getFile(Long fileId) {
        FileAttachment file = fileAttachmentDao.lambdaQuery()
                .eq(FileAttachment::getId, fileId).one();
        if (file == null) {
            throw new BusinessException(BusinessErrorEnum.FILE_NOT_FOUND);
        }
        return buildFileVO(file);
    }

    @Override
    public List<FileVO> getFiles(List<Long> fileIds) {
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
    public boolean isFileUploaded(Long fileId) {
        FileAttachment file = fileAttachmentDao.lambdaQuery()
                .eq(FileAttachment::getId, fileId)
                .one();
        return file != null && FileStatusEnum.UPLOADED.getStatus().equals(file.getStatus());
    }

    @Override
    public String getDownloadUrl(Long fileId) {
        FileAttachment file = fileAttachmentDao.lambdaQuery()
                .eq(FileAttachment::getId, fileId).one();
        if (file == null || !FileStatusEnum.UPLOADED.getStatus().equals(file.getStatus())) {
            throw new BusinessException(BusinessErrorEnum.FILE_NOT_FOUND);
        }
        String preview = getContentTypePreview(file.getMimeType());
        try {
            return minioClient.getPresignedObjectUrl(
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
            return minioClient.getPresignedObjectUrl(
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
        } catch (Exception e) {
            log.error("Failed to generate download URL for {}", file.getObjectKey(), e);
            throw new BusinessException(BusinessErrorEnum.FILE_UPLOAD_FAILED);
        }
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
