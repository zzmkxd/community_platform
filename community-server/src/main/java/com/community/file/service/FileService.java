package com.community.file.service;

import com.community.message.domain.vo.FileVO;

public interface FileService {

    /** 获取预签名上传 URL */
    FileVO getPresignedUrl(String fileName, Long fileSize, String mimeType);

    /** 确认上传完成 */
    FileVO confirmUpload(Long fileId);

    /** 获取文件信息 */
    FileVO getFile(Long fileId);

    /** 获取下载 URL */
    String getDownloadUrl(Long fileId);
}
