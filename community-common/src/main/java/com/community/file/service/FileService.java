package com.community.file.service;

import com.community.file.domain.vo.FileVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "community-file-service", path = "/internal/file")
public interface FileService {

    /** 获取预签名上传 URL */
    @PostMapping("/presigned-url")
    FileVO getPresignedUrl(@RequestParam("fileName") String fileName,
                           @RequestParam("fileSize") Long fileSize,
                           @RequestParam("mimeType") String mimeType);

    /** 确认上传完成 */
    @PostMapping("/{fileId}/confirm")
    FileVO confirmUpload(@PathVariable("fileId") Long fileId);

    /** 获取文件信息 */
    @GetMapping("/{fileId}")
    FileVO getFile(@PathVariable("fileId") Long fileId);

    /** 批量获取文件信息 */
    @PostMapping("/batch")
    List<FileVO> getFiles(@RequestBody List<Long> fileIds);

    /** 判断文件是否已上传完成 */
    @GetMapping("/{fileId}/uploaded")
    boolean isFileUploaded(@PathVariable("fileId") Long fileId);

    /** 获取下载 URL */
    @GetMapping("/{fileId}/download")
    String getDownloadUrl(@PathVariable("fileId") Long fileId);
}
