package com.community.server.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("invite")
public class Invite {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long serverId;

    private Long inviterId;

    private String code;

    /** 0=不限 */
    private Integer maxUses;

    private Integer usedCount;

    /** NULL=永不过期 */
    private LocalDateTime expireTime;

    /** 0=失效 1=有效 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
