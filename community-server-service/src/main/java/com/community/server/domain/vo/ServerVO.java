package com.community.server.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ServerVO {

    @Schema(description = "服务器 ID")
    private Long id;

    @Schema(description = "服务器名称")
    private String name;

    @Schema(description = "描述")
    private String description;

    @Schema(description = "图标 URL")
    private String icon;

    @Schema(description = "创建者 ID")
    private Long ownerId;

    @Schema(description = "成员数量")
    private Integer memberCount;

    private LocalDateTime createTime;
}
