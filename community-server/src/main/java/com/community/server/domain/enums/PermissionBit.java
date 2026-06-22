package com.community.server.domain.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 13 位权限位图 (BIGINT 位掩码)
 * 位掩码递增: 0x0001, 0x0002, 0x0004, 0x0008, 0x0010, 0x0020, 0x0040, 0x0080, 0x0100, 0x0200, 0x0400, 0x0800, 0x1000
 */
@AllArgsConstructor
@Getter
public enum PermissionBit {

    CREATE_INVITE(0x0001, "创建邀请"),
    KICK_MEMBERS(0x0002, "踢出成员"),
    BAN_MEMBERS(0x0004, "封禁成员"),
    ADMINISTRATOR(0x0008, "管理员（全部权限）"),
    MANAGE_CHANNELS(0x0010, "管理频道"),
    MANAGE_SERVER(0x0020, "管理服务器"),
    ADD_REACTIONS(0x0040, "添加反应"),
    SEND_MESSAGES(0x0080, "发送消息"),
    USE_THREADS(0x0100, "使用话题"),
    EMBED_LINKS(0x0200, "嵌入链接"),
    ATTACH_FILES(0x0400, "上传文件"),
    MENTION_EVERYONE(0x0800, "@全体成员"),
    MANAGE_ROLES(0x1000, "管理角色"),
    ;

    private final int bit;
    private final String desc;
}
