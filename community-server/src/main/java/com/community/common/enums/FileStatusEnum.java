package com.community.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 文件附件状态枚举
 * <p>
 * 注：该枚举下沉到 common 模块，file/message 共享，避免微服务拆分后跨服务依赖 ORM Entity 包。
 */
@AllArgsConstructor
@Getter
public enum FileStatusEnum {

    PENDING("PENDING", "等待上传"),
    UPLOADED("UPLOADED", "已上传"),
    ;

    private final String status;
    private final String desc;
}
