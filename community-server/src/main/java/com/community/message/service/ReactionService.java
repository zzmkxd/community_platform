package com.community.message.service;

import com.community.message.domain.vo.ReactionVO;

import java.util.List;

public interface ReactionService {

    List<ReactionVO> addReaction(Long msgId, String emoji);

    List<ReactionVO> removeReaction(Long msgId, String emoji);

    List<ReactionVO> getReactions(Long msgId);
}
