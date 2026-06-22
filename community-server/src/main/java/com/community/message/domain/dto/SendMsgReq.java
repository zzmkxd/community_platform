package com.community.message.domain.dto;

import lombok.Data;

import java.util.List;

@Data
public class SendMsgReq {

    private String content;

    /** TEXT=1, IMAGE=2, FILE=3, SYSTEM=4, SOUND=5 */
    private Integer msgType;

    private Long threadId;

    private Long replyMsgId;

    /** 附件文件 ID 列表（图片/文件消息） */
    private List<Long> fileIds;

    /** 语音消息元数据（JSON 序列化至 extra 字段） */
    private SoundMsgDTO soundMsg;

    @Data
    public static class SoundMsgDTO {
        private String audioUrl;
        private Long size;
        private Integer second;
    }
}
