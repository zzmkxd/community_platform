package com.community.server.domain.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("channel_permission")
public class ChannelPermission {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long channelId;

    /** 0=角色覆盖, 1=用户覆盖 */
    private Integer targetType;

    /** role_id 或 user_id */
    private Long targetId;

    /** 允许的权限位 */
    private Long allowBits;

    /** 拒绝的权限位 */
    private Long denyBits;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
