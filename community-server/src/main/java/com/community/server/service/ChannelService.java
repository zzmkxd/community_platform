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

    /** 按频道 ID 查询（不校验 serverId） */
    ChannelVO getById(Long channelId);

    /** 按服务器 ID 查询所有有效频道 */
    List<ChannelVO> listByServerId(Long serverId);

    ChannelVO updateChannel(Long serverId, Long channelId, String name, String topic);

    void deleteChannel(Long serverId, Long channelId);
}
