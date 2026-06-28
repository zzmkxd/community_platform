package com.community.server.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
public class MemberVO {

    @Schema(description = "成员 ID")
    private Long userId;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像 URL")
    private String avatar;

    @Schema(description = "服务器内昵称")
    private String serverNickname;

    @Schema(description = "成员状态")
    private Integer status;

    @Schema(description = "角色列表")
    private List<RoleVO> roles;
}
