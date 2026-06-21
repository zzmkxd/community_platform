package com.community.server.service.impl;

import com.community.server.domain.vo.CategoryVO;
import com.community.server.domain.vo.ChannelVO;
import com.community.server.service.ChannelService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChannelServiceImpl implements ChannelService {

    @Override
    public CategoryVO createCategory(Long serverId, String name, Integer sortOrder) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public CategoryVO updateCategory(Long serverId, Long categoryId, String name) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void deleteCategory(Long serverId, Long categoryId) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public ChannelVO createChannel(Long serverId, Long categoryId, String name, Integer type, String topic) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public List<CategoryVO> getChannels(Long serverId) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public ChannelVO getChannel(Long serverId, Long channelId) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public ChannelVO updateChannel(Long serverId, Long channelId, String name, String topic) {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public void deleteChannel(Long serverId, Long channelId) {
        throw new UnsupportedOperationException("TODO");
    }
}
