package com.community.server.service;

import com.community.server.domain.vo.CategoryVO;
import com.community.server.domain.vo.ChannelVO;

import java.util.List;

public interface ChannelService {

    // ---- Category ----
    CategoryVO createCategory(Long serverId, String name, Integer sortOrder);

    CategoryVO updateCategory(Long serverId, Long categoryId, String name);

    void deleteCategory(Long serverId, Long categoryId);

    // ---- Channel ----
    ChannelVO createChannel(Long serverId, Long categoryId, String name, Integer type, String topic);

    List<CategoryVO> getChannels(Long serverId);

    ChannelVO getChannel(Long serverId, Long channelId);

    ChannelVO updateChannel(Long serverId, Long channelId, String name, String topic);

    void deleteChannel(Long serverId, Long channelId);
}
