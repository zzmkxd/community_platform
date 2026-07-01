package com.community.server.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ChannelStatusEnum {

    DELETED(0, "已删除"),
    ACTIVE(1, "活跃"),
    ;

    private final Integer status;
    private final String desc;
}
