package com.community.server.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class InviteVO {

    @Schema(description = "邀请 ID")
    private Long id;

    @Schema(description = "服务器 ID")
    private Long serverId;

    @Schema(description = "创建者 ID")
    private Long inviterId;

    @Schema(description = "邀请码")
    private String code;

    @Schema(description = "最大使用次数 (0=不限)")
    private Integer maxUses;

    @Schema(description = "已使用次数")
    private Integer usedCount;

    @Schema(description = "过期时间 (null=永不过期)")
    private LocalDateTime expireTime;

    @Schema(description = "状态: 0=失效 1=有效")
    private Integer status;

    private LocalDateTime createTime;
}
