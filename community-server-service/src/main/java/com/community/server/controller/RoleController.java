package com.community.server.controller;

import com.community.common.domain.vo.response.ApiResult;
import com.community.server.domain.vo.RoleVO;
import com.community.server.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/servers/{serverId}")
@RequiredArgsConstructor
@Tag(name = "角色")
public class RoleController {

    private final RoleService roleService;

    // ---- Role CRUD ----

    @Operation(summary = "创建角色")
    @PostMapping("/roles")
    public ApiResult<RoleVO> create(@PathVariable Long serverId, @RequestBody Map<String, Object> body) {
        return ApiResult.success(roleService.createRole(serverId,
                (String) body.get("name"),
                (String) body.get("color"),
                body.get("permissions") != null ? ((Number) body.get("permissions")).longValue() : 0L,
                body.get("position") != null ? ((Number) body.get("position")).intValue() : 0));
    }

    @Operation(summary = "获取服务器所有角色")
    @GetMapping("/roles")
    public ApiResult<List<RoleVO>> getRoles(@PathVariable Long serverId) {
        return ApiResult.success(roleService.getRoles(serverId));
    }

    @Operation(summary = "修改角色信息（名称、颜色、权限位掩码）")
    @PutMapping("/roles/{roleId}")
    public ApiResult<RoleVO> update(@PathVariable Long serverId, @PathVariable Long roleId,
                                     @RequestBody Map<String, Object> body) {
        return ApiResult.success(roleService.updateRole(serverId, roleId,
                (String) body.get("name"),
                (String) body.get("color"),
                body.get("permissions") != null ? ((Number) body.get("permissions")).longValue() : null));
    }

    @Operation(summary = "删除角色")
    @DeleteMapping("/roles/{roleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long serverId, @PathVariable Long roleId) {
        roleService.deleteRole(serverId, roleId);
    }

    // ---- Member-Role Assignment ----

    @Operation(summary = "为用户分配角色（原子批量替换）")
    @PostMapping("/members/{userId}/roles")
    public ApiResult<List<RoleVO>> assignRoles(@PathVariable Long serverId, @PathVariable Long userId,
                                               @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Number> roleIdsRaw = (List<Number>) body.get("roleIds");
        List<Long> roleIds = roleIdsRaw != null ? roleIdsRaw.stream().map(Number::longValue).toList() : List.of();
        return ApiResult.success(roleService.assignRoles(serverId, userId, roleIds));
    }

    @Operation(summary = "移除用户指定角色")
    @DeleteMapping("/members/{userId}/roles/{roleId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeRole(@PathVariable Long serverId, @PathVariable Long userId, @PathVariable Long roleId) {
        roleService.removeRole(serverId, userId, roleId);
    }
}
