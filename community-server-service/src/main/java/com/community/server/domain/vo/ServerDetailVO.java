package com.community.server.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * GET /api/v1/servers/{serverId} 的返回体：
 * 服务器详情 + 分类（含频道）+ 我的角色
 */
@Data
public class ServerDetailVO {

    @Schema(description = "服务器基本信息")
    private ServerVO server;

    @Schema(description = "分类列表（嵌套频道）")
    private List<CategoryVO> categories;

    @Schema(description = "我在该服务器的角色列表")
    private List<RoleVO> myRoles;
}
