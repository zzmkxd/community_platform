package com.community.file.service;

import com.community.file.domain.vo.FileVO;

import java.util.List;

public interface FileService {

    /** 获取预签名上传 URL */
    FileVO getPresignedUrl(String fileName, Long fileSize, String mimeType);

    /** 确认上传完成 */
    FileVO confirmUpload(Long fileId);

    /** 获取文件信息 */
    FileVO getFile(Long fileId);

    /** 批量获取文件信息 */
    List<FileVO> getFiles(List<Long> fileIds);

    /** 判断文件是否已上传完成 */
    boolean isFileUploaded(Long fileId);

    /** 获取下载 URL */
    String getDownloadUrl(Long fileId);
}
