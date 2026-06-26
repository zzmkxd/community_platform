package com.community.message.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum MessageStatusEnum {

    NORMAL(0, "正常"),
    DELETED(1, "已删除"),
    EDITED(2, "已编辑"),
    ;

    private final Integer status;
    private final String desc;
}
