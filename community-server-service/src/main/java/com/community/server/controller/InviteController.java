package com.community.server.controller;

import com.community.common.domain.vo.response.ApiResult;
import com.community.server.domain.vo.InviteVO;
import com.community.server.domain.vo.ServerVO;
import com.community.server.service.InviteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/servers/{serverId}/invites")
@RequiredArgsConstructor
@Tag(name = "邀请链接")
public class InviteController {

    private final InviteService inviteService;

    @Operation(summary = "创建邀请链接（可设最大使用次数和有效期）")
    @PostMapping
    public ApiResult<InviteVO> create(@PathVariable Long serverId, @RequestBody Map<String, Object> body) {
        Integer maxUses = body.get("maxUses") != null ? ((Number) body.get("maxUses")).intValue() : 0;
        Integer expireHours = body.get("expireHours") != null ? ((Number) body.get("expireHours")).intValue() : 0;
        return ApiResult.success(inviteService.createInvite(serverId, maxUses, expireHours));
    }

    @Operation(summary = "通过邀请码加入服务器")
    @PostMapping("/join")
    public ApiResult<ServerVO> join(@PathVariable Long serverId, @RequestBody Map<String, String> body) {
        return ApiResult.success(inviteService.joinByInvite(body.get("code")));
    }
}
