package com.community.file.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("file_attachment")
public class FileAttachment {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** MinIO 上的 objectKey */
    private String objectKey;

    private String fileName;

    private Long fileSize;

    private String mimeType;

    /** PENDING / UPLOADED */
    private String status;

    /** 关联的消息 ID（上传时为空，发消息时关联） */
    private Long messageId;

    private Integer width;

    private Integer height;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
