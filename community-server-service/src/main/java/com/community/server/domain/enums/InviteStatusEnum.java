package com.community.server.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum InviteStatusEnum {

    DISABLED(0, "已禁用"),
    ACTIVE(1, "有效"),
    ;

    private final Integer status;
    private final String desc;
}
