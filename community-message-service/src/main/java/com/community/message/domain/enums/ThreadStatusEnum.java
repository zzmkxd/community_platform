package com.community.message.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ThreadStatusEnum {

    ACTIVE("ACTIVE", "活跃"),
    ARCHIVED("ARCHIVED", "已归档"),
    ;

    private final String status;
    private final String desc;
}
