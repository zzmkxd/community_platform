package com.community.server.service;

public interface PermissionService {

    /**
     * 检查用户在服务器的某个频道是否有指定权限
     * @return true = 有权限
     */
    boolean checkPermission(Long serverId, Long userId, Long channelId, int permissionBit);
}
