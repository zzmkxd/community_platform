package com.community.server.service;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "community-server-service", contextId = "permissionService", path = "/internal/permission")
public interface PermissionService {

    /** 检查用户在服务器的某个频道是否有指定权限 */
    @GetMapping("/check")
    boolean checkPermission(@RequestParam("serverId") Long serverId,
                            @RequestParam("userId") Long userId,
                            @RequestParam("channelId") Long channelId,
                            @RequestParam("permissionBit") int permissionBit);
}
