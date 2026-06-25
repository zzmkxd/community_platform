package com.community.message.domain.vo;

import com.community.common.domain.vo.UserVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ThreadVO {

    @Schema(description = "话题 ID")
    private Long id;

    @Schema(description = "频道 ID")
    private Long channelId;

    @Schema(description = "触发消息 ID")
    private Long rootMsgId;

    @Schema(description = "话题名称")
    private String name;

    @Schema(description = "创建者")
    private UserVO creator;

    @Schema(description = "状态 ACTIVE/ARCHIVED")
    private String status;

    @Schema(description = "消息数")
    private Integer messageCount;

    @Schema(description = "最后活跃时间")
    private LocalDateTime lastActive;

    private LocalDateTime createTime;
}
