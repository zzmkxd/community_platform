package com.community.server.service.impl;

import com.community.server.domain.vo.EmojiVO;
import com.community.server.service.EmojiService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmojiServiceImpl implements EmojiService {

    @Override
    public EmojiVO uploadEmoji(Long serverId, String name, byte[] imageBytes) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public List<EmojiVO> getEmojis(Long serverId) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void deleteEmoji(Long serverId, Long emojiId) {
        throw new UnsupportedOperationException("TODO");
    }
}
