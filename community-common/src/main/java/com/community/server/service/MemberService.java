package com.community.server.service;

import com.community.common.domain.vo.response.CursorPageBaseResp;
import com.community.server.domain.vo.MemberVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "community-server-service", contextId = "memberService", path = "/internal/member")
public interface MemberService {

    @GetMapping("/server/{serverId}")
    CursorPageBaseResp<MemberVO> getMembers(@PathVariable("serverId") Long serverId,
                                            @RequestParam(value = "cursor", required = false) String cursor,
                                            @RequestParam(value = "pageSize", required = false) Integer pageSize);

    @PostMapping("/server/{serverId}/join")
    MemberVO joinServer(@PathVariable("serverId") Long serverId);

    @DeleteMapping("/server/{serverId}/member/{userId}")
    void leaveOrKick(@PathVariable("serverId") Long serverId, @PathVariable("userId") Long userId);

    @PutMapping("/server/{serverId}/nickname")
    MemberVO updateNickname(@PathVariable("serverId") Long serverId,
                            @RequestParam("nickname") String nickname);

    @PostMapping("/server/{serverId}/transfer/{newOwnerId}")
    void transferOwnership(@PathVariable("serverId") Long serverId,
                           @PathVariable("newOwnerId") Long newOwnerId);

    /** 查询服务器的有效成员用户 ID 列表 */
    @GetMapping("/server/{serverId}/uids")
    List<Long> getServerMemberUids(@PathVariable("serverId") Long serverId,
                                   @RequestParam(value = "excludeUid", required = false) Long excludeUid);
}
