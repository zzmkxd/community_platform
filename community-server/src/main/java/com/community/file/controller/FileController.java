package com.community.file.controller;

import com.community.common.domain.vo.response.ApiResult;
import com.community.file.service.FileService;
import com.community.message.domain.vo.FileVO;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/upload")
@RequiredArgsConstructor
@Tag(name = "文件")
public class FileController {

    private final FileService fileService;

    @PostMapping("/presigned")
    public ApiResult<Map<String, Object>> getPresignedUrl(@RequestBody Map<String, Object> body) {
        String fileName = (String) body.get("fileName");
        Long fileSize = ((Number) body.get("fileSize")).longValue();
        String mimeType = (String) body.get("mimeType");
        FileVO vo = fileService.getPresignedUrl(fileName, fileSize, mimeType);
        return ApiResult.success(Map.of("uploadUrl", vo.getDownloadUrl(), "fileId", vo.getId()));
    }

    @PostMapping("/confirm")
    public ApiResult<FileVO> confirm(@RequestBody Map<String, Object> body) {
        Long fileId = ((Number) body.get("fileId")).longValue();
        return ApiResult.success(fileService.confirmUpload(fileId));
    }

    @GetMapping("/api/v1/files/{fileId}")
    public ApiResult<FileVO> getFile(@PathVariable Long fileId) {
        // TODO
        throw new UnsupportedOperationException("TODO");
    }

    @GetMapping("/api/v1/files/{fileId}/download")
    public ApiResult<Map<String, String>> download(@PathVariable Long fileId) {
        return ApiResult.success(Map.of("downloadUrl", fileService.getDownloadUrl(fileId)));
    }
}
