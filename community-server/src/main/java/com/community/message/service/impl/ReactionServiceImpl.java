package com.community.message.service.impl;

import com.community.message.domain.vo.ReactionVO;
import com.community.message.service.ReactionService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReactionServiceImpl implements ReactionService {

    @Override
    public List<ReactionVO> addReaction(Long msgId, String emoji) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public List<ReactionVO> removeReaction(Long msgId, String emoji) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public List<ReactionVO> getReactions(Long msgId) {
        throw new UnsupportedOperationException("TODO");
    }
}
