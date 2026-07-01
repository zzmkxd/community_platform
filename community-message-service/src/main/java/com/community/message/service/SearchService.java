package com.community.message.service;

import com.community.common.domain.vo.response.CursorPageBaseResp;
import com.community.message.domain.vo.MessageVO;

public interface SearchService {

    CursorPageBaseResp<MessageVO> search(Long serverId, String q, Long channelId,
                                          String from, String to, Integer page);

    /**
     * 重建指定服务器的 ES 搜索索引（从 MySQL 全量同步）。
     * 适用场景：ES 数据丢失、首次部署、索引映射变更。
     *
     * @param serverId 服务器 ID
     * @return 已索引的消息数量
     */
    int reindex(Long serverId);
}
