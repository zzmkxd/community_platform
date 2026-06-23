package com.community.message.domain.vo;

import com.community.file.domain.vo.FileVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class MessageVO {

    @Schema(description = "消息 ID")
    private Long id;

    @Schema(description = "频道 ID")
    private Long channelId;

    @Schema(description = "话题 ID（null=频道主时间线）")
    private Long threadId;

    @Schema(description = "发送者")
    private MessageUserVO fromUser;

    @Schema(description = "消息内容（Markdown）")
    private String content;

    @Schema(description = "消息类型")
    private Integer msgType;

    @Schema(description = "消息状态 0=正常 1=删除 2=编辑过")
    private Integer status;

    @Schema(description = "回复的消息 ID")
    private Long replyMsgId;

    @Schema(description = "附件列表")
    private List<FileVO> attachments;

    @Schema(description = "Reaction 列表")
    private List<ReactionVO> reactions;

    @Schema(description = "Thread 摘要（如果有）")
    private ThreadSummaryVO thread;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @Data
    public static class MessageUserVO {
        private Long id;
        private String nickname;
        private String avatar;
    }

    @Data
    public static class ThreadSummaryVO {
        private Long id;
        private String name;
        private Integer messageCount;
    }
}
