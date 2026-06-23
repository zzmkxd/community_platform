package com.community.server.service;

import com.community.common.domain.vo.response.CursorPageBaseResp;
import com.community.server.domain.vo.MemberVO;

import java.util.List;

public interface MemberService {

    CursorPageBaseResp<MemberVO> getMembers(Long serverId, String cursor, Integer pageSize);

    MemberVO joinServer(Long serverId);

    void leaveOrKick(Long serverId, Long userId);

    MemberVO updateNickname(Long serverId, String nickname);

    void transferOwnership(Long serverId, Long newOwnerId);

    /**
     * 查询服务器的有效成员用户 ID 列表
     * @param serverId 服务器 ID
     * @param excludeUid 需要排除的用户 ID（null 表示不排除）
     * @return 成员用户 ID 列表
     */
    List<Long> getServerMemberUids(Long serverId, Long excludeUid);
}
