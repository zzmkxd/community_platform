package com.community.server.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ServerStatusEnum {

    DELETED(0, "已删除"),
    NORMAL(1, "正常"),
    ;

    private final Integer status;
    private final String desc;
}
