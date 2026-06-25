package com.community.server.service;

import com.community.server.domain.vo.EmojiVO;

import java.util.List;

public interface EmojiService {

    EmojiVO uploadEmoji(Long serverId, String name, byte[] imageBytes);

    List<EmojiVO> getEmojis(Long serverId);

    void deleteEmoji(Long serverId, Long emojiId);
}
