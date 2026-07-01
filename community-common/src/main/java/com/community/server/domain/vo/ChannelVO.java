package com.community.server.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class ChannelVO {

    @Schema(description = "频道 ID")
    private Long id;

    @Schema(description = "频道名称")
    private String name;

    @Schema(description = "频道类型 TEXT=0, VOICE=1")
    private Integer type;

    @Schema(description = "频道简介")
    private String topic;

    @Schema(description = "排序")
    private Integer sortOrder;

    @Schema(description = "所属服务器 ID")
    private Long serverId;
}
