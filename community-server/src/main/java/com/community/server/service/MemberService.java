package com.community.server.service;

import com.community.common.domain.vo.response.CursorPageBaseResp;
import com.community.server.domain.vo.MemberVO;

public interface MemberService {

    CursorPageBaseResp<MemberVO> getMembers(Long serverId, String cursor, Integer pageSize);

    MemberVO joinServer(Long serverId);

    void leaveOrKick(Long serverId, Long userId);

    MemberVO updateNickname(Long serverId, String nickname);
}
