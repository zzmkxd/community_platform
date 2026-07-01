package com.community.server.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum MemberStatusEnum {

    ACTIVE(1, "正常"),
    KICKED(2, "被踢出"),
    LEFT(3, "已离开"),
    ;

    private final Integer status;
    private final String desc;
}
