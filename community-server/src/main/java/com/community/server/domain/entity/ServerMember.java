package com.community.server.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("server_member")
public class ServerMember {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long serverId;

    private Long userId;

    /** 服务器内昵称（可不同于全局昵称） */
    private String nickname;

    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
