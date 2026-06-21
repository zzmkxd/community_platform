package com.community.server.controller;

import com.community.common.domain.vo.response.ApiResult;
import com.community.server.domain.vo.RoleVO;
import com.community.server.service.RoleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/servers/{serverId}/roles")
@RequiredArgsConstructor
@Tag(name = "角色")
public class RoleController {

    private final RoleService roleService;

    @PostMapping
    public ApiResult<RoleVO> create(@PathVariable Long serverId, @RequestBody Map<String, Object> body) {
        return ApiResult.success(roleService.createRole(serverId,
                (String) body.get("name"),
                (String) body.get("color"),
                body.get("permissions") != null ? ((Number) body.get("permissions")).longValue() : 0L,
                body.get("position") != null ? ((Number) body.get("position")).intValue() : 0));
    }

    @GetMapping
    public ApiResult<List<RoleVO>> getRoles(@PathVariable Long serverId) {
        return ApiResult.success(roleService.getRoles(serverId));
    }

    @PutMapping("/{roleId}")
    public ApiResult<RoleVO> update(@PathVariable Long serverId, @PathVariable Long roleId,
                                     @RequestBody Map<String, Object> body) {
        return ApiResult.success(roleService.updateRole(serverId, roleId,
                (String) body.get("name"),
                (String) body.get("color"),
                body.get("permissions") != null ? ((Number) body.get("permissions")).longValue() : null));
    }

    @DeleteMapping("/{roleId}")
    public ApiResult<Void> delete(@PathVariable Long serverId, @PathVariable Long roleId) {
        roleService.deleteRole(serverId, roleId);
        return ApiResult.success();
    }
}
