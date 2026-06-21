package com.community.common.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum BusinessErrorEnum implements ErrorEnum {

    // ---- 用户模块 ----
    USER_NOT_FOUND(1001, "用户不存在"),
    PASSWORD_ERROR(1002, "密码错误"),
    USERNAME_DUPLICATE(1003, "用户名已存在"),
    TOKEN_INVALID(1004, "Token 无效或已过期"),
    TOKEN_EXPIRED(1005, "Token 已过期，请重新登录"),

    // ---- 服务器模块 ----
    SERVER_NOT_FOUND(2001, "服务器不存在"),
    NO_PERMISSION(2002, "无权限执行此操作"),
    NOT_SERVER_MEMBER(2003, "不是该服务器成员"),
    ROLE_NOT_FOUND(2004, "角色不存在"),
    CHANNEL_NOT_FOUND(2005, "频道不存在"),
    CATEGORY_NOT_FOUND(2006, "分类不存在"),
    EMOJI_NOT_FOUND(2007, "表情不存在"),

    // ---- 消息模块 ----
    MESSAGE_NOT_FOUND(3001, "消息不存在"),
    NOT_MESSAGE_AUTHOR(3002, "不是消息作者"),
    THREAD_NOT_FOUND(3003, "话题不存在"),
    THREAD_ARCHIVED(3004, "话题已归档，不可回复"),

    // ---- 文件模块 ----
    FILE_NOT_FOUND(4001, "文件不存在"),
    FILE_UPLOAD_FAILED(4002, "文件上传失败"),
    ;

    private final Integer code;
    private final String msg;

    @Override
    public Integer getErrorCode() {
        return this.code;
    }

    @Override
    public String getErrorMsg() {
        return this.msg;
    }
}
