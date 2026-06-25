package com.community.server.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EmojiVO {

    @Schema(description = "表情 ID")
    private Long id;

    @Schema(description = "服务器 ID")
    private Long serverId;

    @Schema(description = "表情名称")
    private String name;

    @Schema(description = "图片 URL")
    private String url;

    @Schema(description = "上传者 ID")
    private Long creatorId;

    private LocalDateTime createTime;
}
