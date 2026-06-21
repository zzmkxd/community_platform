package com.community.user.domain.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserVO {

    @Schema(description = "用户 ID")
    private Long id;

    @Schema(description = "JWT Token (注册/登录时返回)")
    private String token;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "头像 URL")
    private String avatar;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "微信 openId")
    private String openId;

    @Schema(description = "微信 unionId")
    private String unionId;

    @Schema(description = "性别")
    private Integer sex;

    @Schema(description = "状态")
    private Integer status;

    private LocalDateTime createTime;
}
