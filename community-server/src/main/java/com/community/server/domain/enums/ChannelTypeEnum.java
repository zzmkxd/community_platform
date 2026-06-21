package com.community.server.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ChannelTypeEnum {

    TEXT(0, "文字频道"),
    VOICE(1, "语音频道（预留）"),
    ;

    private final Integer type;
    private final String desc;
}
