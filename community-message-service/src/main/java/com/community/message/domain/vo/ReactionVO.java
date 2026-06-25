package com.community.message.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class ReactionVO {

    @Schema(description = "emoji 字符")
    private String emoji;

    @Schema(description = "数量")
    private Integer count;

    @Schema(description = "添加该反应的部分用户 ID（截断展示）")
    private List<Long> userIds;

    @Schema(description = "当前用户是否已添加")
    private Boolean reacted;
}
