package com.community.message.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class FileVO {

    @Schema(description = "文件 ID")
    private Long id;

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @Schema(description = "MIME 类型")
    private String mimeType;

    @Schema(description = "图片宽度（图片才有）")
    private Integer width;

    @Schema(description = "图片高度（图片才有）")
    private Integer height;

    @Schema(description = "状态: PENDING / UPLOADED")
    private String status;

    @Schema(description = "下载链接（预签名，仅 UPLOADED 状态有效）")
    private String downloadUrl;
}
