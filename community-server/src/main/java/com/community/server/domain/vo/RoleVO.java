package com.community.server.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class RoleVO {

    @Schema(description = "角色 ID")
    private Long id;

    @Schema(description = "角色名称")
    private String name;

    @Schema(description = "角色颜色")
    private String color;

    @Schema(description = "权限位掩码（14 位）")
    private Long permissions;

    @Schema(description = "角色等级")
    private Integer position;
}
