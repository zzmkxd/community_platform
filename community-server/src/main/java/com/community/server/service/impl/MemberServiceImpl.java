package com.community.server.service.impl;

import com.community.common.domain.vo.response.CursorPageBaseResp;
import com.community.server.domain.vo.MemberVO;
import com.community.server.service.MemberService;
import org.springframework.stereotype.Service;

@Service
public class MemberServiceImpl implements MemberService {

    @Override
    public CursorPageBaseResp<MemberVO> getMembers(Long serverId, String cursor, Integer pageSize) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public MemberVO joinServer(Long serverId) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void leaveOrKick(Long serverId, Long userId) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public MemberVO updateNickname(Long serverId, String nickname) {
        throw new UnsupportedOperationException("TODO");
    }
}
