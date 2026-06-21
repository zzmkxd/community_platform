package com.community.file.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum FileStatusEnum {

    PENDING("PENDING", "等待上传"),
    UPLOADED("UPLOADED", "已上传"),
    ;

    private final String status;
    private final String desc;
}
