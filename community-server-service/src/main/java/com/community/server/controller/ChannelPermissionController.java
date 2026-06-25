package com.community.server.controller;

import com.community.common.domain.vo.response.ApiResult;
import com.community.server.domain.vo.ChannelPermissionVO;
import com.community.server.service.ChannelPermissionService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/channels/{channelId}/permissions")
@RequiredArgsConstructor
@Tag(name = "频道权限覆盖")
public class ChannelPermissionController {

    private final ChannelPermissionService channelPermissionService;

    @PutMapping
    public ApiResult<ChannelPermissionVO> upsert(@PathVariable Long channelId, @RequestBody Map<String, Object> body) {
        Integer targetType = ((Number) body.get("targetType")).intValue();
        Long targetId = ((Number) body.get("targetId")).longValue();
        Long allowBits = body.get("allowBits") != null ? ((Number) body.get("allowBits")).longValue() : 0L;
        Long denyBits = body.get("denyBits") != null ? ((Number) body.get("denyBits")).longValue() : 0L;
        return ApiResult.success(channelPermissionService.upsertPermission(channelId, targetType, targetId, allowBits, denyBits));
    }

    @GetMapping
    public ApiResult<List<ChannelPermissionVO>> getPermissions(@PathVariable Long channelId) {
        return ApiResult.success(channelPermissionService.getPermissions(channelId));
    }

    @DeleteMapping("/{permId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long channelId, @PathVariable Long permId) {
        channelPermissionService.deletePermission(channelId, permId);
    }
}
