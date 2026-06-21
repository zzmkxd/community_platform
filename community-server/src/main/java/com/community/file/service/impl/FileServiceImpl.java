package com.community.file.service.impl;

import com.community.file.service.FileService;
import com.community.message.domain.vo.FileVO;
import org.springframework.stereotype.Service;

@Service
public class FileServiceImpl implements FileService {

    @Override
    public FileVO getPresignedUrl(String fileName, Long fileSize, String mimeType) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public FileVO confirmUpload(Long fileId) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public String getDownloadUrl(Long fileId) {
        throw new UnsupportedOperationException("TODO");
    }
}
