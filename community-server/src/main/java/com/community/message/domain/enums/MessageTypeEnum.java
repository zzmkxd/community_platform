package com.community.message.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum MessageTypeEnum {

    TEXT(1, "文本消息"),
    IMAGE(2, "图片消息"),
    FILE(3, "文件消息"),
    SYSTEM(4, "系统消息"),
    SOUND(5, "语音消息"),
    EMOJI(6, "表情消息"),
    ;

    private final Integer type;
    private final String desc;

    public static MessageTypeEnum of(Integer type) {
        for (MessageTypeEnum value : values()) {
            if (value.getType().equals(type)) {
                return value;
            }
        }
        return null;
    }
}
