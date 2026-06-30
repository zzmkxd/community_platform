package com.community.message.service.impl;

import com.community.common.domain.vo.response.CursorPageBaseResp;
import com.community.common.exception.BusinessException;
import com.community.common.exception.CommonErrorEnum;
import com.community.message.dao.MessageDao;
import com.community.message.dao.ReactionDao;
import com.community.message.domain.document.MessageDocument;
import com.community.message.domain.entity.Message;
import com.community.message.domain.vo.MessageVO;
import com.community.message.service.SearchService;
import com.community.message.service.adapter.MessageAdapter;
import com.community.server.domain.vo.ChannelVO;
import com.community.server.service.ChannelService;
import com.community.common.domain.vo.UserVO;
import com.community.user.service.UserService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final MessageDao messageDao;
    private final ReactionDao reactionDao;
    private final UserService userService;
    private final ChannelService channelService;
    private final ElasticsearchOperations esOps;

    @Override
    public CursorPageBaseResp<MessageVO> search(Long serverId, String q, Long channelId,
                                                 String from, String to, Integer page) {
        // ES 查询：content 全文匹配 + serverId 精确过滤 + status != 1
        var criteria = new Criteria("content").matches(q)
                .and(new Criteria("serverId").is(serverId));
        if (channelId != null) {
            criteria = criteria.and(new Criteria("channelId").is(channelId));
        }
        criteria = criteria.and(new Criteria("status").not().is(1));
        if (from != null) {
            criteria = criteria.and(new Criteria("createTime")
                    .greaterThanEqual(from.replace(" ", "T")));
        }
        if (to != null) {
            criteria = criteria.and(new Criteria("createTime")
                    .lessThanEqual(to.replace(" ", "T")));
        }

        var query = new CriteriaQuery(criteria);
        query.setMaxResults(50);
        query.addSort(Sort.by(Sort.Direction.DESC, "createTime"));

        SearchHits<MessageDocument> hits;
        try {
            hits = esOps.search(query, MessageDocument.class);
        } catch (Exception e) {
            log.error("ES search query FAILED — serverId={}, q={}, error={}", serverId, q, e.toString());
            throw new BusinessException(CommonErrorEnum.SYSTEM_ERROR.getErrorCode(), "搜索服务暂时不可用，请稍后重试");
        }

        List<Long> msgIds = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(MessageDocument::getId)
                .toList();

        if (msgIds.isEmpty()) {
            CursorPageBaseResp<MessageVO> resp = new CursorPageBaseResp<>();
            resp.setIsLast(true);
            resp.setList(List.of());
            return resp;
        }

        // 按 ES 返回顺序从 MySQL 批量取
        Map<Long, Message> msgMap = messageDao.listByIds(msgIds).stream()
                .collect(Collectors.toMap(Message::getId, m -> m));
        List<Message> ordered = msgIds.stream()
                .map(msgMap::get)
                .filter(Objects::nonNull)
                .toList();

        List<Long> userIds = ordered.stream().map(Message::getFromUid).distinct().toList();
        Map<Long, UserVO> userMap = userService.getBatchUsers(userIds).stream()
                .collect(Collectors.toMap(UserVO::getId, u -> u));

        Map<Long, List<com.community.message.domain.vo.ReactionVO>> reactionMap =
                MessageAdapter.buildReactionMap(msgIds, reactionDao);

        List<MessageVO> vos = ordered.stream()
                .map(msg -> MessageAdapter.buildMessageVO(
                        msg, userMap.get(msg.getFromUid()),
                        reactionMap.getOrDefault(msg.getId(), List.of()), null))
                .toList();

        CursorPageBaseResp<MessageVO> resp = new CursorPageBaseResp<>();
        resp.setIsLast(true);
        resp.setList(vos);
        return resp;
    }

    @Override
    public int reindex(Long serverId) {
        // 1. 获取该服务器下所有频道
        List<ChannelVO> channels;
        try {
            channels = channelService.listByServerId(serverId);
        } catch (Exception e) {
            log.error("Reindex FAILED — cannot fetch channels for serverId={}, error={}", serverId, e.toString());
            throw new BusinessException(CommonErrorEnum.SYSTEM_ERROR.getErrorCode(), "无法获取服务器频道列表");
        }

        if (channels == null || channels.isEmpty()) {
            log.info("Reindex: no channels for serverId={}", serverId);
            return 0;
        }

        int total = 0;
        for (ChannelVO channel : channels) {
            total += reindexChannel(channel.getId(), serverId);
        }

        log.info("Reindex COMPLETED — serverId={}, totalMessages={}, channels={}", serverId, total, channels.size());
        return total;
    }

    /**
     * 重建单个频道的消息索引（按游标分页读取 MySQL）
     */
    private int reindexChannel(Long channelId, Long serverId) {
        int count = 0;
        Long cursor = null;
        while (true) {
            // 按 id 游标分页查询（等价于 getMessages 的 MySQL 查询方式）
            List<Message> batch = messageDao.lambdaQuery()
                    .eq(Message::getChannelId, channelId)
                    .ne(Message::getStatus, 1) // 排除已删除
                    .gt(cursor != null, Message::getId, cursor)
                    .orderByAsc(Message::getId)
                    .last("limit 200")
                    .list();

            if (batch.isEmpty()) {
                break;
            }

            List<MessageDocument> docs = new ArrayList<>();
            for (Message msg : batch) {
                docs.add(MessageDocument.from(msg, serverId));
            }

            try {
                esOps.save(docs);
                count += docs.size();
                log.info("Reindex batch — channelId={}, cursor={}, batchSize={}", channelId, cursor, docs.size());
            } catch (Exception e) {
                log.error("Reindex FAILED — channelId={}, cursor={}, batchSize={}, error={}",
                        channelId, cursor, docs.size(), e.toString());
                throw new BusinessException(CommonErrorEnum.SYSTEM_ERROR.getErrorCode(),
                        "ES 索引写入失败（channelId=" + channelId + "），请检查 Elasticsearch 是否正常运行");
            }

            cursor = batch.get(batch.size() - 1).getId();
        }
        return count;
    }
}
