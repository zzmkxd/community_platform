package com.community.server.controller;

import com.community.common.domain.vo.request.CursorPageBaseReq;
import com.community.common.domain.vo.response.ApiResult;
import com.community.common.domain.vo.response.CursorPageBaseResp;
import com.community.server.domain.vo.MemberVO;
import com.community.server.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/servers/{serverId}/members")
@RequiredArgsConstructor
@Tag(name = "成员")
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "获取服务器成员列表（游标分页）")
    @GetMapping
    public ApiResult<CursorPageBaseResp<MemberVO>> getMembers(
            @PathVariable Long serverId, CursorPageBaseReq req) {
        return ApiResult.success(memberService.getMembers(serverId, req.getCursor(), req.getPageSize()));
    }

    @Operation(summary = "加入公开服务器")
    @PostMapping
    public ApiResult<MemberVO> join(@PathVariable Long serverId) {
        return ApiResult.success(memberService.joinServer(serverId));
    }

    @Operation(summary = "踢出成员或主动退出")
    @DeleteMapping("/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leaveOrKick(@PathVariable Long serverId, @PathVariable Long userId) {
        memberService.leaveOrKick(serverId, userId);
    }

    @Operation(summary = "修改本人在该服务器的昵称")
    @PutMapping("/me/nickname")
    public ApiResult<MemberVO> updateNickname(@PathVariable Long serverId, @RequestBody Map<String, String> body) {
        return ApiResult.success(memberService.updateNickname(serverId, body.get("nickname")));
    }

    @Operation(summary = "转让服务器所有权")
    @PutMapping("/transfer-ownership")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void transferOwnership(@PathVariable Long serverId, @RequestBody Map<String, Object> body) {
        Long newOwnerId = ((Number) body.get("newOwnerId")).longValue();
        memberService.transferOwnership(serverId, newOwnerId);
    }
}
