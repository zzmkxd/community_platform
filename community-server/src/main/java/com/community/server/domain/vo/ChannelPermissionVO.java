package com.community.server.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ChannelPermissionVO {

    @Schema(description = "覆盖 ID")
    private Long id;

    @Schema(description = "频道 ID")
    private Long channelId;

    @Schema(description = "目标类型: 0=角色, 1=用户")
    private Integer targetType;

    @Schema(description = "角色 ID 或 用户 ID")
    private Long targetId;

    @Schema(description = "允许的权限位")
    private Long allowBits;

    @Schema(description = "拒绝的权限位")
    private Long denyBits;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}
