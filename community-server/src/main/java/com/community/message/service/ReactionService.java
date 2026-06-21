package com.community.message.service;

import com.community.message.domain.vo.ReactionVO;

import java.util.List;

public interface ReactionService {

    /** Toggle reaction: 已有则删除，无则添加 */
    List<ReactionVO> toggleReaction(Long msgId, String emoji);

    List<ReactionVO> getReactions(Long msgId);
}
